import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/theme/theme.dart';
import 'package:trypto/features/regret/regret_chart.dart';
import 'package:trypto/models/enums.dart';
import 'package:trypto/models/regret.dart';

RegretChart _chart(int days) => RegretChart(
  roundId: 1,
  exchangeId: 1,
  exchangeName: '업비트',
  currency: 'KRW',
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
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.textContaining('매일 밤 집계'), findsOneWidget);
  });
}
