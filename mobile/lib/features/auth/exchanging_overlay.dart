import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/auth/auth_config.dart';
import '../../core/theme/theme.dart';
import '../../models/enums.dart';

/// 인가 코드를 세션으로 교환하는 동안의 전체 차단 오버레이(계획서 §5.1).
/// 웹의 `SocialCallbackPage` 가 하던 시각적 역할을 그대로 대신한다 — 라우트가 아니다.
class ExchangingOverlay extends StatelessWidget {
  const ExchangingOverlay({super.key, required this.provider});

  final SocialProvider provider;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return AbsorbPointer(
      child: ColoredBox(
        color: TryptoPalette.background.withValues(alpha: 0.94),
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                LucideIcons.activity,
                size: 28,
                color: theme.colorScheme.primary,
              ),
              const SizedBox(height: TryptoSpacing.lg),
              const SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(strokeWidth: 2),
              ),
              const SizedBox(height: TryptoSpacing.lg),
              Text(
                '${AuthConfig.label(provider)}로 로그인 중…',
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
