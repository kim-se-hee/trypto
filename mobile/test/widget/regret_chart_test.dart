import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/theme/theme.dart';
import 'package:trypto/features/regret/regret_chart.dart';
import 'package:trypto/features/regret/rule_chips.dart';
import 'package:trypto/models/enums.dart';
import 'package:trypto/models/regret.dart';

RegretChart _chart(int days) => RegretChart(
  roundId: 1,
  totalDays: days,
  assetHistory: [
    for (var i = 0; i < days; i++)
      AssetHistoryPoint(
        snapshotDate: DateTime(2026, 7, 1).add(Duration(days: i)),
        actualAsset: 1000000 + i * 1000,
        ruleFollowedAsset: 1100000 + i * 1200,
        btcHoldAsset: 900000 + i * 800,
      ),
  ],
  violationMarkers: [
    if (days > 2)
      ViolationMarker(
        snapshotDate: DateTime(2026, 7, 2),
        assetValue: 1001000,
      ),
  ],
);

/// 자산을 평평하게 두면 Y 범위가 `값 ± 1` 이라 눈금 5개가 모두 같은 축약 문자열이 된다.
/// 축이 어떤 표기를 쓰는지만 보면 되므로 눈금 위치를 맞출 필요가 없다.
RegretChart _flatChart(double value) => RegretChart(
  roundId: 1,
  totalDays: 3,
  assetHistory: [
    for (var i = 0; i < 3; i++)
      AssetHistoryPoint(
        snapshotDate: DateTime(2026, 7, 1).add(Duration(days: i)),
        actualAsset: value,
        ruleFollowedAsset: value,
        btcHoldAsset: value,
      ),
  ],
  violationMarkers: const [],
);

void main() {
  testWidgets('차트가 그려진다', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: buildTryptoTheme(),
        home: Scaffold(
          body: RegretAssetChart(
            chart: _chart(30),
            enabledRules: const {RuleType.chaseBuyBan, RuleType.lossCut},
            btcEnabled: true,
            violations: const [],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byType(RegretAssetChart), findsOneWidget);
  });

  testWidgets('스냅샷이 2개 미만이면 집계 전 안내를 그린다', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: buildTryptoTheme(),
        home: Scaffold(
          body: RegretAssetChart(
            chart: _chart(1),
            enabledRules: const {},
            btcEnabled: false,
            violations: const [],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.textContaining('매일 밤 집계'), findsOneWidget);
  });

  testWidgets('Y축 눈금은 억/만 축약을 공백 없이 찍는다', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: buildTryptoTheme(),
        home: Scaffold(
          body: RegretAssetChart(
            // 1억 123만 — 억과 만 사이에 공백도 자릿점도 없어야 한다.
            chart: _flatChart(101230000.0),
            enabledRules: const {},
            btcEnabled: false,
            violations: const [],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('1억123만'), findsWidgets);
    expect(find.text('1억 123만'), findsNothing);
    expect(find.text('101,230,000'), findsNothing);
  });

  testWidgets('리포트가 비어도 BTC 홀드 칩과 수익률이 남는다', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: buildTryptoTheme(),
        home: Scaffold(
          body: RuleChips(
            impacts: const [],
            enabled: const {},
            btcEnabled: true,
            btcProfitRate: 12.34,
            onToggleRule: (_) {},
            onToggleBtc: () {},
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('설정한 투자 원칙이 없습니다.'), findsOneWidget);
    expect(find.text('BTC만 홀드한 나 +12.34%'), findsOneWidget);
  });

  testWidgets('위반 0건 규칙도 금액과 위반 배지를 그린다', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: buildTryptoTheme(),
        home: Scaffold(
          body: RuleChips(
            impacts: const [
              RuleImpact(
                ruleType: RuleType.overtradingLimit,
                thresholdValue: 5.0,
                violationCount: 0,
                totalLossAmount: 0.0,
              ),
            ],
            enabled: const {RuleType.overtradingLimit},
            btcEnabled: false,
            btcProfitRate: null,
            onToggleRule: (_) {},
            onToggleBtc: () {},
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('과매매 제한'), findsOneWidget);
    expect(find.text('0'), findsWidgets);
  });
}
