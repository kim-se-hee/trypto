import 'package:flutter/material.dart';

import '../../core/format/formatters.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/numeric_text.dart';
import '../../models/enums.dart';
import '../../models/regret.dart';
import '../round/round_rules.dart';
import 'regret_chart.dart';

/// 규칙별 색(사양서 §6.3.3). 라벨은 [ruleLabels], 단위는 아래 [regretRuleUnits] 가 단일 출처다.
const Map<RuleType, Color> ruleColors = {
  RuleType.lossCut: Color(0xFFED4B9E),
  RuleType.profitTake: Color(0xFF31D0AA),
  RuleType.chaseBuyBan: Color(0xFFFFB237),
  RuleType.averagingDownLimit: Color(0xFFE84142),
  RuleType.overtradingLimit: Color(0xFF1FC7D4),
};

Color ruleColor(BuildContext context, RuleType? rule) =>
    ruleColors[rule] ?? Theme.of(context).colorScheme.onSurfaceVariant;

/// 복기 전용 임계값 단위 표. 라운드 생성 화면의 [ruleUnits] 와 과매매 제한에서 갈린다 —
/// 생성 화면은 하루 한도라는 뜻을 살려 `회/일` 이지만, 복기는 웹과 같이 `회` 로 적는다
/// (웹 `regret-api.ts` 의 `RULE_THRESHOLD_UNIT`).
const Map<RuleType, String> regretRuleUnits = {
  RuleType.lossCut: '%',
  RuleType.profitTake: '%',
  RuleType.chaseBuyBan: '%',
  RuleType.averagingDownLimit: '회',
  RuleType.overtradingLimit: '회',
  RuleType.unknown: '',
};

/// 임계값 표기 — `+10%`, `+3회`. 서버 `thresholdUnit` 을 쓰지 않고 프론트 상수 표로 정한다(§6.3.3).
/// 부호는 단위를 가리지 않고 양수면 붙인다(웹 `MeVsMe.tsx`).
String ruleThresholdLabel(RuleType? rule, double? value) {
  if (value == null || value == 0) return '';
  final unit = regretRuleUnits[rule] ?? '';
  final sign = value > 0 ? '+' : '';
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

  /// 첫 스냅샷 대비 마지막 스냅샷의 등락률. 스냅샷이 모자라면 null 이다.
  final double? btcProfitRate;

  final void Function(RuleType rule) onToggleRule;
  final VoidCallback onToggleBtc;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 규칙이 없어도 벤치마크 칩은 남긴다. BTC 홀드 곡선은 리포트가 아니라 차트 데이터로
        // 계산되므로 배치 전에도 그려지는데, 칩이 사라지면 끌 수단도 값을 읽을 수단도 없다.
        if (impacts.isEmpty) ...[
          Text(
            '설정한 투자 원칙이 없습니다.',
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: TryptoSpacing.sm),
        ],
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: [
              for (final impact in impacts) ...[
                _RuleChip(
                  impact: impact,
                  selected:
                      impact.ruleType != null &&
                      enabled.contains(impact.ruleType),
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
                label: Text(
                  btcProfitRate == null
                      ? 'BTC만 홀드한 나'
                      : 'BTC만 홀드한 나 ${formatProfitPercent(btcProfitRate!)}',
                ),
                labelStyle: TextStyle(
                  fontSize: 12,
                  fontWeight: btcEnabled ? FontWeight.w700 : FontWeight.w600,
                  color: theme.colorScheme.onSurface,
                ),
                side: BorderSide(
                  color: btcEnabled ? btcHoldColor : TryptoPalette.border,
                ),
                selectedColor: btcHoldColor.withValues(alpha: 0.12),
              ),
            ],
          ),
        ),
      ],
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
    final loss = impact.totalLossAmount;

    return GestureDetector(
      onLongPress: onLongPress,
      child: FilterChip(
        selected: selected,
        onSelected: onTap == null ? null : (_) => onTap!(),
        side: BorderSide(color: selected ? color : TryptoPalette.border),
        selectedColor: color.withValues(alpha: 0.12),
        label: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(ruleLabels[rule] ?? '알 수 없는 원칙'),
            // 위반 횟수만으로는 그 원칙이 얼마짜리였는지 알 수 없다. 금액을 함께 단다.
            // 0 도 '지켰다' 는 정보이므로 감추지 않는다.
            if (loss != null) ...[
              const SizedBox(width: TryptoSpacing.xs),
              NumericText(
                formatCurrencyShort(loss, roundCurrency),
                size: 11,
                weight: FontWeight.w600,
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ],
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
              // 이 원칙만 지켰다면 자산에 남았을 금액. 거래소를 합친 원화다.
              if (loss != null)
                _DetailRow(
                  label: '총 위반 손실',
                  value: formatCurrencyShort(loss, roundCurrency),
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
