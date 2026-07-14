import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/api/query.dart';
import '../../core/format/server_time.dart';
import '../../models/cursor_page.dart';
import '../../models/enums.dart';
import '../../models/ranking.dart';

class RankingRepository {
  const RankingRepository(this._dio);

  final Dio _dio;

  /// 비인증 경로다(정확히 `/api/rankings` 만). 커서 이름이 `cursorRank` 다.
  /// [referenceDate] 는 서버가 `yyyy-MM-dd` 로만 받는다.
  Future<CursorPage<RankingItem>> getRankings({
    required RankingPeriod period,
    DateTime? referenceDate,
    int? cursorRank,
    int size = 20,
  }) => apiCall(() async {
    final response = await _dio.get(
      '/api/rankings',
      queryParameters: query({
        'period': period.wire,
        'referenceDate': referenceDate == null
            ? null
            : ServerTime.formatLocalDate(referenceDate),
        'cursorRank': cursorRank,
        'size': size,
      }),
    );
    return CursorPage.fromJson(
      response.data as Map<String, dynamic>,
      (json) => RankingItem.fromJson(json as Map<String, dynamic>),
    );
  });

  /// 인증 필요. **성공이면서 `data` 가 null** 일 수 있다(랭킹 미집계 사용자).
  Future<MyRanking?> getMyRanking(RankingPeriod period) => apiCall(() async {
    final response = await _dio.get(
      '/api/rankings/me',
      queryParameters: query({'period': period.wire}),
    );
    final data = response.data;
    if (data == null) return null;
    return MyRanking.fromJson(data as Map<String, dynamic>);
  });

  /// 비인증 경로다.
  Future<RankingStats> getStats(RankingPeriod period) => apiCall(() async {
    final response = await _dio.get(
      '/api/rankings/stats',
      queryParameters: query({'period': period.wire}),
    );
    return RankingStats.fromJson(response.data as Map<String, dynamic>);
  });

  /// 인증 필요. 집계가 없으면 404 `RANKING_NOT_FOUND`, 101위 이하이면
  /// 403 `PORTFOLIO_VIEW_NOT_ALLOWED` 다. 후자는 호출부가 순위로 선제 차단한다.
  Future<RankerPortfolio> getRankerPortfolio({
    required int userId,
    required RankingPeriod period,
  }) => apiCall(() async {
    final response = await _dio.get(
      '/api/rankings/$userId/portfolio',
      queryParameters: query({'period': period.wire}),
    );
    return RankerPortfolio.fromJson(response.data as Map<String, dynamic>);
  });
}

final rankingRepositoryProvider = Provider<RankingRepository>(
  (ref) => RankingRepository(ref.watch(dioProvider)),
);
