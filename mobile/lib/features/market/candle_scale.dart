import 'dart:math' as math;

import 'package:flutter/foundation.dart';
import 'package:flutter/painting.dart';

import '../../models/candle.dart';
import 'live_candles.dart';

/// 오른쪽은 y축 가격 라벨 자리다. 웹의 124px 은 960px 캔버스 기준이며 모바일 폭에서는 화면의
/// 1/3 을 먹는다.
const EdgeInsets kChartPadding = EdgeInsets.only(
  left: 20,
  top: 16,
  right: 60,
  bottom: 22,
);

/// 스케일·히트테스트를 위젯 없이 테스트하기 위한 순수 함수 묶음(사양서 §4.3.3).
///
/// 서버 캔들이 아니라 **합성 결과의 표시 구간**에 대해 매번 다시 계산한다 — 실시간 봉의
/// 고가·저가가 바뀌면 y 스케일도 함께 바뀐다.
class CandleScale {
  factory CandleScale.of(Size size, EdgeInsets padding, List<Candle> visible) {
    var minPrice = double.infinity;
    var maxPrice = -double.infinity;
    for (final candle in visible) {
      if (candle.low < minPrice) minPrice = candle.low;
      if (candle.high > maxPrice) maxPrice = candle.high;
    }
    if (visible.isEmpty) {
      minPrice = 0;
      maxPrice = 1;
    }

    // range = (max - min) || (max × 0.02) || 1 — 웹의 falsy 연쇄를 그대로 옮긴다.
    var range = maxPrice - minPrice;
    if (range <= 0) range = maxPrice * 0.02;
    if (range <= 0) range = 1;

    final plotWidth = math.max(1.0, size.width - padding.horizontal);
    final plotHeight = math.max(1.0, size.height - padding.vertical);
    final count = visible.length;
    final slotWidth = count == 0 ? plotWidth : plotWidth / count;

    return CandleScale._(
      padding: padding,
      count: count,
      plotWidth: plotWidth,
      plotHeight: plotHeight,
      slotWidth: slotWidth,
      candleWidth: (slotWidth * 0.62).clamp(6.0, 16.0),
      paddedMin: math.max(0, minPrice - range * 0.08),
      paddedMax: maxPrice + range * 0.08,
    );
  }

  const CandleScale._({
    required this.padding,
    required this.count,
    required this.plotWidth,
    required this.plotHeight,
    required this.slotWidth,
    required this.candleWidth,
    required this.paddedMin,
    required this.paddedMax,
  });

  final EdgeInsets padding;
  final int count;
  final double plotWidth;
  final double plotHeight;
  final double slotWidth;
  final double candleWidth;
  final double paddedMin;
  final double paddedMax;

  double getX(int index) => padding.left + (index + 0.5) * slotWidth;

  double getY(double price) {
    final span = paddedMax - paddedMin;
    if (span <= 0) return padding.top + plotHeight;
    return padding.top + (1 - (price - paddedMin) / span) * plotHeight;
  }

  /// 표시 구간 안의 인덱스. 허용 오차는 `candleWidth` 가 아니라 **`slotWidth`** 다 — 손가락이
  /// 캔들을 가리므로 웹보다 넓혀야 한다(계획서 §5.4).
  int? hitTest(double x) {
    if (count == 0) return null;
    final index = ((x - padding.left) / slotWidth - 0.5).round();
    if (index < 0 || index >= count) return null;
    if ((x - getX(index)).abs() > slotWidth) return null;
    return index;
  }

  /// 드래그 거리를 캔들 개수로 바꾼다.
  int movedCandles(double dx) => (dx / slotWidth).round();
}

/// 표시 구간(사양서 §4.3.8). 최신 봉 추종은 여기서 결정된다.
@immutable
class CandleViewport {
  const CandleViewport({
    required this.visibleCount,
    this.anchorEndIndex = 0,
    this.followingLatest = true,
  });

  final int visibleCount;
  final int anchorEndIndex;

  /// 참이면 오른쪽 끝은 항상 마지막 봉이다. 새 봉이 열리면 화면이 저절로 따라간다.
  final bool followingLatest;

  /// `min` 이 재조정으로 서버 캔들 수가 줄어드는 경우를 막는다.
  int endIndexOf(int total) =>
      followingLatest ? total : math.min(anchorEndIndex, total);

  int countOf(int total) => math.min(visibleCount, total);

  int startIndexOf(int total) =>
      math.max(0, endIndexOf(total) - countOf(total));

  /// 오른쪽 끝까지 되밀면 추종이 자동으로 재개된다.
  CandleViewport withEnd(int end, int total) {
    final count = countOf(total);
    final bounded = end.clamp(count, math.max(count, total)).toInt();
    return CandleViewport(
      visibleCount: visibleCount,
      anchorEndIndex: bounded,
      followingLatest: bounded >= total,
    );
  }

  /// 앵커가 화면에서 차지하던 비율을 유지한 채 확대·축소한다. [scale] > 1 이면 확대(표시
  /// 개수 감소)다. [focusRatio] 는 플롯 폭 안에서의 손가락 위치 비율이다.
  CandleViewport zoom({
    required int total,
    required double scale,
    required double focusRatio,
  }) {
    if (total == 0 || scale <= 0) return this;

    final minVisible = math.min(kMinVisibleCount, total);
    final nextCount = (visibleCount / scale).round().clamp(minVisible, total);
    final start = startIndexOf(total);
    final count = countOf(total);
    final anchor = start + focusRatio * count;

    var end = (anchor - focusRatio * nextCount).round() + nextCount;
    if (end > total) end = total;
    if (end < nextCount) end = nextCount;

    return CandleViewport(
      visibleCount: nextCount,
      anchorEndIndex: end,
      followingLatest: end >= total,
    );
  }
}
