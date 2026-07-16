import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../core/format/formatters.dart';
import '../../core/format/server_time.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/exchange_segment.dart';
import '../../core/widgets/numeric_text.dart';
import '../../models/enums.dart';
import '../../models/regret.dart';
import '../round/round_rules.dart';
import 'regret_simulation.dart';
import 'rule_chips.dart';

final DateFormat _monthDay = DateFormat('M/d', 'en_US');

/// 필터 세그먼트. 라벨에 건수를 함께 싣는다(사양서 §6.3.5).
class ViolationFilterBar extends StatelessWidget {
  const ViolationFilterBar({
    super.key,
    required this.violations,
    required this.filter,
    required this.onChanged,
  });

  final List<ViolationDetail> violations;
  final ViolationFilter filter;
  final ValueChanged<ViolationFilter> onChanged;

  @override
  Widget build(BuildContext context) {
    return ExchangeSegment<ViolationFilter>(
      items: [
        for (final value in ViolationFilter.values)
          SegmentItem(
            value,
            '${value.label} ${filterViolations(violations, value).length}',
          ),
      ],
      value: filter,
      onChanged: onChanged,
    );
  }
}

/// 2행 레이아웃 — 1행: 코인·날짜·손익 / 2행: 규칙 태그 가로 스크롤(사양서 §6.6.2-7).
/// 한 행에 규칙 태그가 여러 개면 가로가 부족하다.
class ViolationTile extends StatelessWidget {
  const ViolationTile({
    super.key,
    required this.violation,
    required this.currency,
  });

  final ViolationDetail violation;
  final String currency;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final profitLoss = violation.profitLoss;

    return Card(
      child: InkWell(
        onTap: () => _showViolationDetail(context, violation, currency),
        child: Padding(
          padding: const EdgeInsets.all(TryptoSpacing.md),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(violation.coinSymbol, style: TryptoText.symbol),
                  const SizedBox(width: TryptoSpacing.sm),
                  Text(
                    _monthDay.format(violation.occurredAt),
                    style: theme.textTheme.labelSmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                  const Spacer(),
                  NumericText(
                    // profitLoss == 0 은 수익으로 분류되지만 부호는 붙이지 않는다.
                    '${profitLoss > 0 ? '+' : ''}'
                    '${formatCurrencyCompact(profitLoss, currency)}',
                    color: context.profitColor(profitLoss),
                  ),
                ],
              ),
              const SizedBox(height: TryptoSpacing.sm),
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: [
                    for (final rule in violation.violatedRules) ...[
                      RuleTag(rule: rule),
                      const SizedBox(width: TryptoSpacing.xs),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class RuleTag extends StatelessWidget {
  const RuleTag({super.key, required this.rule});

  final RuleType rule;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final color = ruleColor(context, rule);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(TryptoRadius.sm),
      ),
      child: Text(
        ruleLabels[rule] ?? '알 수 없는 원칙',
        style: theme.textTheme.labelSmall?.copyWith(color: color),
      ),
    );
  }
}

/// 감정 배지는 구현하지 않는다 — 서버 응답에 대응 필드가 없다(사양서 §6.3.5).
Future<void> _showViolationDetail(
  BuildContext context,
  ViolationDetail violation,
  String currency,
) {
  return showModalBottomSheet<void>(
    context: context,
    useSafeArea: true,
    builder: (context) {
      final theme = Theme.of(context);
      final profitLoss = violation.profitLoss;

      return SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(
            TryptoSpacing.screen,
            0,
            TryptoSpacing.screen,
            TryptoSpacing.screen,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '${violation.coinSymbol} 위반 거래',
                style: theme.textTheme.titleLarge,
              ),
              const SizedBox(height: TryptoSpacing.lg),
              _DetailRow(
                label: '손익',
                value:
                    '${profitLoss > 0 ? '+' : ''}'
                    '${formatCurrencyCompact(profitLoss, currency)}',
                color: context.profitColor(profitLoss),
              ),
              _DetailRow(
                label: '체결 시각',
                value: ServerTime.formatDateTime(violation.occurredAt),
              ),
              _DetailRow(
                label: '주문 번호',
                // orderId 가 없으면 주문이 아니라 모니터링에서 잡힌 위반이다.
                value: violation.orderId?.toString() ?? '모니터링 위반',
              ),
              const SizedBox(height: TryptoSpacing.md),
              Text('위반한 원칙', style: theme.textTheme.titleMedium),
              const SizedBox(height: TryptoSpacing.sm),
              Wrap(
                spacing: TryptoSpacing.xs,
                runSpacing: TryptoSpacing.xs,
                children: [
                  for (final rule in violation.violatedRules)
                    RuleTag(rule: rule),
                ],
              ),
            ],
          ),
        ),
      );
    },
  );
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({required this.label, required this.value, this.color});

  final String label;
  final String value;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: TryptoSpacing.xs),
      child: Row(
        children: [
          Expanded(
            child: Text(
              label,
              style: theme.textTheme.labelLarge?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ),
          NumericText(value, size: 13, color: color),
        ],
      ),
    );
  }
}
