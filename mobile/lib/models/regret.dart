import 'package:json_annotation/json_annotation.dart';

import '../core/json/converters.dart';
import 'enums.dart';

part 'regret.g.dart';

/// `GET /api/rounds/{roundId}/regret`
///
/// 리포트는 **야간 배치 산출물**이다. 배치 전에는 서버가 `RegretReport.empty()` 를 200 으로
/// 내리는데, 이 빈 리포트는 **`reportId`·`analysisStart`·`analysisEnd` 가 null** 이다
/// (`RegretReport.java:62-73` 의 builder 가 세 필드를 채우지 않는다). 사양서 §1.6.8 표는 이 셋을
/// 비-nullable 로 적었으나 실제 응답과 다르다 — 비-nullable 로 선언하면 배치 전 사용자 전원이
/// 복기 화면에서 파싱 오류를 맞는다.
@JsonSerializable(createToJson: false)
class RegretReport {
  const RegretReport({
    required this.roundId,
    required this.exchangeId,
    required this.exchangeName,
    required this.currency,
    required this.totalViolations,
    required this.missedProfit,
    required this.actualProfitRate,
    required this.ruleFollowedProfitRate,
    required this.ruleImpacts,
    required this.violationDetails,
    this.reportId,
    this.analysisStart,
    this.analysisEnd,
  });

  factory RegretReport.fromJson(Map<String, dynamic> json) =>
      _$RegretReportFromJson(json);

  /// 배치 전 빈 리포트에서는 null 이다. 집계 전 안내를 띄우는 판별식이 된다.
  final int? reportId;

  final int roundId;
  final int exchangeId;
  final String exchangeName;
  final String currency;
  final int totalViolations;

  @NullableLocalDateConverter()
  final DateTime? analysisStart;

  @NullableLocalDateConverter()
  final DateTime? analysisEnd;

  final double missedProfit;
  final double actualProfitRate;
  final double ruleFollowedProfitRate;
  final List<RuleImpact> ruleImpacts;
  final List<ViolationDetail> violationDetails;

  /// 배치가 아직 이 라운드를 집계하지 않았다.
  bool get isEmpty => reportId == null;
}

@JsonSerializable(createToJson: false)
class RuleImpact {
  const RuleImpact({
    required this.violationCount,
    this.ruleImpactId,
    this.ruleId,
    this.ruleType,
    this.thresholdValue,
    this.thresholdUnit,
    this.totalLossAmount,
    this.impactGap,
  });

  factory RuleImpact.fromJson(Map<String, dynamic> json) =>
      _$RuleImpactFromJson(json);

  final int? ruleImpactId;
  final int? ruleId;

  /// 서버가 `result.ruleType() != null ? name() : null` 로 내리므로 null 이 가능하다.
  @JsonKey(unknownEnumValue: RuleType.unknown)
  final RuleType? ruleType;

  final double? thresholdValue;

  /// 웹은 이 값을 쓰지 않고 로컬 상수 표(`%`/`회`)를 쓴다. 표시 계층에서 결정한다.
  final String? thresholdUnit;

  final int violationCount;
  final double? totalLossAmount;
  final double? impactGap;
}

@JsonSerializable(createToJson: false)
class ViolationDetail {
  const ViolationDetail({
    required this.violationDetailId,
    required this.coinSymbol,
    required this.violatedRules,
    required this.profitLoss,
    required this.occurredAt,
    this.orderId,
  });

  factory ViolationDetail.fromJson(Map<String, dynamic> json) =>
      _$ViolationDetailFromJson(json);

  final int violationDetailId;
  final int? orderId;
  final String coinSymbol;

  /// enum 이름 문자열 배열로 온다(`["CHASE_BUY_BAN", ...]`).
  @JsonKey(unknownEnumValue: RuleType.unknown)
  final List<RuleType> violatedRules;

  final double profitLoss;

  @KstDateTimeConverter()
  final DateTime occurredAt;
}

/// `GET /api/rounds/{roundId}/regret/chart`
///
/// 웹은 서버가 주는 [totalDays] 를 무시하고 `assetHistory.length` 로 다시 계산한다. 서버 값을 쓴다.
@JsonSerializable(createToJson: false)
class RegretChart {
  const RegretChart({
    required this.roundId,
    required this.exchangeId,
    required this.exchangeName,
    required this.currency,
    required this.totalDays,
    required this.assetHistory,
    required this.violationMarkers,
  });

  factory RegretChart.fromJson(Map<String, dynamic> json) =>
      _$RegretChartFromJson(json);

  final int roundId;
  final int exchangeId;
  final String exchangeName;
  final String currency;
  final int totalDays;
  final List<AssetHistoryPoint> assetHistory;
  final List<ViolationMarker> violationMarkers;
}

/// 자산 곡선 3선. 시간대 변환을 하지 않는 `LocalDate` 다.
@JsonSerializable(createToJson: false)
class AssetHistoryPoint {
  const AssetHistoryPoint({
    required this.snapshotDate,
    required this.actualAsset,
    required this.ruleFollowedAsset,
    required this.btcHoldAsset,
  });

  factory AssetHistoryPoint.fromJson(Map<String, dynamic> json) =>
      _$AssetHistoryPointFromJson(json);

  @LocalDateConverter()
  final DateTime snapshotDate;

  final double actualAsset;
  final double ruleFollowedAsset;
  final double btcHoldAsset;
}

@JsonSerializable(createToJson: false)
class ViolationMarker {
  const ViolationMarker({required this.snapshotDate, required this.assetValue});

  factory ViolationMarker.fromJson(Map<String, dynamic> json) =>
      _$ViolationMarkerFromJson(json);

  @LocalDateConverter()
  final DateTime snapshotDate;

  final double assetValue;
}
