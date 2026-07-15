import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../core/format/formatters.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/numeric_text.dart';
import '../../models/enums.dart';
import '../../models/regret.dart';
import 'regret_simulation.dart';

/// BTC 홀드 벤치마크 색(사양서 §6.4.3). 팔레트에 없는 브랜드 색이라 여기서만 쓴다.
const Color btcHoldColor = Color(0xFFF7931A);

const int _kMinSnapshots = 2;

final DateFormat _monthDay = DateFormat('M/d', 'en_US');
final DateFormat _yearMonth = DateFormat('yyyy.MM', 'en_US');
final DateFormat _fullDate = DateFormat('yyyy-MM-dd', 'en_US');

/// 자산 곡선 3선 + 위반 마커. 모델 `RegretChart` 와 이름이 겹치지 않게 `RegretAssetChart` 다.
///
/// 마우스 호버가 없으므로 드래그 크로스헤어로 대체하고, 값은 차트 위 툴팁이 아니라 **차트 아래
/// 고정 패널**에 내린다(사양서 §6.6.2-2).
class RegretAssetChart extends StatefulWidget {
  const RegretAssetChart({
    super.key,
    required this.chart,
    required this.enabledRules,
    required this.btcEnabled,
  });

  final RegretChart chart;
  final Set<RuleType> enabledRules;
  final bool btcEnabled;

  @override
  State<RegretAssetChart> createState() => _RegretAssetChartState();
}

class _RegretAssetChartState extends State<RegretAssetChart> {
  int? _touchedIndex;

  @override
  Widget build(BuildContext context) {
    final history = widget.chart.assetHistory;
    if (history.length < _kMinSnapshots) {
      return const _EmptyChart();
    }

    final theme = Theme.of(context);
    final colors = context.tryptoColors;
    final currency = widget.chart.currency;

    final actual = [for (final point in history) point.actualAsset];
    final simulation = simulationLine(history, widget.enabledRules);
    final btcHold = [for (final point in history) point.btcHoldAsset];
    final showSimulation = widget.enabledRules.isNotEmpty;

    final range = chartYRange([
      actual,
      if (showSimulation) simulation,
      if (widget.btcEnabled) btcHold,
    ]);

    // 마커는 문자열이 아니라 날짜로 매칭한다(사양서 §6.4.3).
    final markerIndexes = _markerIndexes(history, widget.chart.violationMarkers);
    final interval = labelTickInterval(widget.chart.totalDays);
    final index = _touchedIndex;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          height: 220,
          child: LineChart(
            LineChartData(
              minX: 0,
              maxX: (history.length - 1).toDouble(),
              minY: range.min,
              maxY: range.max,
              clipData: const FlClipData.all(),
              gridData: FlGridData(
                drawVerticalLine: false,
                horizontalInterval: (range.max - range.min) / 4,
                getDrawingHorizontalLine: (value) =>
                    const FlLine(color: TryptoPalette.border, strokeWidth: 1),
              ),
              borderData: FlBorderData(show: false),
              titlesData: FlTitlesData(
                topTitles: const AxisTitles(),
                rightTitles: const AxisTitles(),
                leftTitles: AxisTitles(
                  sideTitles: SideTitles(
                    showTitles: true,
                    reservedSize: 52,
                    interval: (range.max - range.min) / 4,
                    getTitlesWidget: (value, meta) => Padding(
                      padding: const EdgeInsets.only(right: TryptoSpacing.xs),
                      child: NumericText(
                        formatCurrencyCompact(value, currency),
                        size: 10,
                        weight: FontWeight.w500,
                        color: theme.colorScheme.onSurfaceVariant,
                        textAlign: TextAlign.right,
                      ),
                    ),
                  ),
                ),
                bottomTitles: AxisTitles(
                  sideTitles: SideTitles(
                    showTitles: true,
                    reservedSize: 24,
                    interval: 1,
                    getTitlesWidget: (value, meta) {
                      final i = value.round();
                      if (i < 0 || i >= history.length) {
                        return const SizedBox.shrink();
                      }
                      // 마지막 날짜는 직전 라벨과 충분히 떨어졌을 때만 덧붙인다(§6.4.2).
                      final last = history.length - 1;
                      final isTick = i % interval == 0;
                      final isTail =
                          i == last && last % interval > interval / 2;
                      if (!isTick && !isTail) return const SizedBox.shrink();
                      return Text(
                        _axisLabel(
                          history[i].snapshotDate,
                          widget.chart.totalDays,
                        ),
                        style: theme.textTheme.labelSmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      );
                    },
                  ),
                ),
              ),
              lineTouchData: LineTouchData(
                touchSpotThreshold: 24,
                touchCallback: (event, response) {
                  final spot = response?.lineBarSpots?.firstOrNull;
                  if (spot == null) return;
                  if (spot.spotIndex == _touchedIndex) return;
                  setState(() => _touchedIndex = spot.spotIndex);
                },
                // 툴팁 상자를 그리지 않는다. 값은 아래 패널이 보여주고, 여기서는 세로 지시선과
                // 강조 점만 남긴다.
                touchTooltipData: LineTouchTooltipData(
                  getTooltipItems: (spots) => [for (final _ in spots) null],
                ),
                getTouchedSpotIndicator: (barData, indexes) => [
                  for (final _ in indexes)
                    TouchedSpotIndicatorData(
                      const FlLine(
                        color: TryptoPalette.mutedForeground,
                        strokeWidth: 1,
                        dashArray: [4, 3],
                      ),
                      FlDotData(
                        getDotPainter: (spot, percent, bar, index) =>
                            FlDotCirclePainter(
                              radius: 4,
                              color: bar.color ?? theme.colorScheme.primary,
                              strokeColor: TryptoPalette.card,
                              strokeWidth: 1.5,
                            ),
                      ),
                    ),
                ],
              ),
              lineBarsData: [
                _line(actual, theme.colorScheme.primary, 2.2),
                if (showSimulation)
                  _line(
                    simulation,
                    colors.negative,
                    1.8,
                    dashArray: const [6, 4],
                  ),
                if (widget.btcEnabled)
                  _line(
                    btcHold,
                    btcHoldColor.withValues(alpha: 0.5),
                    1.5,
                  ),
                _markers(actual, markerIndexes, colors.negative),
              ],
            ),
          ),
        ),
        const SizedBox(height: TryptoSpacing.md),
        _ValuePanel(
          date: history[index ?? history.length - 1].snapshotDate,
          actual: actual[index ?? actual.length - 1],
          simulation: showSimulation
              ? simulation[index ?? simulation.length - 1]
              : null,
          btcHold: widget.btcEnabled
              ? btcHold[index ?? btcHold.length - 1]
              : null,
          violated: markerIndexes.contains(index ?? history.length - 1),
          currency: currency,
          touched: index != null,
        ),
        const SizedBox(height: TryptoSpacing.sm),
        _Legend(
          showSimulation: showSimulation,
          showBtcHold: widget.btcEnabled,
        ),
      ],
    );
  }

  LineChartBarData _line(
    List<double> values,
    Color color,
    double width, {
    List<int>? dashArray,
  }) {
    return LineChartBarData(
      spots: [
        for (var i = 0; i < values.length; i++)
          FlSpot(i.toDouble(), values[i]),
      ],
      color: color,
      barWidth: width,
      dashArray: dashArray,
      isCurved: false,
      dotData: const FlDotData(show: false),
    );
  }

  /// 위반 마커는 선을 그리지 않는 별도 시리즈다 — 점만 남긴다(사양서 §6.4.3).
  LineChartBarData _markers(
    List<double> actual,
    Set<int> indexes,
    Color color,
  ) {
    return LineChartBarData(
      spots: [
        for (var i = 0; i < actual.length; i++)
          FlSpot(i.toDouble(), actual[i]),
      ],
      color: Colors.transparent,
      barWidth: 0,
      dotData: FlDotData(
        checkToShowDot: (spot, bar) => indexes.contains(spot.x.round()),
        getDotPainter: (spot, percent, bar, index) => FlDotCirclePainter(
          radius: 3,
          color: color,
          strokeColor: TryptoPalette.card,
          strokeWidth: 1.5,
        ),
      ),
    );
  }
}

Set<int> _markerIndexes(
  List<AssetHistoryPoint> history,
  List<ViolationMarker> markers,
) {
  final byDate = <DateTime, int>{
    for (var i = 0; i < history.length; i++) history[i].snapshotDate: i,
  };
  return {
    for (final marker in markers)
      if (byDate[marker.snapshotDate] != null) byDate[marker.snapshotDate]!,
  };
}

String _axisLabel(DateTime date, int totalDays) =>
    totalDays <= 180 ? _monthDay.format(date) : _yearMonth.format(date);

/// 차트 아래 고정 값 패널. 손가락이 차트를 가려도 값이 보인다.
class _ValuePanel extends StatelessWidget {
  const _ValuePanel({
    required this.date,
    required this.actual,
    required this.simulation,
    required this.btcHold,
    required this.violated,
    required this.currency,
    required this.touched,
  });

  final DateTime date;
  final double actual;
  final double? simulation;
  final double? btcHold;
  final bool violated;
  final String currency;
  final bool touched;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.tryptoColors;
    final simulation = this.simulation;
    final btcHold = this.btcHold;

    return Container(
      padding: const EdgeInsets.all(TryptoSpacing.md),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainer,
        borderRadius: BorderRadius.circular(TryptoRadius.md),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                _fullDate.format(date),
                style: theme.textTheme.labelMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
              const Spacer(),
              if (violated)
                Text(
                  '규칙 위반 (손실)',
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: colors.negative,
                  ),
                )
              else if (!touched)
                Text(
                  '차트를 드래그해 날짜별 값을 봅니다',
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
            ],
          ),
          const SizedBox(height: TryptoSpacing.sm),
          Row(
            children: [
              Expanded(
                child: _ValueCell(
                  label: '실제',
                  value: formatCurrencyCompact(actual, currency),
                  color: theme.colorScheme.primary,
                ),
              ),
              if (simulation != null)
                Expanded(
                  child: _ValueCell(
                    label: '규칙 준수',
                    value: formatCurrencyCompact(simulation, currency),
                    color: colors.negative,
                  ),
                ),
              if (btcHold != null)
                Expanded(
                  child: _ValueCell(
                    label: 'BTC 홀드',
                    value: formatCurrencyCompact(btcHold, currency),
                    color: btcHoldColor,
                  ),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ValueCell extends StatelessWidget {
  const _ValueCell({
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Container(
              width: 8,
              height: 2,
              decoration: BoxDecoration(
                color: color,
                borderRadius: BorderRadius.circular(TryptoRadius.xs),
              ),
            ),
            const SizedBox(width: TryptoSpacing.xs),
            Text(
              label,
              style: theme.textTheme.labelSmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
        const SizedBox(height: 2),
        NumericText(value, size: 13),
      ],
    );
  }
}

class _Legend extends StatelessWidget {
  const _Legend({required this.showSimulation, required this.showBtcHold});

  final bool showSimulation;
  final bool showBtcHold;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.tryptoColors;

    return Wrap(
      spacing: TryptoSpacing.md,
      runSpacing: TryptoSpacing.xs,
      children: [
        _LegendItem(color: theme.colorScheme.primary, label: '실제'),
        if (showSimulation)
          _LegendItem(color: colors.negative, label: '규칙 준수 시뮬레이션', dashed: true),
        if (showBtcHold) const _LegendItem(color: btcHoldColor, label: 'BTC 홀드'),
        _LegendItem(color: colors.negative, label: '위반 지점', dot: true),
      ],
    );
  }
}

class _LegendItem extends StatelessWidget {
  const _LegendItem({
    required this.color,
    required this.label,
    this.dashed = false,
    this.dot = false,
  });

  final Color color;
  final String label;
  final bool dashed;
  final bool dot;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (dot)
          Container(
            width: 6,
            height: 6,
            decoration: BoxDecoration(color: color, shape: BoxShape.circle),
          )
        else
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              for (var i = 0; i < (dashed ? 2 : 1); i++) ...[
                if (i > 0) const SizedBox(width: 2),
                Container(width: dashed ? 5 : 12, height: 2, color: color),
              ],
            ],
          ),
        const SizedBox(width: TryptoSpacing.xs),
        Text(
          label,
          style: theme.textTheme.labelSmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}

/// 스냅샷이 2개 미만이면 선을 그릴 수 없다. **스켈레톤을 쓰지 않는다** — 빈 상태를 로딩으로
/// 오인시킨다(사양서 §6.6.2-8).
class _EmptyChart extends StatelessWidget {
  const _EmptyChart();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return SizedBox(
      height: 220,
      child: CustomPaint(
        painter: const _DashedGridPainter(),
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(TryptoSpacing.lg),
            child: Text(
              '복기 리포트는 매일 밤 집계됩니다.\n내일 다시 확인해 주세요.',
              textAlign: TextAlign.center,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _DashedGridPainter extends CustomPainter {
  const _DashedGridPainter();

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = TryptoPalette.border
      ..strokeWidth = 1;

    for (var line = 0; line < 5; line++) {
      final y = size.height * line / 4;
      for (var x = 0.0; x < size.width; x += 8) {
        canvas.drawLine(
          Offset(x, y),
          Offset((x + 4).clamp(0, size.width), y),
          paint,
        );
      }
    }
  }

  @override
  bool shouldRepaint(_DashedGridPainter oldDelegate) => false;
}
