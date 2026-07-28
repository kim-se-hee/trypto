import 'dart:math' as math;

import '../../core/format/server_time.dart';
import '../../models/enums.dart';
import '../../models/regret.dart';

/// 복기 화면의 순수 계산 전량. 위젯 없이 테스트한다.
///
/// **웹과 값이 일치해야 한다**(사양서 §6.3.4). 누적식·반올림을 그대로 이식한다.

/// 켜 둔 규칙이 실제로 유발한 위반 손실만 골라 그날의 실제 자산에 반영한다.
///
/// 아직 매도되지 않은 몫은 금액을 그대로 더하고, 매도로 확정된 몫은 실현일의 자산 대비 비율(배수)로
/// 환산해 곱한다. 실현된 돈은 지갑에 섞여 이후 투자와 함께 굴러가므로, 확정 금액을 계속 빼면 시장이
/// 이미 가져간 금액을 한 번 더 빼는 이중 차감이 된다. 긴급 충전은 위반과 무관한 새 돈이라 그날의
/// 배수를 1 쪽으로 되돌린다.
///
/// 서버가 전체 규칙 곡선(`ruleFollowedAsset`)을 만드는 방식과 같은 계산이므로, 규칙을 모두 켜면
/// 두 곡선이 모든 지점에서 일치한다. 규칙 조합이 바뀌면 배수도 처음부터 다시 굴린다.
///
/// 곡선은 라운드 전체를 원화로 합친 것이므로 거래소 기축통화가 아니라 원화 환산액을 쓴다.
List<double> simulationLine(
  List<AssetHistoryPoint> history,
  Set<RuleType> enabled,
  List<ViolationDetail> violations, [
  List<EmergencyCharge> emergencyCharges = const [],
]) {
  final occurredLosses =
      [
        for (final violation in violations)
          (
            date: ServerTime.kstDate(violation.occurredAt),
            amount: _enabledLoss(violation, enabled),
          ),
      ]..sort((a, b) => a.date.compareTo(b.date));

  final realizations =
      [
        for (final violation in violations)
          for (final rule in violation.violatedRules)
            if (enabled.contains(rule.ruleType))
              for (final realized in rule.realizedPortions)
                (date: realized.realizedOn, amount: realized.lossAmountKrw),
      ]..sort((a, b) => a.date.compareTo(b.date));

  final charges = [...emergencyCharges]
    ..sort((a, b) => a.chargedDate.compareTo(b.chargedDate));

  final line = <double>[];
  var occurred = 0.0;
  var realized = 0.0;
  var multiplier = 1.0;
  var nextLoss = 0;
  var nextRealization = 0;
  var nextCharge = 0;

  for (final point in history) {
    // 그래프 시작일 이전에 발생한 위반과 실현도 첫 점에서 한꺼번에 반영된다.
    while (nextLoss < occurredLosses.length &&
        !occurredLosses[nextLoss].date.isAfter(point.snapshotDate)) {
      occurred += occurredLosses[nextLoss].amount;
      nextLoss += 1;
    }

    var realizedToday = 0.0;
    while (nextRealization < realizations.length &&
        !realizations[nextRealization].date.isAfter(point.snapshotDate)) {
      realizedToday += realizations[nextRealization].amount;
      nextRealization += 1;
    }

    var chargedToday = 0.0;
    while (nextCharge < charges.length &&
        !charges[nextCharge].chargedDate.isAfter(point.snapshotDate)) {
      chargedToday += charges[nextCharge].amount;
      nextCharge += 1;
    }

    realized += realizedToday;
    multiplier = _nextMultiplier(
      multiplier,
      point.actualAsset,
      chargedToday,
      realizedToday,
    );

    line.add(
      ((point.actualAsset + occurred - realized) * multiplier).roundToDouble(),
    );
  }

  return line;
}

/// 켜 둔 규칙 몫의 위반 손실 합계. 미실현분과 실현분을 모두 담은 총액이다.
double _enabledLoss(ViolationDetail violation, Set<RuleType> enabled) {
  var total = 0.0;
  for (final rule in violation.violatedRules) {
    if (enabled.contains(rule.ruleType)) {
      total += rule.lossAmountKrw;
    }
  }
  return total;
}

/// 실현도 충전도 없는 날은 배수가 그대로다. 자산은 음수가 될 수 없으므로 배수의 하한은 0 이다.
double _nextMultiplier(
  double multiplier,
  double totalAsset,
  double charged,
  double realized,
) {
  if (totalAsset <= 0 || (charged == 0 && realized == 0)) return multiplier;
  return math.max(
    0,
    (multiplier * (totalAsset - charged + realized) + charged) / totalAsset,
  );
}

/// BTC 홀드 벤치마크 수익률. 웹은 이 값을 `0%` 로 하드코딩해 두었다(사양서 §6.3.4).
/// 스냅샷이 2개 미만이거나 시작 평가액이 0 이면 계산할 수 없다.
double? btcHoldProfitRate(List<AssetHistoryPoint> history) {
  if (history.length < 2) return null;
  final first = history.first.btcHoldAsset;
  if (first == 0) return null;
  return (history.last.btcHoldAsset / first - 1) * 100;
}

/// X축 라벨 간격(일). 기간이 길수록 성기게 찍는다(사양서 §6.4.2).
int labelTickInterval(int totalDays) {
  if (totalDays <= 14) return 1;
  if (totalDays <= 60) return 7;
  if (totalDays <= 180) return 14;
  return 30;
}

/// 표시 중인 **모든 시리즈**의 min/max 에 `(max-min) × 0.1` 을 덧댄다. 범위가 0이면 패딩은 1이다.
({double min, double max}) chartYRange(List<List<double>> series) {
  var min = double.infinity;
  var max = double.negativeInfinity;
  for (final line in series) {
    for (final value in line) {
      min = math.min(min, value);
      max = math.max(max, value);
    }
  }
  if (min == double.infinity) return (min: 0, max: 1);

  final range = max - min;
  final padding = range == 0 ? 1.0 : range * 0.1;
  return (min: min - padding, max: max + padding);
}

/// 위반 거래의 손익 축 필터. 위반 손실은 **양수가 손해**이므로 손실은 0 초과이고,
/// **0 이하는 수익으로 분류된다** — 원칙을 어긴 쪽이 오히려 이득이었던 거래다.
enum ViolationFilter {
  all('전체'),
  loss('손실'),
  profit('수익');

  const ViolationFilter(this.label);

  final String label;

  bool matches(ViolationDetail violation) => switch (this) {
    ViolationFilter.all => true,
    ViolationFilter.loss => violation.totalLossAmount > 0,
    ViolationFilter.profit => violation.totalLossAmount <= 0,
  };
}

/// 위반 거래의 거래소 축 필터. null 이면 전 거래소다. 손익 축과 조합해서 쓴다.
List<ViolationDetail> filterViolations(
  List<ViolationDetail> violations,
  ViolationFilter filter, {
  int? exchangeId,
}) => [
  for (final violation in violations)
    if (filter.matches(violation) &&
        (exchangeId == null || violation.exchangeId == exchangeId))
      violation,
];

/// 라운드에서 위반이 실제로 일어난 거래소만 (ID, 이름) 으로 추린다. 서버 순서를 유지한다.
List<({int id, String name})> violatedExchanges(
  List<ViolationDetail> violations,
) {
  final names = <int, String>{};
  for (final violation in violations) {
    names.putIfAbsent(violation.exchangeId, () => violation.exchangeName);
  }
  return [
    for (final entry in names.entries) (id: entry.key, name: entry.value),
  ];
}
