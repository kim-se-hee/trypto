import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/format/formatters.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/numeric_text.dart';
import '../../models/enums.dart';
import '../../models/regret.dart';
import '../round/round_rules.dart';
import 'regret_chart.dart';

/// 규칙별 색과 아이콘(사양서 §6.3.3). 라벨·단위는 [ruleLabels]·[ruleUnits] 가 단일 출처다.
const Map<RuleType, Color> ruleColors = {
  RuleType.lossCut: Color(0xFFED4B9E),
  RuleType.profitTake: Color(0xFF31D0AA),
  RuleType.chaseBuyBan: Color(0xFFFFB237),
  RuleType.averagingDownLimit: Color(0xFFE84142),
  RuleType.overtradingLimit: Color(0xFF1FC7D4),
};

const Map<RuleType, IconData> ruleIcons = {
  RuleType.lossCut: LucideIcons.trendingDown,
  RuleType.profitTake: LucideIcons.trendingUp,
  RuleType.chaseBuyBan: LucideIcons.ban,
  RuleType.averagingDownLimit: LucideIcons.layers,
  RuleType.overtradingLimit: LucideIcons.timer,
};

Color ruleColor(BuildContext context, RuleType? rule) =>
    ruleColors[rule] ?? Theme.of(context).colorScheme.onSurfaceVariant;

IconData ruleIcon(RuleType? rule) => ruleIcons[rule] ?? LucideIcons.circleDot;

/// 임계값 표기 — `+10%`, `3회`. 서버 `thresholdUnit` 을 쓰지 않고 프론트 상수 표로 정한다(§6.3.3).
String ruleThresholdLabel(RuleType? rule, double? value) {
  if (value == null || value == 0) return '';
  final unit = ruleUnits[rule] ?? '';
  final sign = value > 0 && unit == '%' ? '+' : '';
  final amount = value == value.roundToDouble()
      ? value.toInt().toString()
      : value.toString();
  return '$sign$amount$unit';
}

/// MeVsMe 의 세로 체크박스 리스트를 가로 스크롤 칩 행으로 압축한다(사양서 §6.6.2-4).
///
/// 토글은 화면 로컬 상태이며 서버에 저장되지 않는다. 바뀌는 것은 **차트의 시뮬레이션 라인
/// 하나뿐**이다 — 상단 3-stat 타일은 서버 요약이므로 토글과 무관하다.
class RuleChips extends StatelessWidget {
  const RuleChips({
    super.key,
    required this.impacts,
    required this.enabled,
    required this.btcEnabled,
    required this.btcProfitRate,
    required this.onToggleRule,
    required this.onToggleBtc,
  });

  final List<RuleImpact> impacts;
  final Set<RuleType> enabled;
  final bool btcEnabled;

  /// 웹은 이 값을 `0%` 로 하드코딩했다. 스냅샷이 모자라면 null 이다.
  final double? btcProfitRate;

  final void Function(RuleType rule) onToggleRule;
  final VoidCallback onToggleBtc;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    if (impacts.isEmpty) {
      return Text(
        '설정한 투자 원칙이 없습니다.',
        style: theme.textTheme.bodyMedium?.copyWith(
          color: theme.colorScheme.onSurfaceVariant,
        ),
      );
    }

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (final impact in impacts) ...[
            _RuleChip(
              impact: impact,
              selected:
                  impact.ruleType != null && enabled.contains(impact.ruleType),
              onTap: impact.ruleType == null
                  ? null
                  : () => onToggleRule(impact.ruleType!),
              onLongPress: () => _showRuleDetail(context, impact),
            ),
            const SizedBox(width: TryptoSpacing.sm),
          ],
          FilterChip(
            selected: btcEnabled,
            onSelected: (_) => onToggleBtc(),
            avatar: Icon(LucideIcons.bitcoin, size: 14, color: btcHoldColor),
            label: Text(
              btcProfitRate == null
                  ? 'BTC만 홀드한 나'
                  : 'BTC만 홀드한 나 ${formatProfitPercent(btcProfitRate!)}',
            ),
            side: BorderSide(
              color: btcEnabled ? btcHoldColor : TryptoPalette.border,
            ),
            selectedColor: btcHoldColor.withValues(alpha: 0.12),
          ),
        ],
      ),
    );
  }
}

class _RuleChip extends StatelessWidget {
  const _RuleChip({
    required this.impact,
    required this.selected,
    required this.onTap,
    required this.onLongPress,
  });

  final RuleImpact impact;
  final bool selected;
  final VoidCallback? onTap;
  final VoidCallback onLongPress;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.tryptoColors;
    final rule = impact.ruleType;
    final color = ruleColor(context, rule);

    return GestureDetector(
      onLongPress: onLongPress,
      child: FilterChip(
        selected: selected,
        onSelected: onTap == null ? null : (_) => onTap!(),
        avatar: Icon(ruleIcon(rule), size: 14, color: color),
        side: BorderSide(color: selected ? color : TryptoPalette.border),
        selectedColor: color.withValues(alpha: 0.12),
        label: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(ruleLabels[rule] ?? '알 수 없는 원칙'),
            if (impact.violationCount > 0) ...[
              const SizedBox(width: TryptoSpacing.xs),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                decoration: BoxDecoration(
                  color: colors.negative.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: NumericText(
                  '${impact.violationCount}',
                  size: 10,
                  weight: FontWeight.w700,
                  color: colors.negative,
                ),
              ),
            ],
          ],
        ),
        labelStyle: theme.textTheme.labelMedium,
      ),
    );
  }
}

/// 칩 롱프레스 → 임계값·영향도 상세. 좁은 칩에 다 담을 수 없는 값을 여기서 편다.
Future<void> _showRuleDetail(BuildContext context, RuleImpact impact) {
  return showModalBottomSheet<void>(
    context: context,
    useSafeArea: true,
    builder: (context) {
      final theme = Theme.of(context);
      final colors = context.tryptoColors;
      final rule = impact.ruleType;
      final color = ruleColor(context, rule);
      final threshold = ruleThresholdLabel(rule, impact.thresholdValue);
      final loss = impact.totalLossAmount;
      final gap = impact.impactGap;

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
              Row(
                children: [
                  Container(
                    width: 32,
                    height: 32,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: color.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(TryptoRadius.md),
                    ),
                    child: Icon(ruleIcon(rule), size: 16, color: color),
                  ),
                  const SizedBox(width: TryptoSpacing.md),
                  Expanded(
                    child: Text(
                      ruleLabels[rule] ?? '알 수 없는 원칙',
                      style: theme.textTheme.titleLarge,
                    ),
                  ),
                  if (threshold.isNotEmpty)
                    NumericText(threshold, size: 16, color: color),
                ],
              ),
              const SizedBox(height: TryptoSpacing.lg),
              _DetailRow(
                label: '위반 횟수',
                value: '${impact.violationCount}건',
                color: impact.violationCount > 0 ? colors.negative : null,
              ),
              if (loss != null)
                _DetailRow(label: '누적 손실', value: formatKRWCompact(loss)),
              if (gap != null)
                _DetailRow(
                  label: '영향도',
                  value: formatProfitPercent(gap),
                  color: context.profitColor(gap),
                ),
              const SizedBox(height: TryptoSpacing.sm),
              Text(
                '칩을 눌러 이 원칙을 시뮬레이션에서 켜고 끌 수 있습니다.',
                style: theme.textTheme.labelSmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
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
