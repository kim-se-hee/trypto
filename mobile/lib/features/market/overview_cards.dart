import 'package:flutter/material.dart';

import '../../core/format/formatters.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/numeric_text.dart';
import '../../core/widgets/profit_badge.dart';
import 'market_controller.dart';

/// 주요 코인 카드. 표시 대상은 `BTC/ETH/SOL` 고정이며 목록에 없는 심볼은 건너뛴다
/// (사양서 §4.2.8). 하나도 없으면 영역 자체를 그리지 않는다.
class OverviewCards extends StatelessWidget {
  const OverviewCards({
    super.key,
    required this.coins,
    required this.baseCurrency,
  });

  final List<CoinEntry> coins;
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
        itemBuilder: (context, index) =>
            _OverviewCard(entry: featured[index], baseCurrency: baseCurrency),
      ),
    );
  }
}

class _OverviewCard extends StatelessWidget {
  const _OverviewCard({required this.entry, required this.baseCurrency});

  final CoinEntry entry;
  final String baseCurrency;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      width: 156,
      padding: const EdgeInsets.all(TryptoSpacing.md),
      decoration: BoxDecoration(
        color: TryptoPalette.card,
        borderRadius: BorderRadius.circular(TryptoRadius.xl),
        border: Border.all(color: TryptoPalette.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  entry.symbol,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TryptoText.symbol,
                ),
              ),
              ProfitBadge(
                value: entry.changeRate,
                text: formatChangeRate(entry.changeRate),
              ),
            ],
          ),
          Text(
            entry.name,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: theme.textTheme.labelSmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          NumericText(
            entry.price <= 0
                ? '-'
                : '${getCurrencySymbol(baseCurrency)}'
                      '${formatPrice(entry.price, baseCurrency)}',
            size: 15,
            weight: FontWeight.w700,
            color: context.profitColor(entry.changeRate),
          ),
        ],
      ),
    );
  }
}
