import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../core/format/formatters.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../models/candle.dart';
import '../../models/enums.dart';
import 'candle_scale.dart';

/// 페인터가 쓰는 색·서체. `BuildContext` 는 페인트 시점에 없다.
@immutable
class CandleChartTheme {
  const CandleChartTheme({
    required this.positive,
    required this.negative,
    required this.grid,
    required this.label,
    required this.crosshair,
    required this.baseCurrency,
  });

  factory CandleChartTheme.of(BuildContext context, String baseCurrency) {
    final colors = context.tryptoColors;
    return CandleChartTheme(
      positive: colors.positive,
      negative: colors.negative,
      grid: TryptoPalette.border,
      label: TryptoPalette.mutedForeground,
      crosshair: TryptoPalette.foreground,
      baseCurrency: baseCurrency,
    );
  }

  final Color positive;
  final Color negative;
  final Color grid;
  final Color label;
  final Color crosshair;
  final String baseCurrency;

  @override
  bool operator ==(Object other) =>
      other is CandleChartTheme &&
      other.positive == positive &&
      other.negative == negative &&
      other.grid == grid &&
      other.label == label &&
      other.crosshair == crosshair &&
      other.baseCurrency == baseCurrency;

  @override
  int get hashCode =>
      Object.hash(positive, negative, grid, label, crosshair, baseCurrency);
}

/// 캔들·격자·축. 크로스헤어는 별개 레이어다 — 손가락이 움직여도 이 레이어는 다시 칠하지
/// 않는다.
class CandlePainter extends CustomPainter {
  const CandlePainter({
    required this.server,
    required this.visible,
    required this.startIndex,
    required this.endIndex,
    required this.liveVisible,
    required this.revision,
    required this.interval,
    required this.theme,
  });

  /// 서버 캔들 배열의 **참조**. 재조회로 교체되면 다시 칠한다.
  final List<Candle> server;
  final List<Candle> visible;
  final int startIndex;
  final int endIndex;

  /// 실시간 값이 반영된 봉이 표시 구간 안에 있는가.
  final bool liveVisible;
  final int revision;
  final CandleInterval interval;
  final CandleChartTheme theme;

  @override
  void paint(Canvas canvas, Size size) {
    if (visible.isEmpty) return;
    final scale = CandleScale.of(size, kChartPadding, visible);

    _paintGrid(canvas, size, scale);
    _paintCandles(canvas, scale);
    _paintTimeAxis(canvas, size, scale);
  }

  void _paintGrid(Canvas canvas, Size size, CandleScale scale) {
    final line = Paint()
      ..color = theme.grid
      ..strokeWidth = 1;
    final step = (scale.paddedMax - scale.paddedMin) / 3;

    for (var i = 0; i < 4; i++) {
      final price = scale.paddedMax - step * i;
      final y = scale.getY(price);
      _dashedLine(
        canvas,
        Offset(kChartPadding.left, y),
        Offset(size.width - kChartPadding.right, y),
        line,
        dash: 4,
        gap: 6,
      );
      final label = _text(
        formatCurrencyCompact(price, theme.baseCurrency),
        theme.label,
      );
      label.paint(
        canvas,
        Offset(size.width - kChartPadding.right + 6, y - label.height / 2),
      );
    }
  }

  void _paintCandles(Canvas canvas, CandleScale scale) {
    final wick = Paint()
      ..strokeWidth = 1.6
      ..strokeCap = StrokeCap.round;
    final body = Paint()..style = PaintingStyle.fill;

    for (var i = 0; i < visible.length; i++) {
      final candle = visible[i];
      final color = candle.close >= candle.open ? theme.positive : theme.negative;
      wick.color = color;
      body.color = color;

      final x = scale.getX(i);
      canvas.drawLine(
        Offset(x, scale.getY(candle.high)),
        Offset(x, scale.getY(candle.low)),
        wick,
      );

      final openY = scale.getY(candle.open);
      final closeY = scale.getY(candle.close);
      final top = math.min(openY, closeY);
      // 시가와 종가가 같아도 한 줄은 보여야 한다.
      final height = math.max(2.0, (closeY - openY).abs());
      canvas.drawRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH(
            x - scale.candleWidth / 2,
            top,
            scale.candleWidth,
            height,
          ),
          const Radius.circular(2),
        ),
        body,
      );
    }
  }

  void _paintTimeAxis(Canvas canvas, Size size, CandleScale scale) {
    final step = math.max(1, visible.length ~/ 4);
    final y = size.height - kChartPadding.bottom + 4;

    for (var i = 0; i < visible.length; i++) {
      if (i % step != 0 && i != visible.length - 1) continue;
      final label = _text(_timeLabel(visible[i].time), theme.label);
      final x = (scale.getX(i) - label.width / 2).clamp(
        0.0,
        size.width - label.width,
      );
      label.paint(canvas, Offset(x, y));
    }
  }

  String _timeLabel(DateTime time) {
    String two(int value) => value.toString().padLeft(2, '0');
    return switch (interval) {
      CandleInterval.minute1 => '${two(time.hour)}:${two(time.minute)}',
      CandleInterval.hour1 ||
      CandleInterval.hour4 => '${time.month}/${time.day} ${time.hour}시',
      CandleInterval.month1 => '${two(time.year % 100)}. ${time.month}',
      CandleInterval.day1 || CandleInterval.week1 => '${time.month}/${time.day}',
    };
  }

  @override
  bool shouldRepaint(CandlePainter old) {
    if (!identical(old.server, server) ||
        old.startIndex != startIndex ||
        old.endIndex != endIndex ||
        old.interval != interval ||
        old.liveVisible != liveVisible ||
        old.theme != theme) {
      return true;
    }
    // 실시간 봉이 표시 구간 밖이면 값이 바뀌어도 그림이 같다. y 스케일도 보이는 구간만으로
    // 정해지므로 결과가 동일하다 — 초당 수백 틱에서 repaint 가 0이 된다.
    if (!liveVisible) return false;
    return old.revision != revision;
  }
}

/// 크로스헤어. 캔들 레이어와 분리되어 있어 틱이 와도(값이 바뀌어도) 손가락이 없으면 아예
/// 칠하지 않는다.
class CrosshairPainter extends CustomPainter {
  const CrosshairPainter({
    required this.visible,
    required this.index,
    required this.revision,
    required this.theme,
  });

  final List<Candle> visible;

  /// 표시 구간 안의 인덱스. null 이면 아무것도 그리지 않는다.
  final int? index;
  final int revision;
  final CandleChartTheme theme;

  @override
  void paint(Canvas canvas, Size size) {
    final i = index;
    if (i == null || i < 0 || i >= visible.length) return;

    final scale = CandleScale.of(size, kChartPadding, visible);
    final candle = visible[i];
    final x = scale.getX(i);
    final y = scale.getY(candle.close);

    final vertical = Paint()
      ..color = theme.crosshair.withValues(alpha: 0.24)
      ..strokeWidth = 1;
    final horizontal = Paint()
      ..color = theme.crosshair.withValues(alpha: 0.16)
      ..strokeWidth = 1;

    _dashedLine(
      canvas,
      Offset(x, kChartPadding.top),
      Offset(x, size.height - kChartPadding.bottom),
      vertical,
      dash: 4,
      gap: 4,
    );
    _dashedLine(
      canvas,
      Offset(kChartPadding.left, y),
      Offset(size.width - kChartPadding.right, y),
      horizontal,
      dash: 4,
      gap: 4,
    );
  }

  @override
  bool shouldRepaint(CrosshairPainter old) {
    if (old.index != index || old.theme != theme) return true;
    if (index == null) return false;
    return old.revision != revision || old.visible.length != visible.length;
  }
}

void _dashedLine(
  Canvas canvas,
  Offset from,
  Offset to,
  Paint paint, {
  required double dash,
  required double gap,
}) {
  final total = (to - from).distance;
  if (total <= 0) return;
  final unit = (to - from) / total;
  var drawn = 0.0;
  while (drawn < total) {
    final end = math.min(drawn + dash, total);
    canvas.drawLine(from + unit * drawn, from + unit * end, paint);
    drawn = end + gap;
  }
}

TextPainter _text(String value, Color color) => TextPainter(
  text: TextSpan(
    text: value,
    style: TryptoText.numeric(size: 10, weight: FontWeight.w500, color: color),
  ),
  textDirection: TextDirection.ltr,
)..layout();
