import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/features/portfolio/donut_painter.dart';
import 'package:trypto/features/portfolio/portfolio_summary.dart';
import 'package:trypto/models/portfolio.dart';

HoldingSnapshot _holding(
  String symbol, {
  required double quantity,
  required double avgBuyPrice,
  required double currentPrice,
}) => HoldingSnapshot(
  coinId: symbol.hashCode,
  coinSymbol: symbol,
  coinName: symbol,
  quantity: quantity,
  avgBuyPrice: avgBuyPrice,
  currentPrice: currentPrice,
);

MyHoldings _my(List<HoldingSnapshot> holdings, {double cash = 0}) => MyHoldings(
  exchangeId: 1,
  baseCurrencyBalance: cash,
  baseCurrencySymbol: 'KRW',
  holdings: holdings,
);

void main() {
  group('PortfolioSummary', () {
    test('서버가 주지 않는 합계를 holdings 로 계산한다', () {
      final summary = PortfolioSummary.of(
        _my([
          _holding('BTC', quantity: 0.5, avgBuyPrice: 100, currentPrice: 200),
          _holding('ETH', quantity: 2, avgBuyPrice: 50, currentPrice: 25),
        ], cash: 1000),
      );

      expect(summary.totalBuy.toDouble(), 150); // 0.5×100 + 2×50
      expect(summary.totalEval.toDouble(), 150); // 0.5×200 + 2×25
      expect(summary.totalAsset.toDouble(), 1150);
      expect(summary.profitLoss.toDouble(), 0);
      expect(summary.profitRate, 0);
    });

    test('수익률은 퍼센트 값 그 자체다', () {
      final summary = PortfolioSummary.of(
        _my([
          _holding('BTC', quantity: 1, avgBuyPrice: 1000, currentPrice: 1250),
        ]),
      );

      expect(summary.profitLoss.toDouble(), 250);
      expect(summary.profitRate, closeTo(25, 1e-9));
      expect(summary.holdings.single.profitRate, closeTo(25, 1e-9));
    });

    test('매수금액이 0이면 수익률은 0이다 (0으로 나누지 않는다)', () {
      final summary = PortfolioSummary.of(_my([], cash: 500));

      expect(summary.totalBuy.toDouble(), 0);
      expect(summary.profitRate, 0);
      expect(summary.totalAsset.toDouble(), 500);
    });

    test('8자리 수량 × 원 단위 가격을 Decimal 로 누산한다', () {
      final summary = PortfolioSummary.of(
        _my([
          for (var i = 0; i < 3; i++)
            _holding(
              'C$i',
              quantity: 0.1,
              avgBuyPrice: 0.1,
              currentPrice: 0.1,
            ),
        ]),
      );

      // double 로 누산하면 0.030000000000000002 가 된다.
      expect(summary.totalEval.toString(), '0.03');
    });
  });

  group('sortHoldings', () {
    final holdings = [
      HoldingView(
        _holding('AAA', quantity: 1, avgBuyPrice: 10, currentPrice: 30),
      ),
      HoldingView(
        _holding('BBB', quantity: 1, avgBuyPrice: 10, currentPrice: 5),
      ),
      HoldingView(
        _holding('CCC', quantity: 1, avgBuyPrice: 10, currentPrice: 20),
      ),
    ];

    test('기본은 평가금액 내림차순이다', () {
      final sorted = sortHoldings(holdings, HoldingSortKey.evalAmount);

      expect([for (final h in sorted) h.symbol], ['AAA', 'CCC', 'BBB']);
    });

    test('오름차순 토글과 코인명 정렬', () {
      final byRate = sortHoldings(
        holdings,
        HoldingSortKey.profitRate,
        descending: false,
      );
      expect([for (final h in byRate) h.symbol], ['BBB', 'CCC', 'AAA']);

      final byName = sortHoldings(
        holdings,
        HoldingSortKey.name,
        descending: false,
      );
      expect([for (final h in byName) h.symbol], ['AAA', 'BBB', 'CCC']);
    });

    test('원본 목록을 뒤집지 않는다', () {
      sortHoldings(holdings, HoldingSortKey.name);

      expect([for (final h in holdings) h.symbol], ['AAA', 'BBB', 'CCC']);
    });
  });

  group('buildDonutSegments', () {
    test('총자산이 0이면 조각이 없다', () {
      expect(buildDonutSegments(PortfolioSummary.of(_my([]))), isEmpty);
    });

    test('현금만 있으면 현금 한 조각이 100% 다', () {
      final segments = buildDonutSegments(
        PortfolioSummary.of(_my([], cash: 1000)),
      );

      expect(segments, hasLength(1));
      expect(segments.single.label, 'KRW');
      expect(segments.single.ratio, 1);
    });

    test('현금이 맨 앞, 코인은 평가금액 내림차순이다', () {
      final segments = buildDonutSegments(
        PortfolioSummary.of(
          _my([
            _holding('ETH', quantity: 1, avgBuyPrice: 1, currentPrice: 200),
            _holding('BTC', quantity: 1, avgBuyPrice: 1, currentPrice: 700),
          ], cash: 100),
        ),
      );

      expect([for (final s in segments) s.label], ['KRW', 'BTC', 'ETH']);
      expect(segments[1].ratio, closeTo(0.7, 1e-9));
      expect(segments[0].color, const Color(0xFFC2B8AB));
      expect(segments[1].color, const Color(0xFFF7931A)); // BTC 고유색
    });

    test('현금이 0이면 현금 조각을 생략한다', () {
      final segments = buildDonutSegments(
        PortfolioSummary.of(
          _my([
            _holding('BTC', quantity: 1, avgBuyPrice: 1, currentPrice: 100),
          ]),
        ),
      );

      expect([for (final s in segments) s.label], ['BTC']);
    });

    test('평가금액이 0인 코인은 조각이 되지 않는다', () {
      final segments = buildDonutSegments(
        PortfolioSummary.of(
          _my([
            _holding('BTC', quantity: 1, avgBuyPrice: 1, currentPrice: 100),
            _holding('DUST', quantity: 0, avgBuyPrice: 1, currentPrice: 100),
          ]),
        ),
      );

      expect([for (final s in segments) s.label], ['BTC']);
    });

    test('코인이 7개 이상이면 상위 5개 + 기타로 접는다', () {
      final segments = buildDonutSegments(
        PortfolioSummary.of(
          _my([
            for (var i = 1; i <= 8; i++)
              _holding(
                'C$i',
                quantity: 1,
                avgBuyPrice: 1,
                currentPrice: i * 10,
              ),
          ]),
        ),
      );

      expect([for (final s in segments) s.label], [
        'C8',
        'C7',
        'C6',
        'C5',
        'C4',
        '기타',
      ]);
      // 나머지 C3·C2·C1 의 합
      expect(segments.last.value, 60);
      expect(
        segments.fold<double>(0, (sum, s) => sum + s.ratio),
        closeTo(1, 1e-9),
      );
    });

    test('코인이 정확히 6개면 접지 않는다', () {
      final segments = buildDonutSegments(
        PortfolioSummary.of(
          _my([
            for (var i = 1; i <= 6; i++)
              _holding(
                'C$i',
                quantity: 1,
                avgBuyPrice: 1,
                currentPrice: i * 10,
              ),
          ]),
        ),
      );

      expect(segments, hasLength(6));
      expect(segments.map((s) => s.label), isNot(contains('기타')));
    });
  });
}
