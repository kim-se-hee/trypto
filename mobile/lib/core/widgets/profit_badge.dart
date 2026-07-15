import 'package:flutter/material.dart';

import '../theme/trypto_colors.dart';
import 'numeric_text.dart';

/// 웹 Badge (§8.6.4) — 알약, 좌우 8·상하 2, 12/w500, 아이콘 12 · 간격 4.
class TryptoBadge extends StatelessWidget {
  const TryptoBadge({
    super.key,
    required this.label,
    required this.foreground,
    this.background,
    this.icon,
    this.numeric = false,
  });

  final String label;
  final Color foreground;
  final Color? background;
  final IconData? icon;

  /// 수치 배지(등락률·수익률)는 모노스페이스로 그린다.
  final bool numeric;

  @override
  Widget build(BuildContext context) {
    final text = numeric
        ? NumericText(label, size: 12, weight: FontWeight.w500, color: foreground)
        : Text(
            label,
            style: Theme.of(
              context,
            ).textTheme.labelMedium?.copyWith(color: foreground),
          );

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 12, color: foreground),
            const SizedBox(width: 4),
          ],
          text,
        ],
      ),
    );
  }
}

/// 등락률·수익률 배지. 양수는 `positive/15`, 음수는 `negative/20`, **0 은 배경 없이 보조색**
/// 이다 (§5.1.5, §8.1.3 알파 표).
class ProfitBadge extends StatelessWidget {
  const ProfitBadge({
    super.key,
    required this.value,
    required this.text,
    this.showTrendIcon = false,
  });

  /// 색과 부호 판정에만 쓴다. 표시 문자열은 [text] 가 이미 포맷된 값이다.
  final num value;
  final String text;
  final bool showTrendIcon;

  @override
  Widget build(BuildContext context) {
    final colors = context.tryptoColors;
    final scheme = Theme.of(context).colorScheme;

    final (Color fg, Color? bg) = switch (value) {
      > 0 => (colors.positive, colors.positive.withValues(alpha: 0.15)),
      < 0 => (colors.negative, colors.negative.withValues(alpha: 0.20)),
      _ => (scheme.onSurfaceVariant, null),
    };

    return TryptoBadge(
      label: text,
      foreground: fg,
      background: bg,
      numeric: true,
      icon: showTrendIcon && value != 0
          ? (value > 0 ? Icons.arrow_drop_up : Icons.arrow_drop_down)
          : null,
    );
  }
}

/// 대기·경고 배지 (`--warning` #F0A030). 주문 `PENDING`, 송금 `PENDING` 에 쓴다.
class WarningBadge extends StatelessWidget {
  const WarningBadge({super.key, required this.label, this.icon});

  final String label;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final warning = context.tryptoColors.warning;
    return TryptoBadge(
      label: label,
      foreground: warning,
      background: warning.withValues(alpha: 0.15),
      icon: icon,
    );
  }
}
