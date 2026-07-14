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

/// 라운드와 거래소가 함께 바뀌므로 키는 값 동등 레코드다. 거래소를 바꾸면 provider 키가 바뀌어
/// 이전 리포트가 화면에 남지 않는다.
typedef RegretRequest = ({int roundId, int exchangeId});

/// 리포트와 차트는 항상 함께 쓰인다. 하나만 오면 히어로와 차트의 기준이 어긋난다.
class RegretBundle {
  const RegretBundle({required this.report, required this.chart});

  final RegretReport report;
  final RegretChart chart;
}

final regretProvider = FutureProvider.family<RegretBundle, RegretRequest>((
  ref,
  request,
) async {
  final repository = ref.watch(regretRepositoryProvider);
  final results = await Future.wait<Object>([
    repository.getReport(
      roundId: request.roundId,
      exchangeId: request.exchangeId,
    ),
    repository.getChart(
      roundId: request.roundId,
      exchangeId: request.exchangeId,
    ),
  ]);
  return RegretBundle(
    report: results[0] as RegretReport,
    chart: results[1] as RegretChart,
  );
});
