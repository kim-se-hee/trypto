import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../models/portfolio.dart';
import 'portfolio_summary.dart';

class PortfolioRepository {
  const PortfolioRepository(this._dio);

  final Dio _dio;

  /// [walletId] 는 활성 라운드 응답의 `wallets[]` 에서만 얻는다.
  Future<MyHoldings> getPortfolio(int walletId) => apiCall(() async {
    final response = await _dio.get('/api/wallets/$walletId/portfolio');
    return MyHoldings.fromJson(response.data as Map<String, dynamic>);
  });
}

final portfolioRepositoryProvider = Provider<PortfolioRepository>(
  (ref) => PortfolioRepository(ref.watch(dioProvider)),
);

/// 폴링도 WS 구독도 없다(사양서 §5.1.2). 갱신 경로는 당김 새로고침 하나뿐이다.
final portfolioProvider = FutureProvider.family<PortfolioSummary, int>((
  ref,
  walletId,
) async {
  final holdings = await ref
      .watch(portfolioRepositoryProvider)
      .getPortfolio(walletId);
  return PortfolioSummary.of(holdings);
});
