import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/api/query.dart';
import '../../models/candle.dart';
import '../../models/enums.dart';

class CandleRepository {
  const CandleRepository(this._dio);

  final Dio _dio;

  /// 비인증 경로다.
  ///
  /// [exchange] 는 거래소 **이름 문자열**(`UPBIT`)이지 id 가 아니다 — `Exchange.candleCode` 를 넘긴다.
  /// [coin] 은 **심볼만**(`BTC`) 넘긴다. 서버가 기준 통화를 붙여 `BTC/KRW` 로 만든다.
  /// [cursor] 는 ISO-8601 Instant 문자열이다.
  Future<List<Candle>> getCandles({
    required String exchange,
    required String coin,
    required CandleInterval interval,
    int? limit,
    DateTime? cursor,
  }) => apiCall(() async {
    final response = await _dio.get(
      '/api/candles',
      queryParameters: query({
        'exchange': exchange,
        'coin': coin,
        'interval': interval.wire,
        'limit': limit,
        'cursor': cursor?.toUtc().toIso8601String(),
      }),
    );
    return (response.data as List<dynamic>)
        .map((json) => Candle.fromJson(json as Map<String, dynamic>))
        .toList();
  });
}

final candleRepositoryProvider = Provider<CandleRepository>(
  (ref) => CandleRepository(ref.watch(dioProvider)),
);
