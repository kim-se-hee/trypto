import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../models/regret.dart';

class RegretRepository {
  const RegretRepository(this._dio);

  final Dio _dio;

  /// 라운드 단위 조회다. 거래소를 가려서 요청하지 않는다 — 서버가 라운드에 속한 거래소를 모두
  /// 합쳐 원화로 내린다.
  ///
  /// 배치 전에는 서버가 **빈 리포트를 200 으로** 내린다(`RegretReport.isEmpty`). 오류가 아니다.
  Future<RegretReport> getReport({required int roundId}) => apiCall(() async {
    final response = await _dio.get('/api/rounds/$roundId/regret');
    return RegretReport.fromJson(response.data as Map<String, dynamic>);
  });

  Future<RegretChart> getChart({required int roundId}) => apiCall(() async {
    final response = await _dio.get('/api/rounds/$roundId/regret/chart');
    return RegretChart.fromJson(response.data as Map<String, dynamic>);
  });
}

final regretRepositoryProvider = Provider<RegretRepository>(
  (ref) => RegretRepository(ref.watch(dioProvider)),
);

/// 리포트와 차트는 항상 함께 쓰인다. 하나만 오면 히어로와 차트의 기준이 어긋난다.
class RegretBundle {
  const RegretBundle({required this.report, required this.chart});

  final RegretReport report;
  final RegretChart chart;
}

/// 키는 라운드 ID 다. 라운드를 바꾸면 provider 키가 바뀌어 이전 리포트가 화면에 남지 않는다.
final regretProvider = FutureProvider.family<RegretBundle, int>((
  ref,
  roundId,
) async {
  final repository = ref.watch(regretRepositoryProvider);
  final results = await Future.wait<Object>([
    repository.getReport(roundId: roundId),
    repository.getChart(roundId: roundId),
  ]);
  return RegretBundle(
    report: results[0] as RegretReport,
    chart: results[1] as RegretChart,
  );
});
