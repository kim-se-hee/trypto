import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/format/server_time.dart';
import 'package:trypto/features/regret/regret_simulation.dart';
import 'package:trypto/models/enums.dart';
import 'package:trypto/models/regret.dart';

AssetHistoryPoint _point(
  double actual,
  double ruleFollowed,
  double btc, {
  DateTime? date,
}) => AssetHistoryPoint(
  snapshotDate: date ?? DateTime(2026, 7, 15),
  actualAsset: actual,
  ruleFollowedAsset: ruleFollowed,
  btcHoldAsset: btc,
);

/// 서버가 내려주는 문자열을 그대로 거쳐 만든다. 기기 시간대에 흔들리지 않게 하기 위함이다.
ViolationDetail _violation(
  double profitLoss, {
  String occurredAt = '2026-07-15T10:00:00',
  List<ViolatedRule> rules = const [],
}) => ViolationDetail(
  violationDetailId: 1,
  coinSymbol: 'BTC',
  violatedRules: rules,
  profitLoss: profitLoss,
  occurredAt: ServerTime.parseKst(occurredAt),
);

ViolatedRule _rule(RuleType ruleType, double lossAmount) =>
    ViolatedRule(ruleType: ruleType, lossAmount: lossAmount);

void main() {
  group('simulationLine', () {
    final history = [
      _point(1000000, 1200000, 1100000, date: DateTime(2026, 7, 14)),
      _point(900000, 1300000, 1150000, date: DateTime(2026, 7, 15)),
    ];

    // 7/14 추격매수 20만, 7/15 손절 20만 → 누적 20만 → 40만. history 의 규칙 준수 곡선과 같다.
    final violations = [
      _violation(
        -200000,
        occurredAt: '2026-07-14T09:00:00',
        rules: [_rule(RuleType.chaseBuyBan, 200000)],
      ),
      _violation(
        -200000,
        occurredAt: '2026-07-15T09:00:00',
        rules: [_rule(RuleType.lossCut, 200000)],
      ),
    ];

    test('전부 켜면 서버의 규칙 준수 곡선과 모든 지점에서 일치한다', () {
      final line = simulationLine(history, {
        RuleType.chaseBuyBan,
        RuleType.lossCut,
      }, violations);
      expect(line, [for (final point in history) point.ruleFollowedAsset]);
    });

    test('전부 끄면 실제 곡선과 같다', () {
      expect(simulationLine(history, {}, violations), [1000000, 900000]);
    });

    test('켜 둔 규칙 몫만 누적한다', () {
      final line = simulationLine(history, {RuleType.chaseBuyBan}, violations);
      // 7/15 의 위반은 손절이라 꺼져 있고, 7/14 의 20만만 남는다.
      expect(line, [1200000, 1100000]);
    });

    test('위반이 없는 날은 직전 누적값을 유지한다', () {
      final line = simulationLine(
        [
          _point(1000000, 0, 0, date: DateTime(2026, 7, 14)),
          _point(1000000, 0, 0, date: DateTime(2026, 7, 15)),
          _point(1000000, 0, 0, date: DateTime(2026, 7, 16)),
        ],
        {RuleType.chaseBuyBan},
        violations,
      );
      expect(line, [1200000, 1200000, 1200000]);
    });

    test('위반 손익이 음수면 실제 자산 아래로 내려간다', () {
      final line = simulationLine(
        [_point(1000000, 0, 0)],
        {RuleType.chaseBuyBan},
        [
          _violation(
            60000,
            rules: [_rule(RuleType.chaseBuyBan, -60000)],
          ),
        ],
      );
      expect(line, [940000]);
    });

    test('그래프 시작일 이전에 발생한 위반은 첫 점에 포함된다', () {
      final line = simulationLine(
        [_point(1000000, 0, 0)],
        {RuleType.chaseBuyBan},
        [
          _violation(
            -50000,
            occurredAt: '2026-07-01T09:00:00',
            rules: [_rule(RuleType.chaseBuyBan, 50000)],
          ),
        ],
      );
      expect(line, [1050000]);
    });

    test('위반이 없으면 실제 곡선과 같다', () {
      expect(simulationLine(history, {RuleType.chaseBuyBan}, []), [
        1000000,
        900000,
      ]);
    });

    test('소수는 round 로 다듬는다', () {
      final line = simulationLine(
        [_point(100, 0, 0)],
        {RuleType.chaseBuyBan},
        [
          _violation(
            -0.4,
            rules: [_rule(RuleType.chaseBuyBan, 0.4)],
          ),
        ],
      );
      expect(line, [100]);
    });
  });

  group('btcHoldProfitRate', () {
    test('첫 값과 마지막 값으로 수익률을 계산한다', () {
      final rate = btcHoldProfitRate([
        _point(0, 0, 1000000),
        _point(0, 0, 1250000),
      ]);
      expect(rate, closeTo(25, 1e-9));
    });

    test('스냅샷이 2개 미만이면 계산하지 않는다', () {
      expect(btcHoldProfitRate([_point(0, 0, 1000)]), isNull);
      expect(btcHoldProfitRate([]), isNull);
    });

    test('시작 평가액이 0 이면 계산하지 않는다', () {
      expect(btcHoldProfitRate([_point(0, 0, 0), _point(0, 0, 100)]), isNull);
    });
  });

  group('labelTickInterval', () {
    test('기간 구간별 라벨 간격', () {
      expect(labelTickInterval(14), 1);
      expect(labelTickInterval(15), 7);
      expect(labelTickInterval(60), 7);
      expect(labelTickInterval(61), 14);
      expect(labelTickInterval(180), 14);
      expect(labelTickInterval(181), 30);
    });
  });

  group('chartYRange', () {
    test('표시 중인 모든 시리즈의 min/max 에 10% 패딩을 덧댄다', () {
      final range = chartYRange([
        [100, 200],
        [50, 150],
      ]);
      // min 50, max 200 → 패딩 15
      expect(range.min, closeTo(35, 1e-9));
      expect(range.max, closeTo(215, 1e-9));
    });

    test('범위가 0 이면 패딩은 1 이다', () {
      final range = chartYRange([
        [100, 100],
      ]);
      expect(range.min, 99);
      expect(range.max, 101);
    });

    test('데이터가 없으면 기본 범위를 준다', () {
      final range = chartYRange([]);
      expect(range.min, 0);
      expect(range.max, 1);
    });
  });

  group('filterViolations', () {
    final violations = [_violation(-1000), _violation(0), _violation(2000)];

    test('profitLoss == 0 은 수익으로 분류된다', () {
      expect(filterViolations(violations, ViolationFilter.profit).length, 2);
      expect(filterViolations(violations, ViolationFilter.loss).length, 1);
      expect(filterViolations(violations, ViolationFilter.all).length, 3);
    });

    test('정렬하지 않고 서버 순서를 유지한다', () {
      final all = filterViolations(violations, ViolationFilter.all);
      expect(all.map((v) => v.profitLoss), [-1000, 0, 2000]);
    });
  });
}
