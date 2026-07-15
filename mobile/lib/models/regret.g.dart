// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'regret.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RegretReport _$RegretReportFromJson(Map<String, dynamic> json) => RegretReport(
  roundId: (json['roundId'] as num).toInt(),
  exchangeId: (json['exchangeId'] as num).toInt(),
  exchangeName: json['exchangeName'] as String,
  currency: json['currency'] as String,
  totalViolations: (json['totalViolations'] as num).toInt(),
  missedProfit: (json['missedProfit'] as num).toDouble(),
  actualProfitRate: (json['actualProfitRate'] as num).toDouble(),
  ruleFollowedProfitRate: (json['ruleFollowedProfitRate'] as num).toDouble(),
  ruleImpacts: (json['ruleImpacts'] as List<dynamic>)
      .map((e) => RuleImpact.fromJson(e as Map<String, dynamic>))
      .toList(),
  violationDetails: (json['violationDetails'] as List<dynamic>)
      .map((e) => ViolationDetail.fromJson(e as Map<String, dynamic>))
      .toList(),
  reportId: (json['reportId'] as num?)?.toInt(),
  analysisStart: const NullableLocalDateConverter().fromJson(
    json['analysisStart'] as String?,
  ),
  analysisEnd: const NullableLocalDateConverter().fromJson(
    json['analysisEnd'] as String?,
  ),
);

RuleImpact _$RuleImpactFromJson(Map<String, dynamic> json) => RuleImpact(
  violationCount: (json['violationCount'] as num).toInt(),
  ruleImpactId: (json['ruleImpactId'] as num?)?.toInt(),
  ruleId: (json['ruleId'] as num?)?.toInt(),
  ruleType: $enumDecodeNullable(
    _$RuleTypeEnumMap,
    json['ruleType'],
    unknownValue: RuleType.unknown,
  ),
  thresholdValue: (json['thresholdValue'] as num?)?.toDouble(),
  thresholdUnit: json['thresholdUnit'] as String?,
  totalLossAmount: (json['totalLossAmount'] as num?)?.toDouble(),
  impactGap: (json['impactGap'] as num?)?.toDouble(),
);

const _$RuleTypeEnumMap = {
  RuleType.lossCut: 'LOSS_CUT',
  RuleType.profitTake: 'PROFIT_TAKE',
  RuleType.chaseBuyBan: 'CHASE_BUY_BAN',
  RuleType.averagingDownLimit: 'AVERAGING_DOWN_LIMIT',
  RuleType.overtradingLimit: 'OVERTRADING_LIMIT',
  RuleType.unknown: 'UNKNOWN',
};

ViolationDetail _$ViolationDetailFromJson(Map<String, dynamic> json) =>
    ViolationDetail(
      violationDetailId: (json['violationDetailId'] as num).toInt(),
      coinSymbol: json['coinSymbol'] as String,
      violatedRules: (json['violatedRules'] as List<dynamic>)
          .map(
            (e) => $enumDecode(
              _$RuleTypeEnumMap,
              e,
              unknownValue: RuleType.unknown,
            ),
          )
          .toList(),
      profitLoss: (json['profitLoss'] as num).toDouble(),
      occurredAt: const KstDateTimeConverter().fromJson(
        json['occurredAt'] as String,
      ),
      orderId: (json['orderId'] as num?)?.toInt(),
    );

RegretChart _$RegretChartFromJson(Map<String, dynamic> json) => RegretChart(
  roundId: (json['roundId'] as num).toInt(),
  exchangeId: (json['exchangeId'] as num).toInt(),
  exchangeName: json['exchangeName'] as String,
  currency: json['currency'] as String,
  totalDays: (json['totalDays'] as num).toInt(),
  assetHistory: (json['assetHistory'] as List<dynamic>)
      .map((e) => AssetHistoryPoint.fromJson(e as Map<String, dynamic>))
      .toList(),
  violationMarkers: (json['violationMarkers'] as List<dynamic>)
      .map((e) => ViolationMarker.fromJson(e as Map<String, dynamic>))
      .toList(),
);

AssetHistoryPoint _$AssetHistoryPointFromJson(Map<String, dynamic> json) =>
    AssetHistoryPoint(
      snapshotDate: const LocalDateConverter().fromJson(
        json['snapshotDate'] as String,
      ),
      actualAsset: (json['actualAsset'] as num).toDouble(),
      ruleFollowedAsset: (json['ruleFollowedAsset'] as num).toDouble(),
      btcHoldAsset: (json['btcHoldAsset'] as num).toDouble(),
    );

ViolationMarker _$ViolationMarkerFromJson(Map<String, dynamic> json) =>
    ViolationMarker(
      snapshotDate: const LocalDateConverter().fromJson(
        json['snapshotDate'] as String,
      ),
      assetValue: (json['assetValue'] as num).toDouble(),
    );
