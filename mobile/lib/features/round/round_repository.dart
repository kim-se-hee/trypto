import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../models/round.dart';

class RoundRepository {
  const RoundRepository(this._dio);

  final Dio _dio;

  Future<StartRoundResponse> startRound(StartRoundRequest request) =>
      apiCall(() async {
        final response = await _dio.post('/api/rounds', data: request.toJson());
        return StartRoundResponse.fromJson(
          response.data as Map<String, dynamic>,
        );
      });

  /// 활성 라운드가 없으면 서버가 409 `ROUND_NOT_ACTIVE` 를 낸다. **예외가 아니라 `null` 이다**
  /// (사양서 §1.10.3). 이 분기를 놓치면 신규 사용자가 앱에 진입하지 못한다.
  Future<ActiveRound?> getActiveRound() async {
    try {
      return await apiCall(() async {
        final response = await _dio.get('/api/rounds/active');
        return ActiveRound.fromJson(response.data as Map<String, dynamic>);
      });
    } on ApiException catch (error) {
      if (error.isCode(ErrorCodes.roundNotActive)) return null;
      rethrow;
    }
  }

  Future<RoundSummary> getSummary() => apiCall(() async {
    final response = await _dio.get('/api/rounds/summary');
    return RoundSummary.fromJson(response.data as Map<String, dynamic>);
  });

  /// 서버가 바디를 읽지 않는다(사양서 R4-4). 웹은 `{userId}` 를 보내지만 무시된다.
  Future<EndRoundResponse> endRound(int roundId) => apiCall(() async {
    final response = await _dio.post('/api/rounds/$roundId/end');
    return EndRoundResponse.fromJson(response.data as Map<String, dynamic>);
  });

  Future<ChargeEmergencyFundingResponse> chargeEmergencyFunding(
    int roundId,
    ChargeEmergencyFundingRequest request,
  ) => apiCall(() async {
    final response = await _dio.post(
      '/api/rounds/$roundId/emergency-funding',
      data: request.toJson(),
    );
    return ChargeEmergencyFundingResponse.fromJson(
      response.data as Map<String, dynamic>,
    );
  });
}

final roundRepositoryProvider = Provider<RoundRepository>(
  (ref) => RoundRepository(ref.watch(dioProvider)),
);
