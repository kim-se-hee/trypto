import 'dart:math' as math;

import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';

import '../../core/format/formatters.dart';
import '../../core/theme/theme.dart';
import '../../core/widgets/numeric_text.dart';
import 'portfolio_summary.dart';

/// §8.1.5 — 미등록 심볼과 "기타" 조각이 같은 회색을 쓴다.
const Color _kFallback = Color(0xFF8B949E);

/// 기준통화 조각 색(`DonutChart.tsx`).
const Color _kCash = Color(0xFFC2B8AB);

const Map<String, Color> _kCoinColors = {
  'BTC': Color(0xFFF7931A),
  'ETH': Color(0xFF627EEA),
  'XRP': Color(0xFF00AAE4),
  'SOL': Color(0xFF9945FF),
  'DOGE': Color(0xFFC2A633),
  'ADA': Color(0xFF0033AD),
  'AVAX': Color(0xFFE84142),
  'DOT': Color(0xFFE6007A),
  'LINK': Color(0xFF2A5ADA),
  'MATIC': Color(0xFF8247E5),
  'ATOM': Color(0xFF2E3148),
  'UNI': Color(0xFFFF007A),
  'AAVE': Color(0xFFB6509E),
  'SAND': Color(0xFF04ADEF),
  'MANA': Color(0xFFFF2D55),
  'BNB': Color(0xFFF3BA2F),
  'ARB': Color(0xFF28A0F0),
  'OP': Color(0xFFFF0420),
  'EOS': Color(0xFF000000),
  'TRX': Color(0xFFEF0027),
  'QTUM': Color(0xFF2E9AD0),
  'JUP': Color(0xFF00D18C),
  'BONK': Color(0xFFF8A100),
  'RAY': Color(0xFF6C5CE7),
  'ORCA': Color(0xFFFFDA44),
  'MNGO': Color(0xFFE4572E),
  'PYTH': Color(0xFF7B61FF),
  'WIF': Color(0xFFC08B5C),
  'RENDER': Color(0xFF1A1A2E),
  'HNT': Color(0xFF474DFF),
  'MSOL': Color(0xFF9945FF),
};

Color coinColor(String symbol) => _kCoinColors[symbol] ?? _kFallback;

const double _kDonutSize = 180;
const double _kRadius = 73;
const double _kStroke = 28;
const double _kStrokeSelected = 34;

/// 코인 조각의 최대 개수. 이보다 많으면 상위 5개 + "기타" 로 접는다(사양서 §5.1.4-3).
const int _kMaxCoinSegments = 6;

class DonutSegment {
  const DonutSegment({
    required this.label,
    required this.value,
    required this.ratio,
    required this.color,
  });

  final String label;
  final double value;

  /// 0~1. 총자산 대비 비중이다.
  final double ratio;

  final Color color;
}

/// 현금 조각이 맨 앞, 이어서 코인 조각을 value 내림차순으로 놓는다(사양서 §5.1.4).
/// 총자산이 0이면 조각이 없다 — 도넛은 배경 원만 그린다.
List<DonutSegment> buildDonutSegments(PortfolioSummary summary) {
  final total = summary.totalAsset.toDouble();
  if (total <= 0) return const [];

  final coins = [
    for (final holding in summary.holdings)
      if (holding.evalAmount > Decimal.zero)
        (
          label: holding.symbol,
          value: holding.evalAmount.toDouble(),
          color: coinColor(holding.symbol),
        ),
  ]..sort((a, b) => b.value.compareTo(a.value));

  final folded = coins.length > _kMaxCoinSegments
      ? [
          ...coins.take(_kMaxCoinSegments - 1),
          (
            label: '기타',
            value: coins
                .skip(_kMaxCoinSegments - 1)
                .fold<double>(0, (sum, coin) => sum + coin.value),
            color: _kFallback,
          ),
        ]
      : coins;

  final cash = summary.cash.toDouble();
  return [
    if (cash > 0)
      DonutSegment(
        label: summary.baseCurrency,
        value: cash,
        ratio: cash / total,
        color: _kCash,
      ),
    for (final coin in folded)
      DonutSegment(
        label: coin.label,
        value: coin.value,
        ratio: coin.value / total,
        color: coin.color,
      ),
  ];
}

/// 자산 구성. 웹의 hover 강조를 **조각/범례 탭** 토글로 옮긴다.
class DonutChart extends StatefulWidget {
  const DonutChart({
    super.key,
    required this.segments,
    required this.totalAsset,
    required this.baseCurrency,
  });

  final List<DonutSegment> segments;
  final double totalAsset;
  final String baseCurrency;

  @override
  State<DonutChart> createState() => _DonutChartState();
}

class _DonutChartState extends State<DonutChart> {
  int? _selected;

  @override
  void didUpdateWidget(DonutChart oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (_selected != null && _selected! >= widget.segments.length) {
      _selected = null;
    }
  }

  void _toggle(int index) =>
      setState(() => _selected = _selected == index ? null : index);

  /// 조각 히트 테스트. 중심에서의 거리가 링 위이고, 12시부터 시계 방향으로 잰 각도가
  /// 조각의 스윕 안에 들어오는 조각을 고른다.
  void _tapDonut(Offset local) {
    const center = Offset(_kDonutSize / 2, _kDonutSize / 2);
    final offset = local - center;
    final distance = offset.distance;
    if (distance < _kRadius - _kStrokeSelected / 2 ||
        distance > _kRadius + _kStrokeSelected / 2) {
      return;
    }

    final angle =
        (math.atan2(offset.dy, offset.dx) + math.pi / 2 + 2 * math.pi) %
        (2 * math.pi);
    var start = 0.0;
    for (var i = 0; i < widget.segments.length; i++) {
      final end = start + widget.segments[i].ratio * 2 * math.pi;
      if (angle >= start && angle < end) {
        _toggle(i);
        return;
      }
      start = end;
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final selected = _selected == null ? null : widget.segments[_selected!];

    return Column(
      children: [
        GestureDetector(
          onTapUp: (details) => _tapDonut(details.localPosition),
          child: SizedBox(
            width: _kDonutSize,
            height: _kDonutSize,
            child: CustomPaint(
              painter: DonutPainter(
                segments: widget.segments,
                selected: _selected,
                track: TryptoPalette.secondary,
              ),
              child: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      selected?.label ?? '총 자산',
                      style: TryptoText.micro.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 2),
                    NumericText(
                      formatCurrency(
                        selected?.value ?? widget.totalAsset,
                        widget.baseCurrency,
                      ),
                      size: 15,
                      weight: FontWeight.w700,
                    ),
                    if (selected != null) ...[
                      const SizedBox(height: 2),
                      NumericText(
                        '${(selected.ratio * 100).toStringAsFixed(1)}%',
                        size: 11,
                        weight: FontWeight.w500,
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ),
        const SizedBox(height: TryptoSpacing.lg),
        for (var i = 0; i < widget.segments.length; i++)
          _LegendRow(
            segment: widget.segments[i],
            selected: _selected == i,
            dimmed: _selected != null && _selected != i,
            onTap: () => _toggle(i),
          ),
      ],
    );
  }
}

class _LegendRow extends StatelessWidget {
  const _LegendRow({
    required this.segment,
    required this.selected,
    required this.dimmed,
    required this.onTap,
  });

  final DonutSegment segment;
  final bool selected;
  final bool dimmed;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    // 알파는 색으로만 준다. Opacity 위젯은 saveLayer 를 만든다(계획서 §4.2.5-8).
    final alpha = dimmed ? 0.4 : 1.0;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(TryptoRadius.md),
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: TryptoSpacing.sm,
          vertical: TryptoSpacing.xs,
        ),
        decoration: BoxDecoration(
          color: selected
              ? theme.colorScheme.primary.withValues(alpha: 0.06)
              : null,
          borderRadius: BorderRadius.circular(TryptoRadius.md),
        ),
        child: Row(
          children: [
            Container(
              width: 8,
              height: 8,
              decoration: BoxDecoration(
                color: segment.color.withValues(alpha: alpha),
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: TryptoSpacing.sm),
            Expanded(
              child: Text(
                segment.label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.labelLarge?.copyWith(
                  color: theme.colorScheme.onSurface.withValues(alpha: alpha),
                ),
              ),
            ),
            NumericText(
              '${(segment.ratio * 100).toStringAsFixed(1)}%',
              size: 12,
              weight: FontWeight.w500,
              color: theme.colorScheme.onSurfaceVariant.withValues(
                alpha: alpha,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// 12시에서 시작해 시계 방향, 끝 모양은 `butt`(사양서 §5.1.4). 선택된 조각만 두께가 굵어진다.
class DonutPainter extends CustomPainter {
  const DonutPainter({
    required this.segments,
    required this.selected,
    required this.track,
  });

  final List<DonutSegment> segments;
  final int? selected;
  final Color track;

  @override
  void paint(Canvas canvas, Size size) {
    final rect = Rect.fromCircle(
      center: size.center(Offset.zero),
      radius: _kRadius,
    );
    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.butt
      ..strokeWidth = _kStroke
      ..color = track;
    canvas.drawArc(rect, 0, 2 * math.pi, false, paint);

    var start = -math.pi / 2;
    for (var i = 0; i < segments.length; i++) {
      final segment = segments[i];
      final sweep = segment.ratio * 2 * math.pi;
      paint
        ..strokeWidth = selected == i ? _kStrokeSelected : _kStroke
        ..color = selected == null || selected == i
            ? segment.color
            : segment.color.withValues(alpha: 0.4);
      canvas.drawArc(rect, start, sweep, false, paint);
      start += sweep;
    }
  }

  @override
  bool shouldRepaint(DonutPainter old) =>
      old.selected != selected ||
      old.track != track ||
      !identical(old.segments, segments);
}
