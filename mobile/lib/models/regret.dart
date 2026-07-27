import 'package:json_annotation/json_annotation.dart';

import '../core/json/converters.dart';
import 'enums.dart';

part 'regret.g.dart';

/// 라운드 단위로 합친 금액의 통화. 요약·규칙별 손실·자산 곡선이 모두 원화다 — 바이낸스(USDT)
/// 몫은 서버가 고정 환율로 환산해 더한 뒤 내려준다. 거래소 기축통화는 위반 거래 한 건에만 남는다.
const String roundCurrency = 'KRW';

/// `GET /api/rounds/{roundId}/regret`
///
/// 라운드에 속한 모든 거래소의 리포트를 서버가 하나로 합쳐 내린다. 요약과 규칙별 손실의 금액은
/// 전부 원화이며, 거래소별 기축통화는 위반 거래 한 건 한 건에만 남는다.
///
/// 리포트는 **야간 배치 산출물**이다. 배치 전에는 서버가 빈 리포트를 200 으로 내리는데, 이때
/// **`analysisStart`·`analysisEnd` 가 null** 이다 — 합칠 리포트가 하나도 없어 분석 구간을 정할 수
/// 없기 때문이다. 비-nullable 로 선언하면 배치 전 사용자 전원이 복기 화면에서 파싱 오류를 맞는다.
@JsonSerializable(createToJson: false)
class RegretReport {
  const RegretReport({
    required this.roundId,
    required this.totalViolations,
    required this.totalViolationLoss,
    required this.actualAsset,
    required this.ruleFollowedAsset,
    required this.ruleImpacts,
    required this.violationDetails,
    this.analysisStart,
    this.analysisEnd,
  });

  factory RegretReport.fromJson(Map<String, dynamic> json) =>
      _$RegretReportFromJson(json);

  final int roundId;
  final int totalViolations;

  /// 배치 전 빈 리포트에서는 null 이다. 집계 전 안내를 띄우는 판별식이 된다.
  @NullableLocalDateConverter()
  final DateTime? analysisStart;

  @NullableLocalDateConverter()
  final DateTime? analysisEnd;

  /// 위반 손실 합계(원화). **양수가 손해**다. 음수면 원칙을 어긴 쪽이 오히려 이득이었다.
  final double totalViolationLoss;

  final double actualAsset;
  final double ruleFollowedAsset;
  final List<RuleImpact> ruleImpacts;
  final List<ViolationDetail> violationDetails;

  /// 배치가 아직 이 라운드를 집계하지 않았다.
  bool get isEmpty => analysisStart == null;
}

/// 라운드에 설정된 원칙 하나와 그 원칙을 어긴 결과. 거래소별 행을 원칙 기준으로 더한 값이라
/// 행 식별자가 없다 — 원칙은 [ruleId] 로 구분한다.
@JsonSerializable(createToJson: false)
class RuleImpact {
  const RuleImpact({
    required this.violationCount,
    this.ruleId,
    this.ruleType,
    this.thresholdValue,
    this.thresholdUnit,
    this.totalLossAmount,
  });

  factory RuleImpact.fromJson(Map<String, dynamic> json) =>
      _$RuleImpactFromJson(json);

  final int? ruleId;

  /// 서버가 `result.ruleType() != null ? name() : null` 로 내리므로 null 이 가능하다.
  @JsonKey(unknownEnumValue: RuleType.unknown)
  final RuleType? ruleType;

  final double? thresholdValue;

  /// 웹은 이 값을 쓰지 않고 로컬 상수 표(`%`/`회`)를 쓴다. 표시 계층에서 결정한다.
  final String? thresholdUnit;

  final int violationCount;

  /// 이 원칙만 지켰다면 자산에 남았을 금액(원화). 거래소를 가리지 않고 합친 값이다.
  final double? totalLossAmount;
}

/// 위반 거래가 어긴 원칙 하나와 그 원칙 몫의 위반 손실.
@JsonSerializable(createToJson: false)
class ViolatedRule {
  const ViolatedRule({
    required this.ruleType,
    required this.lossAmount,
    required this.lossAmountKrw,
  });

  factory ViolatedRule.fromJson(Map<String, dynamic> json) =>
      _$ViolatedRuleFromJson(json);

  @JsonKey(unknownEnumValue: RuleType.unknown)
  final RuleType ruleType;

  /// 발생 거래소의 기축통화 단위. 양수면 손해, 음수면 어긴 쪽이 결과적으로 이득이었다.
  final double lossAmount;

  /// 같은 값의 원화 환산액. 거래소가 섞이는 그래프 누적 계산은 이쪽을 쓴다.
  final double lossAmountKrw;
}

/// 한 건은 주문 하나다. 한 주문이 여러 원칙을 어겼으면 [violatedRules] 에 나란히 담겨 온다.
@JsonSerializable(createToJson: false)
class ViolationDetail {
  const ViolationDetail({
    required this.violationDetailId,
    required this.exchangeId,
    required this.exchangeName,
    required this.currency,
    required this.coinSymbol,
    required this.violatedRules,
    required this.totalLossAmount,
    required this.totalLossAmountKrw,
    required this.occurredAt,
    this.orderId,
  });

  factory ViolationDetail.fromJson(Map<String, dynamic> json) =>
      _$ViolationDetailFromJson(json);

  final int violationDetailId;
  final int? orderId;

  final int exchangeId;
  final String exchangeName;

  /// 발생 거래소의 기축통화(KRW/USDT). 목록은 이 통화로 금액을 보여준다.
  final String currency;

  final String coinSymbol;

  /// 원칙 이름과 그 원칙 몫의 위반 손실이 짝으로 온다. 복기 그래프의 원칙 토글이 이 금액을 쓴다.
  final List<ViolatedRule> violatedRules;

  /// 이 행의 위반 손실 합계. **양수가 손해**다.
  final double totalLossAmount;

  final double totalLossAmountKrw;

  @KstDateTimeConverter()
  final DateTime occurredAt;
}

/// `GET /api/rounds/{roundId}/regret/chart`
///
/// 라운드 전체를 합친 곡선이라 금액은 모두 원화다([roundCurrency]).
@JsonSerializable(createToJson: false)
class RegretChart {
  const RegretChart({
    required this.roundId,
    required this.totalDays,
    required this.assetHistory,
    required this.violationMarkers,
  });

  factory RegretChart.fromJson(Map<String, dynamic> json) =>
      _$RegretChartFromJson(json);

  final int roundId;

  /// 첫 스냅샷과 마지막 스냅샷의 날짜 차이 + 1. 스냅샷 개수와 다를 수 있으므로 서버 값을 쓴다.
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
