import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/api/query.dart';
import '../../models/regret.dart';

class RegretRepository {
  const RegretRepository(this._dio);

  final Dio _dio;

  /// 배치 전에는 서버가 **빈 리포트를 200 으로** 내린다(`RegretReport.isEmpty`). 오류가 아니다.
  /// 해당 라운드에 그 거래소 지갑이 없으면 404 `WALLET_NOT_FOUND` 다.
  Future<RegretReport> getReport({
    required int roundId,
    required int exchangeId,
  }) => apiCall(() async {
    final response = await _dio.get(
      '/api/rounds/$roundId/regret',
      queryParameters: query({'exchangeId': exchangeId}),
    );
    return RegretReport.fromJson(response.data as Map<String, dynamic>);
  });

  Future<RegretChart> getChart({
    required int roundId,
    required int exchangeId,
  }) => apiCall(() async {
    final response = await _dio.get(
      '/api/rounds/$roundId/regret/chart',
      queryParameters: query({'exchangeId': exchangeId}),
    );
    return RegretChart.fromJson(response.data as Map<String, dynamic>);
  });
}

final regretRepositoryProvider = Provider<RegretRepository>(
  (ref) => RegretRepository(ref.watch(dioProvider)),
);
