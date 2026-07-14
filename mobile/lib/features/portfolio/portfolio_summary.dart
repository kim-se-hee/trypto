import 'package:decimal/decimal.dart';

import '../../core/json/decimal_x.dart';
import '../../models/portfolio.dart';

/// 보유 종목 하나의 파생값(사양서 §5.1.5). 서버는 평가금액도 손익도 주지 않는다.
class HoldingView {
  HoldingView(this.holding)
    : evalAmount = holding.currentPrice.dec * holding.quantity.dec,
      buyAmount = holding.avgBuyPrice.dec * holding.quantity.dec;

  final HoldingSnapshot holding;
  final Decimal evalAmount;
  final Decimal buyAmount;

  String get symbol => holding.coinSymbol;

  String get name => holding.coinName;

  double get quantity => holding.quantity;

  double get avgBuyPrice => holding.avgBuyPrice;

  double get currentPrice => holding.currentPrice;

  Decimal get profitLoss => evalAmount - buyAmount;

  /// 퍼센트 값 그 자체다(12.34 = +12.34%). 티커의 `changeRate`(비율)와 단위가 다르다.
  double get profitRate => _percent(profitLoss, buyAmount);
}

/// 자산 요약(사양서 §5.1.3). 계산은 전부 `Decimal` 로 한다 — 8자리 수량 × 원 단위 가격을
/// double 로 누산하면 합계에서 오차가 눈에 보인다(계획서 §4.5.1).
class PortfolioSummary {
  PortfolioSummary._({
    required this.baseCurrency,
    required this.cash,
    required this.totalBuy,
    required this.totalEval,
    required this.holdings,
  });

  factory PortfolioSummary.of(MyHoldings source) {
    final views = [for (final holding in source.holdings) HoldingView(holding)];
    var totalBuy = Decimal.zero;
    var totalEval = Decimal.zero;
    for (final view in views) {
      totalBuy += view.buyAmount;
      totalEval += view.evalAmount;
    }
    return PortfolioSummary._(
      baseCurrency: source.baseCurrencySymbol,
      cash: source.baseCurrencyBalance.dec,
      totalBuy: totalBuy,
      totalEval: totalEval,
      holdings: views,
    );
  }

  final String baseCurrency;

  /// 사용 가능 기준통화 잔고. 보유 코인이 0개여도 이 값은 있다.
  final Decimal cash;

  final Decimal totalBuy;
  final Decimal totalEval;
  final List<HoldingView> holdings;

  Decimal get totalAsset => cash + totalEval;

  Decimal get profitLoss => totalEval - totalBuy;

  double get profitRate => _percent(profitLoss, totalBuy);
}

double _percent(Decimal profitLoss, Decimal base) {
  if (base <= Decimal.zero) return 0;
  return (profitLoss / base).toDouble() * 100;
}

/// 웹은 응답 순서 그대로라 사용자가 볼 기준이 없다. 기본값을 평가금액 내림차순으로 둔다.
enum HoldingSortKey {
  evalAmount('평가금액'),
  profitLoss('평가손익'),
  profitRate('수익률'),
  quantity('보유수량'),
  avgBuyPrice('평균매수가'),
  currentPrice('현재가'),
  name('코인명');

  const HoldingSortKey(this.label);

  final String label;
}

List<HoldingView> sortHoldings(
  List<HoldingView> holdings,
  HoldingSortKey key, {
  bool descending = true,
}) {
  final sign = descending ? -1 : 1;
  return [...holdings]..sort((a, b) => sign * _compare(a, b, key));
}

int _compare(HoldingView a, HoldingView b, HoldingSortKey key) => switch (key) {
  HoldingSortKey.name => a.symbol.compareTo(b.symbol),
  HoldingSortKey.evalAmount => a.evalAmount.compareTo(b.evalAmount),
  HoldingSortKey.profitLoss => a.profitLoss.compareTo(b.profitLoss),
  HoldingSortKey.profitRate => a.profitRate.compareTo(b.profitRate),
  HoldingSortKey.quantity => a.quantity.compareTo(b.quantity),
  HoldingSortKey.avgBuyPrice => a.avgBuyPrice.compareTo(b.avgBuyPrice),
  HoldingSortKey.currentPrice => a.currentPrice.compareTo(b.currentPrice),
};
