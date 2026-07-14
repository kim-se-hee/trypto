import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../models/exchange_coin.dart';

class ExchangeCoinRepository {
  const ExchangeCoinRepository(this._dio);

  final Dio _dio;

  /// 비인증 경로다. 거래소당 최대 600행. 캐싱은 호출부(마켓 컨트롤러)가 한다 —
  /// 캐시 무효화 시점(라운드 생성·종료·로그아웃)을 아는 쪽이 거기다.
  Future<List<ExchangeCoin>> getCoins(int exchangeId) => apiCall(() async {
    final response = await _dio.get('/api/exchanges/$exchangeId/coins');
    return (response.data as List<dynamic>)
        .map((json) => ExchangeCoin.fromJson(json as Map<String, dynamic>))
        .toList();
  });
}

final exchangeCoinRepositoryProvider = Provider<ExchangeCoinRepository>(
  (ref) => ExchangeCoinRepository(ref.watch(dioProvider)),
);
