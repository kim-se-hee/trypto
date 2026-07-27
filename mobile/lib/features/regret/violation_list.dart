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

/// 위반 손실은 **양수가 손해**다. 손익 색 규약과 부호가 반대라 뒤집어 넘긴다.
Color _lossColor(BuildContext context, double lossAmount) =>
    context.profitColor(-lossAmount);

/// 부호를 금액 앞에 따로 붙인다. USDT 를 그대로 넘기면 `$-100.00` 이 되어 읽기 나쁘다.
String _formatLoss(double lossAmount, String currency) =>
    '${lossAmount < 0 ? '-' : ''}'
    '${formatCurrencyCompact(lossAmount.abs(), currency)}';

/// 손익 축 필터 세그먼트. 라벨에 건수를 함께 싣는다(사양서 §6.3.5).
/// 건수는 거래소 축을 적용한 뒤 센다 — 두 축은 조합해서 쓰인다.
class ViolationFilterBar extends StatelessWidget {
  const ViolationFilterBar({
    super.key,
    required this.violations,
    required this.filter,
    required this.exchangeId,
    required this.onChanged,
  });

  final List<ViolationDetail> violations;
  final ViolationFilter filter;
  final int? exchangeId;
  final ValueChanged<ViolationFilter> onChanged;

  @override
  Widget build(BuildContext context) {
    return ExchangeSegment<ViolationFilter>(
      items: [
        for (final value in ViolationFilter.values)
          SegmentItem(
            value,
            '${value.label} '
            '${filterViolations(violations, value, exchangeId: exchangeId).length}',
          ),
      ],
      value: filter,
      onChanged: onChanged,
    );
  }
}

/// 거래소 축 필터 세그먼트. 위반이 일어난 거래소만 세우고, 앞에 전 거래소를 붙인다.
class ExchangeFilterBar extends StatelessWidget {
  const ExchangeFilterBar({
    super.key,
    required this.exchanges,
    required this.selected,
    required this.onChanged,
  });

  final List<({int id, String name})> exchanges;
  final int? selected;
  final ValueChanged<int?> onChanged;

  @override
  Widget build(BuildContext context) {
    return ExchangeSegment<int?>(
      items: [
        const SegmentItem<int?>(null, '전 거래소'),
        for (final exchange in exchanges)
          SegmentItem<int?>(exchange.id, exchange.name),
      ],
      value: selected,
      onChanged: onChanged,
    );
  }
}

/// 2행 레이아웃 — 1행: 코인·날짜·거래소·위반 손실 합계 / 2행: 어긴 원칙과 원칙별 금액을 가로
/// 스크롤로 담는다(사양서 §6.6.2-7). 한 행에 원칙이 여러 개면 가로가 부족하다.
///
/// 한 행은 주문 하나다. 금액은 라운드 합계와 달리 **발생 거래소의 기축통화**로 보여준다.
class ViolationTile extends StatelessWidget {
  const ViolationTile({super.key, required this.violation});

  final ViolationDetail violation;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final currency = violation.currency;

    return Card(
      child: InkWell(
        onTap: () => _showViolationDetail(context, violation),
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
                  const SizedBox(width: TryptoSpacing.sm),
                  _ExchangeBadge(name: violation.exchangeName),
                  const Spacer(),
                  NumericText(
                    _formatLoss(violation.totalLossAmount, currency),
                    color: _lossColor(context, violation.totalLossAmount),
                  ),
                ],
              ),
              const SizedBox(height: TryptoSpacing.sm),
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: [
                    for (final rule in violation.violatedRules) ...[
                      RuleTag(
                        rule: rule.ruleType,
                        amount: _formatLoss(rule.lossAmount, currency),
                      ),
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

class _ExchangeBadge extends StatelessWidget {
  const _ExchangeBadge({required this.name});

  final String name;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(TryptoRadius.sm),
      ),
      child: Text(
        name,
        style: theme.textTheme.labelSmall?.copyWith(
          color: theme.colorScheme.onSurfaceVariant,
        ),
      ),
    );
  }
}

class RuleTag extends StatelessWidget {
  const RuleTag({super.key, required this.rule, this.amount});

  final RuleType rule;

  /// 이 원칙 몫의 위반 손실. null 이면 이름만 단다.
  final String? amount;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final color = ruleColor(context, rule);
    final label = ruleLabels[rule] ?? '알 수 없는 원칙';

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(TryptoRadius.sm),
      ),
      child: Text(
        amount == null ? label : '$label $amount',
        style: theme.textTheme.labelSmall?.copyWith(color: color),
      ),
    );
  }
}

/// 감정 배지는 구현하지 않는다 — 서버 응답에 대응 필드가 없다(사양서 §6.3.5).
Future<void> _showViolationDetail(
  BuildContext context,
  ViolationDetail violation,
) {
  return showModalBottomSheet<void>(
    context: context,
    useSafeArea: true,
    builder: (context) {
      final theme = Theme.of(context);
      final currency = violation.currency;

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
                label: '위반 손실',
                value: _formatLoss(violation.totalLossAmount, currency),
                color: _lossColor(context, violation.totalLossAmount),
              ),
              // 그래프는 거래소를 합쳐 그리므로 원화 환산액을 함께 보여준다.
              if (currency != roundCurrency)
                _DetailRow(
                  label: '원화 환산',
                  value: _formatLoss(
                    violation.totalLossAmountKrw,
                    roundCurrency,
                  ),
                ),
              _DetailRow(label: '거래소', value: violation.exchangeName),
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
                    RuleTag(
                      rule: rule.ruleType,
                      amount: _formatLoss(rule.lossAmount, currency),
                    ),
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
