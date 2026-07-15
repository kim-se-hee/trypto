import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/features/market/candle_painter.dart';
import 'package:trypto/features/market/candle_scale.dart';
import 'package:trypto/models/candle.dart';
import 'package:trypto/models/enums.dart';

/// 스케일·히트테스트·뷰포트는 위젯 없이 고정한다(계획서 §7-6). `shouldRepaint` 게이트는
/// 10단위 완료 조건 ③⑤의 증거다 — 실시간 봉이 화면 밖이면 repaint 가 0이다.
void main() {
  const padding = EdgeInsets.only(left: 20, top: 20, right: 124, bottom: 42);
  const size = Size(960, 440);

  Candle candle(int minute, double low, double high) => Candle(
    time: DateTime(2026, 7, 15, 10, minute),
    open: low,
    high: high,
    low: low,
    close: high,
  );

  group('CandleScale', () {
    final visible = [candle(0, 100, 200), candle(1, 120, 180)];
    final scale = CandleScale.of(size, padding, visible);

    test('플롯 영역과 슬롯 폭은 패딩을 뺀 값이다', () {
      expect(scale.plotWidth, 816);
      expect(scale.plotHeight, 378);
      expect(scale.slotWidth, 408);
      // clamp(slotWidth × 0.62, 6, 16)
      expect(scale.candleWidth, 16);
    });

    test('가격 범위에 8% 여백을 준다', () {
      // range = 200 - 100 = 100
      expect(scale.paddedMin, 92);
      expect(scale.paddedMax, 208);
    });

    test('저가가 0 이어도 paddedMin 은 음수가 되지 않는다', () {
      final zeroed = CandleScale.of(size, padding, [candle(0, 0, 10)]);
      expect(zeroed.paddedMin, 0);
    });

    test('모든 값이 같으면 range 는 최댓값의 2% 다', () {
      final flat = CandleScale.of(size, padding, [candle(0, 100, 100)]);
      expect(flat.paddedMax - flat.paddedMin, closeTo(2 * 0.08 * 2, 1e-9));
    });

    test('getX 는 슬롯 중앙이고 getY 는 위가 고가다', () {
      expect(scale.getX(0), 20 + 0.5 * 408);
      expect(scale.getX(1), 20 + 1.5 * 408);
      expect(scale.getY(scale.paddedMax), 20);
      expect(scale.getY(scale.paddedMin), 20 + 378);
    });

    test('히트 테스트 허용 오차는 slotWidth 다 — 손가락이 캔들을 가린다', () {
      expect(scale.hitTest(scale.getX(0)), 0);
      expect(scale.hitTest(scale.getX(1)), 1);
      // 캔들 중심에서 candleWidth(16) 를 한참 벗어나도 같은 슬롯이면 잡힌다.
      expect(scale.hitTest(scale.getX(1) - 100), 1);
      expect(scale.hitTest(-500), isNull);
      expect(scale.hitTest(5000), isNull);
    });

    test('드래그 거리를 캔들 개수로 바꾼다', () {
      expect(scale.movedCandles(408 * 2), 2);
      expect(scale.movedCandles(-408), -1);
      expect(scale.movedCandles(10), 0);
    });
  });

  group('CandleViewport', () {
    const total = 100;
    const viewport = CandleViewport(visibleCount: 32);

    test('기본값은 최신 봉 추종이다', () {
      expect(viewport.endIndexOf(total), total);
      expect(viewport.startIndexOf(total), 68);

      // 새 봉이 열려 배열이 길어져도 오른쪽 끝을 따라간다.
      expect(viewport.endIndexOf(total + 1), total + 1);
    });

    test('과거로 패닝하면 추종을 멈추고 그 구간에 머문다', () {
      final panned = viewport.withEnd(80, total);

      expect(panned.followingLatest, isFalse);
      expect(panned.endIndexOf(total), 80);
      // 실시간 봉이 늘어나도 보고 있는 구간은 흔들리지 않는다.
      expect(panned.endIndexOf(total + 4), 80);
    });

    test('오른쪽 끝까지 되밀면 추종이 재개된다', () {
      final panned = viewport.withEnd(80, total);
      final back = panned.withEnd(total, total);

      expect(back.followingLatest, isTrue);
    });

    test('최신 봉보다 오른쪽으로는 넘어가지 못하고 표시 개수보다 왼쪽으로도 못 간다', () {
      expect(viewport.withEnd(500, total).endIndexOf(total), total);
      expect(viewport.withEnd(0, total).endIndexOf(total), 32);
    });

    test('재조정으로 캔들이 줄면 앵커가 배열 밖을 가리키지 않는다', () {
      final panned = viewport.withEnd(90, total);
      expect(panned.endIndexOf(60), 60);
    });

    test('줌은 표시 개수를 [12, 전체] 로 가두고 앵커 비율을 유지한다', () {
      final zoomedIn = viewport.zoom(total: total, scale: 4, focusRatio: 0.5);
      expect(zoomedIn.visibleCount, 12);

      final zoomedOut = viewport.zoom(total: total, scale: 0.1, focusRatio: 0.5);
      expect(zoomedOut.visibleCount, total);

      // 왼쪽 끝을 앵커로 잡고 확대하면 그 봉이 계속 왼쪽 끝에 남는다.
      final anchored = viewport.zoom(total: total, scale: 2, focusRatio: 0);
      expect(anchored.startIndexOf(total), viewport.startIndexOf(total));
      expect(anchored.visibleCount, 16);
    });
  });

  group('CandlePainter.shouldRepaint', () {
    const theme = CandleChartTheme(
      positive: Color(0xFF2ECC87),
      negative: Color(0xFFE85D75),
      grid: Color(0xFFE8E6E1),
      label: Color(0xFF7C7C8A),
      crosshair: Color(0xFF1A1A2E),
      baseCurrency: 'KRW',
    );

    final server = [candle(0, 100, 200), candle(1, 120, 180)];

    CandlePainter painter({
      required bool liveVisible,
      required int revision,
      int endIndex = 2,
      List<Candle>? candles,
    }) => CandlePainter(
      server: candles ?? server,
      visible: candles ?? server,
      startIndex: 0,
      endIndex: endIndex,
      liveVisible: liveVisible,
      revision: revision,
      interval: CandleInterval.day1,
      theme: theme,
    );

    test('실시간 봉이 화면 밖이면 값이 바뀌어도 다시 칠하지 않는다', () {
      final before = painter(liveVisible: false, revision: 7);
      final after = painter(liveVisible: false, revision: 8);

      expect(after.shouldRepaint(before), isFalse);
    });

    test('실시간 봉이 보이면 revision 이 오를 때만 다시 칠한다', () {
      final before = painter(liveVisible: true, revision: 7);

      expect(painter(liveVisible: true, revision: 7).shouldRepaint(before), isFalse);
      expect(painter(liveVisible: true, revision: 8).shouldRepaint(before), isTrue);
    });

    test('서버 캔들 교체와 뷰포트 이동은 항상 다시 칠한다', () {
      final before = painter(liveVisible: false, revision: 7);

      expect(
        painter(
          liveVisible: false,
          revision: 7,
          candles: [candle(0, 100, 200), candle(1, 120, 180)],
        ).shouldRepaint(before),
        isTrue,
      );
      expect(
        painter(liveVisible: false, revision: 7, endIndex: 1)
            .shouldRepaint(before),
        isTrue,
      );
    });

    test('크로스헤어 레이어는 손가락이 없으면 칠하지 않는다', () {
      const idle = CrosshairPainter(
        visible: [],
        index: null,
        revision: 1,
        theme: theme,
      );
      const ticked = CrosshairPainter(
        visible: [],
        index: null,
        revision: 2,
        theme: theme,
      );

      expect(ticked.shouldRepaint(idle), isFalse);
    });
  });
}
