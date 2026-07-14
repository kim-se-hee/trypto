import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/api/query.dart';
import '../../models/candle.dart';
import '../../models/enums.dart';
import 'live_candles.dart';

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

/// 서버 캔들만 담는다. 저빈도(진입·간격 변경·15초 재조정)이므로 Riverpod 그래프를 탄다 —
/// 실시간 봉은 `LiveCandleFolder` 가 따로 들고 있다(계획서 §5.4 ①).
///
/// 키가 [CandleRequest] 이므로 코인·거래소·간격이 바뀌면 이전 캔들이 화면에 남지 않는다.
final candlesProvider = FutureProvider.autoDispose
    .family<List<Candle>, CandleRequest>((ref, request) async {
      final candles = await ref
          .watch(candleRepositoryProvider)
          .getCandles(
            exchange: request.exchangeCode,
            coin: request.symbol,
            interval: request.interval,
            limit: request.limit,
          );
      return normalizeCandles(candles, request.interval);
    });
