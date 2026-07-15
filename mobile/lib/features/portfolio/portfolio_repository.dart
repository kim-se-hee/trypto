import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../models/portfolio.dart';

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
