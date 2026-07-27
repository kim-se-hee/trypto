import 'dart:math' as math;

import '../../core/format/server_time.dart';
import '../../models/enums.dart';
import '../../models/regret.dart';

/// 복기 화면의 순수 계산 전량. 위젯 없이 테스트한다.
///
/// **웹과 값이 일치해야 한다**(사양서 §6.3.4). 누적식·반올림을 그대로 이식한다.

/// 켜 둔 규칙이 실제로 유발한 위반 손실을 발생일 순으로 누적해 그날의 실제 자산에 더한다.
///
/// 서버가 전체 규칙 곡선(`ruleFollowedAsset`)을 만드는 방식과 같은 계산이므로, 규칙을 모두 켜면
/// 두 곡선이 모든 지점에서 일치한다. 위반이 없는 날은 직전 누적값을 유지해 계단 모양이 된다.
///
/// 곡선은 라운드 전체를 원화로 합친 것이므로 거래소 기축통화가 아니라 원화 환산액을 누적한다.
List<double> simulationLine(
  List<AssetHistoryPoint> history,
  Set<RuleType> enabled,
  List<ViolationDetail> violations,
) {
  final dailyLosses =
      [
        for (final violation in violations)
          (
            date: ServerTime.kstDate(violation.occurredAt),
            amount: _enabledLoss(violation, enabled),
          ),
      ]..sort((a, b) => a.date.compareTo(b.date));

  final line = <double>[];
  var cumulative = 0.0;
  var next = 0;

  for (final point in history) {
    // 그래프 시작일 이전에 발생한 위반은 첫 점에서 한꺼번에 반영된다.
    while (next < dailyLosses.length &&
        !dailyLosses[next].date.isAfter(point.snapshotDate)) {
      cumulative += dailyLosses[next].amount;
      next += 1;
    }
    line.add((point.actualAsset + cumulative).roundToDouble());
  }

  return line;
}

double _enabledLoss(ViolationDetail violation, Set<RuleType> enabled) {
  var total = 0.0;
  for (final rule in violation.violatedRules) {
    if (enabled.contains(rule.ruleType)) {
      total += rule.lossAmountKrw;
    }
  }
  return total;
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
