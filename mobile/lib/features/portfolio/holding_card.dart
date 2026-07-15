import 'package:flutter/material.dart';

import '../../core/format/formatters.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/numeric_text.dart';
import '../../core/widgets/profit_badge.dart';
import 'portfolio_summary.dart';

/// 웹의 7열 표(§5.1.5)를 카드 한 장으로 접는다. 상단 심볼·수익률, 중단 평가금액·평가손익,
/// 하단 보유수량·평균매수가·현재가.
class HoldingCard extends StatelessWidget {
  const HoldingCard({
    super.key,
    required this.holding,
    required this.baseCurrency,
    required this.onTap,
  });

  final HoldingView holding;
  final String baseCurrency;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final profitLoss = holding.profitLoss.toDouble();
    final sign = profitLoss > 0 ? '+' : '';

    return Card(
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(TryptoSpacing.lg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(holding.symbol, style: TryptoText.symbol),
                  const SizedBox(width: TryptoSpacing.xs),
                  Expanded(
                    child: Text(
                      holding.name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.labelSmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ),
                  ProfitBadge(
                    value: holding.profitRate,
                    text: '${holding.profitRate > 0 ? '+' : ''}'
                        '${holding.profitRate.toStringAsFixed(2)}%',
                  ),
                ],
              ),
              const SizedBox(height: TryptoSpacing.md),
              Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '평가금액',
                          style: theme.textTheme.labelSmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 2),
                        NumericText(
                          formatCurrencyCompact(
                            holding.evalAmount.toDouble(),
                            baseCurrency,
                          ),
                          size: 18,
                          weight: FontWeight.w700,
                        ),
                      ],
                    ),
                  ),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text(
                        '평가손익',
                        style: theme.textTheme.labelSmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 2),
                      NumericText(
                        '$sign${formatCurrencyCompact(profitLoss, baseCurrency)}',
                        size: 14,
                        weight: FontWeight.w600,
                        color: context.profitColor(profitLoss),
                      ),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: TryptoSpacing.md),
              const Divider(),
              const SizedBox(height: TryptoSpacing.sm),
              Row(
                children: [
                  Expanded(
                    child: _MiniCell(
                      label: '보유수량',
                      value: formatQuantity(holding.quantity),
                    ),
                  ),
                  Expanded(
                    child: _MiniCell(
                      label: '평균매수가',
                      value: formatPrice(holding.avgBuyPrice, baseCurrency),
                    ),
                  ),
                  Expanded(
                    child: _MiniCell(
                      label: '현재가',
                      value: formatPrice(holding.currentPrice, baseCurrency),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _MiniCell extends StatelessWidget {
  const _MiniCell({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: theme.textTheme.labelSmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 2),
        NumericText(value, size: 12, weight: FontWeight.w500),
      ],
    );
  }
}
