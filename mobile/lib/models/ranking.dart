import 'package:json_annotation/json_annotation.dart';

part 'ranking.g.dart';

/// `GET /api/rankings` 의 항목 — 비인증으로 조회된다.
///
/// [profitRate] 는 **퍼센트 값 그 자체**다(12.34 = +12.34%). 코인 `changeRate`(0.0123 = 1.23%)와
/// 단위가 다르다(사양서 §1.8-5).
@JsonSerializable(createToJson: false)
class RankingItem {
  const RankingItem({
    required this.rank,
    required this.userId,
    required this.nickname,
    required this.profitRate,
    required this.tradeCount,
  });

  factory RankingItem.fromJson(Map<String, dynamic> json) =>
      _$RankingItemFromJson(json);

  final int rank;
  final int userId;
  final String nickname;
  final double profitRate;
  final int tradeCount;
}

/// `GET /api/rankings/me` — **성공(`SUCCESS`)이면서 `data` 가 `null`** 일 수 있다(미집계 사용자).
/// repository 가 `null` 을 그대로 돌려준다. `userId` 가 없다.
@JsonSerializable(createToJson: false)
class MyRanking {
  const MyRanking({
    required this.rank,
    required this.nickname,
    required this.profitRate,
    required this.tradeCount,
  });

  factory MyRanking.fromJson(Map<String, dynamic> json) =>
      _$MyRankingFromJson(json);

  final int rank;
  final String nickname;
  final double profitRate;
  final int tradeCount;
}

/// `GET /api/rankings/stats` — 비인증.
@JsonSerializable(createToJson: false)
class RankingStats {
  const RankingStats({
    required this.totalParticipants,
    required this.maxProfitRate,
    required this.avgProfitRate,
  });

  factory RankingStats.fromJson(Map<String, dynamic> json) =>
      _$RankingStatsFromJson(json);

  final int totalParticipants;
  final double maxProfitRate;
  final double avgProfitRate;
}

/// `GET /api/rankings/{userId}/portfolio`
///
/// 집계가 없으면 404 `RANKING_NOT_FOUND`, 101위 이하이면 403 `PORTFOLIO_VIEW_NOT_ALLOWED` 다.
/// 후자는 요청을 보내기 전에 순위로 선제 차단한다(계획서 §4.1.4).
@JsonSerializable(createToJson: false)
class RankerPortfolio {
  const RankerPortfolio({
    required this.userId,
    required this.nickname,
    required this.rank,
    required this.profitRate,
    required this.holdings,
  });

  factory RankerPortfolio.fromJson(Map<String, dynamic> json) =>
      _$RankerPortfolioFromJson(json);

  final int userId;
  final String nickname;
  final int rank;
  final double profitRate;
  final List<RankerHolding> holdings;
}

@JsonSerializable(createToJson: false)
class RankerHolding {
  const RankerHolding({
    required this.coinSymbol,
    required this.exchangeName,
    required this.assetRatio,
    required this.profitRate,
  });

  factory RankerHolding.fromJson(Map<String, dynamic> json) =>
      _$RankerHoldingFromJson(json);

  final String coinSymbol;
  final String exchangeName;
  final double assetRatio;
  final double profitRate;
}
