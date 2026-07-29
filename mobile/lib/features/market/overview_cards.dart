import 'package:flutter/material.dart';

import '../../core/format/formatters.dart';
import '../../core/realtime/ticker_store.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/numeric_text.dart';
import '../../core/widgets/profit_badge.dart';
import 'market_controller.dart';

/// 카드 전용 가격 표기(웹 `MarketOverviewCards.formatCardPrice`). 목록 행과 달리 USDT 에도
/// `$` 를 붙이고 소수를 정확히 두 자리로 고정한다 — [formatCurrency] 의 USDT 분기와 같다.
/// 그 밖은 원화로 보고 천단위 구분만 넣는다.
String _formatCardPrice(double price, String baseCurrency) {
  if (baseCurrency == 'USDT') return formatCurrency(price, 'USDT');
  return '₩${formatPrice(price, 'KRW')}';
}

/// 주요 코인 카드. 표시 대상은 `BTC/ETH/SOL` 고정이며 목록에 없는 심볼은 건너뛴다
/// (사양서 §4.2.8). 하나도 없으면 영역 자체를 그리지 않는다.
class OverviewCards extends StatelessWidget {
  const OverviewCards({
    super.key,
    required this.coins,
    required this.store,
    required this.baseCurrency,
  });

  final List<CoinEntry> coins;
  final TickerStore store;
  final String baseCurrency;

  static const List<String> _symbols = ['BTC', 'ETH', 'SOL'];

  @override
  Widget build(BuildContext context) {
    final bySymbol = {for (final entry in coins) entry.symbol: entry};
    final featured = [
      for (final symbol in _symbols)
        if (bySymbol[symbol] case final entry?) entry,
    ];
    if (featured.isEmpty) return const SizedBox.shrink();

    return SizedBox(
      height: 84,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: TryptoSpacing.screen),
        itemCount: featured.length,
        separatorBuilder: (context, index) =>
            const SizedBox(width: TryptoSpacing.sm),
        itemBuilder: (context, index) {
          final entry = featured[index];
          return _OverviewCard(
            entry: entry,
            row: store.row(entry.symbol),
            baseCurrency: baseCurrency,
          );
        },
      ),
    );
  }
}

class _OverviewCard extends StatelessWidget {
  const _OverviewCard({
    required this.entry,
    required this.row,
    required this.baseCurrency,
  });

  final CoinEntry entry;
  final RowNotifier? row;
  final String baseCurrency;

  @override
  Widget build(BuildContext context) {
    final notifier = row;

    return Container(
      width: 156,
      padding: const EdgeInsets.all(TryptoSpacing.md),
      decoration: BoxDecoration(
        color: TryptoPalette.card,
        borderRadius: BorderRadius.circular(TryptoRadius.xl),
        border: Border.all(color: TryptoPalette.border),
      ),
      child: notifier == null
          ? _CardBody(
              symbol: entry.symbol,
              name: entry.name,
              price: entry.price,
              changeRate: entry.changeRate,
              baseCurrency: baseCurrency,
            )
          : ValueListenableBuilder<CoinRowState>(
              valueListenable: notifier,
              builder: (context, state, child) => _CardBody(
                symbol: entry.symbol,
                name: entry.name,
                price: state.price,
                changeRate: state.changeRate,
                baseCurrency: baseCurrency,
              ),
            ),
    );
  }
}

class _CardBody extends StatelessWidget {
  const _CardBody({
    required this.symbol,
    required this.name,
    required this.price,
    required this.changeRate,
    required this.baseCurrency,
  });

  final String symbol;
  final String name;
  final double price;
  final double changeRate;
  final String baseCurrency;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    // 시세를 아직 받지 못한 코인이 있다(사양서 §4.2.1). 목록 행과 같이 가격·등락률을 함께
    // `-` 로 둔다 — 등락률 0.00% 로 찍으면 실제로 0 인 코인과 구분되지 않는다.
    final unpriced = price <= 0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                symbol,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TryptoText.symbol,
              ),
            ),
            if (unpriced)
              NumericText(
                '-',
                size: 12,
                weight: FontWeight.w500,
                color: theme.colorScheme.onSurfaceVariant,
              )
            else
              ProfitBadge(value: changeRate, text: formatChangeRate(changeRate)),
          ],
        ),
        Text(
          name,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: theme.textTheme.labelSmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        NumericText(
          unpriced ? '-' : _formatCardPrice(price, baseCurrency),
          size: 15,
          weight: FontWeight.w700,
          color: unpriced
              ? theme.colorScheme.onSurfaceVariant
              : context.profitColor(changeRate),
        ),
      ],
    );
  }
}
