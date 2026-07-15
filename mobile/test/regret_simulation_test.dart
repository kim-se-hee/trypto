import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/features/regret/regret_simulation.dart';
import 'package:trypto/models/enums.dart';
import 'package:trypto/models/regret.dart';

AssetHistoryPoint _point(double actual, double ruleFollowed, double btc) =>
    AssetHistoryPoint(
      snapshotDate: DateTime(2026, 7, 15),
      actualAsset: actual,
      ruleFollowedAsset: ruleFollowed,
      btcHoldAsset: btc,
    );

ViolationDetail _violation(double profitLoss) => ViolationDetail(
  violationDetailId: 1,
  coinSymbol: 'BTC',
  violatedRules: const [RuleType.chaseBuyBan],
  profitLoss: profitLoss,
  occurredAt: DateTime(2026, 7, 15),
);

void main() {
  group('가중치', () {
    test('5종의 합이 정확히 1.0 이다', () {
      expect(totalRuleWeight(ruleImpactWeights.keys.toSet()), 1.0);
    });

    test('알 수 없는 규칙은 가중치가 0 이다', () {
      expect(totalRuleWeight({RuleType.unknown}), 0);
    });

    test('부분 집합의 가중치를 더한다', () {
      expect(
        totalRuleWeight({RuleType.lossCut, RuleType.chaseBuyBan}),
        closeTo(0.55, 1e-9),
      );
    });
  });

  group('simulationLine', () {
    final history = [
      _point(1000000, 1200000, 1100000),
      _point(900000, 1300000, 1150000),
    ];

    test('전부 켜면 서버의 규칙 준수 곡선과 정확히 일치한다', () {
      final line = simulationLine(history, ruleImpactWeights.keys.toSet());
      expect(line, [1200000, 1300000]);
    });

    test('전부 끄면 실제 곡선과 같다', () {
      expect(simulationLine(history, {}), [1000000, 900000]);
    });

    test('가중치 합만큼 선형 보간하고 반올림한다', () {
      // 0.30 + 0.25 = 0.55
      final line = simulationLine(history, {
        RuleType.lossCut,
        RuleType.chaseBuyBan,
      });
      // 1000000 + (1200000-1000000)*0.55 = 1110000
      // 900000  + (1300000-900000)*0.55  = 1120000
      expect(line, [1110000, 1120000]);
    });

    test('소수는 round 로 다듬는다', () {
      final line = simulationLine(
        [_point(100, 101, 0)],
        {RuleType.averagingDownLimit}, // 0.10 → 100.1
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
