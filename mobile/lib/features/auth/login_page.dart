import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/auth/auth_config.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/app_snackbar.dart';
import '../../models/enums.dart';
import 'auth_controller.dart';
import 'exchanging_overlay.dart';

class LoginPage extends ConsumerWidget {
  const LoginPage({super.key});

  static const Color _kakaoYellow = Color(0xFFFEE500);
  static const Color _kakaoBrown = Color(0xFF191600);
  static const Color _googleBlue = Color(0xFF4285F4);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    final theme = Theme.of(context);

    // 실패를 조용히 넘기지 않는다(R9). 배너로 남기고 스낵바로도 알린다.
    ref.listen<AuthState>(authControllerProvider, (previous, next) {
      final message = next.errorMessage;
      if (next.status != AuthStatus.failed || message == null) return;
      if (previous?.status == AuthStatus.failed &&
          previous?.errorMessage == message) {
        return;
      }
      showAppSnackbar(context, message, isError: true);
    });

    return Scaffold(
      body: Stack(
        children: [
          SafeArea(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(
                  horizontal: TryptoSpacing.xxl,
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          LucideIcons.activity,
                          size: 30,
                          color: theme.colorScheme.primary,
                        ),
                        const SizedBox(width: TryptoSpacing.sm),
                        Text('Trypto', style: theme.textTheme.headlineMedium),
                      ],
                    ),
                    const SizedBox(height: TryptoSpacing.md),
                    Text(
                      '큰 돈 잃을 걱정 없이 해보는 실전 리허설',
                      textAlign: TextAlign.center,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: TryptoSpacing.xxl * 2),
                    if (auth.errorMessage != null) ...[
                      _ErrorBanner(
                        message: auth.errorMessage!,
                        onDismiss: () => ref
                            .read(authControllerProvider.notifier)
                            .dismissError(),
                      ),
                      const SizedBox(height: TryptoSpacing.lg),
                    ],
                    _SocialButton(
                      provider: SocialProvider.kakao,
                      label: '카카오로 시작하기',
                      background: _kakaoYellow,
                      foreground: _kakaoBrown,
                      icon: const Icon(
                        LucideIcons.messageCircle,
                        size: 18,
                        color: _kakaoBrown,
                      ),
                      busy: auth.isBusy,
                    ),
                    const SizedBox(height: TryptoSpacing.md),
                    _SocialButton(
                      provider: SocialProvider.google,
                      label: '구글로 시작하기',
                      background: theme.colorScheme.surface,
                      foreground: theme.colorScheme.onSurface,
                      border: TryptoPalette.border,
                      icon: const Text(
                        'G',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w700,
                          color: _googleBlue,
                        ),
                      ),
                      busy: auth.isBusy,
                    ),
                  ],
                ),
              ),
            ),
          ),
          if (auth.status == AuthStatus.exchanging && auth.provider != null)
            Positioned.fill(
              child: ExchangingOverlay(provider: auth.provider!),
            ),
        ],
      ),
    );
  }
}

class _SocialButton extends ConsumerWidget {
  const _SocialButton({
    required this.provider,
    required this.label,
    required this.background,
    required this.foreground,
    required this.icon,
    required this.busy,
    this.border,
  });

  final SocialProvider provider;
  final String label;
  final Color background;
  final Color foreground;
  final Color? border;
  final Widget icon;
  final bool busy;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final configured = AuthConfig.isConfigured(provider);
    final enabled = configured && !busy;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SizedBox(
          height: 48,
          child: FilledButton(
            onPressed: enabled
                ? () => ref.read(authControllerProvider.notifier).login(provider)
                : null,
            style: FilledButton.styleFrom(
              backgroundColor: background,
              foregroundColor: foreground,
              disabledBackgroundColor: background.withValues(alpha: 0.5),
              disabledForegroundColor: foreground.withValues(alpha: 0.5),
              elevation: 0,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(TryptoRadius.md),
                side: border == null
                    ? BorderSide.none
                    : BorderSide(color: border!),
              ),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                icon,
                const SizedBox(width: TryptoSpacing.sm),
                Text(
                  label,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
        ),
        // 설정 누락 사유는 개발자만 본다(사양서 §2.4). 릴리스에서는 버튼만 비활성된다.
        if (!configured && kDebugMode) ...[
          const SizedBox(height: TryptoSpacing.xs),
          Text(
            '미설정: ${AuthConfig.missingDefines(provider).join(', ')}',
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: context.tryptoColors.warning,
            ),
          ),
        ],
      ],
    );
  }
}

class _ErrorBanner extends StatelessWidget {
  const _ErrorBanner({required this.message, required this.onDismiss});

  final String message;
  final VoidCallback onDismiss;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final negative = context.tryptoColors.negative;

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: TryptoSpacing.md,
        vertical: TryptoSpacing.md,
      ),
      decoration: BoxDecoration(
        color: negative.withValues(alpha: 0.08),
        border: Border.all(color: negative.withValues(alpha: 0.35)),
        borderRadius: BorderRadius.circular(TryptoRadius.lg),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(LucideIcons.circleAlert, size: 16, color: negative),
          const SizedBox(width: TryptoSpacing.sm),
          Expanded(
            child: Text(
              message,
              style: theme.textTheme.bodySmall?.copyWith(color: negative),
            ),
          ),
          GestureDetector(
            onTap: onDismiss,
            child: Icon(LucideIcons.x, size: 16, color: negative),
          ),
        ],
      ),
    );
  }
}
