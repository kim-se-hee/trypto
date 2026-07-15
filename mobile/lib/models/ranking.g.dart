// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'ranking.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RankingItem _$RankingItemFromJson(Map<String, dynamic> json) => RankingItem(
  rank: (json['rank'] as num).toInt(),
  userId: (json['userId'] as num).toInt(),
  nickname: json['nickname'] as String,
  profitRate: (json['profitRate'] as num).toDouble(),
  tradeCount: (json['tradeCount'] as num).toInt(),
);

MyRanking _$MyRankingFromJson(Map<String, dynamic> json) => MyRanking(
  rank: (json['rank'] as num).toInt(),
  nickname: json['nickname'] as String,
  profitRate: (json['profitRate'] as num).toDouble(),
  tradeCount: (json['tradeCount'] as num).toInt(),
);

RankingStats _$RankingStatsFromJson(Map<String, dynamic> json) => RankingStats(
  totalParticipants: (json['totalParticipants'] as num).toInt(),
  maxProfitRate: (json['maxProfitRate'] as num).toDouble(),
  avgProfitRate: (json['avgProfitRate'] as num).toDouble(),
);

RankerPortfolio _$RankerPortfolioFromJson(Map<String, dynamic> json) =>
    RankerPortfolio(
      userId: (json['userId'] as num).toInt(),
      nickname: json['nickname'] as String,
      rank: (json['rank'] as num).toInt(),
      profitRate: (json['profitRate'] as num).toDouble(),
      holdings: (json['holdings'] as List<dynamic>)
          .map((e) => RankerHolding.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

RankerHolding _$RankerHoldingFromJson(Map<String, dynamic> json) =>
    RankerHolding(
      coinSymbol: json['coinSymbol'] as String,
      exchangeName: json['exchangeName'] as String,
      assetRatio: (json['assetRatio'] as num).toDouble(),
      profitRate: (json['profitRate'] as num).toDouble(),
    );
