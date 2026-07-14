import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/format/formatters.dart';
import '../../core/format/server_time.dart';
import '../../core/router/guard.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/app_snackbar.dart';
import '../../core/widgets/numeric_text.dart';
import '../../core/widgets/profit_badge.dart';
import '../../models/enums.dart';
import '../auth/auth_controller.dart';
import '../round/round_controller.dart';
import '../round/round_end_sheet.dart';
import '../round/round_rules.dart';
import 'feedback_card.dart';
import 'nickname_sheet.dart';
import 'user_repository.dart';

/// 웹의 2열 그리드를 세로 1열 카드 스택으로 편다 — 프로필 → 현재 라운드 → 피드백 → 로그아웃 →
/// 회원 탈퇴(사양서 §7.6).
class MypagePage extends StatelessWidget {
  const MypagePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('마이페이지')),
      body: ListView(
        padding: const EdgeInsets.all(TryptoSpacing.screen),
        children: const [
          _ProfileCard(),
          SizedBox(height: TryptoSpacing.md),
          _RoundCard(),
          SizedBox(height: TryptoSpacing.md),
          FeedbackCard(),
          SizedBox(height: TryptoSpacing.xl),
          _AccountActions(),
        ],
      ),
    );
  }
}

class _ProfileCard extends ConsumerWidget {
  const _ProfileCard();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final user = ref.watch(authControllerProvider).user;
    final profile = ref.watch(userProfileProvider);

    return Card(
      child: Column(
        children: [
          ListTile(
            contentPadding: const EdgeInsets.symmetric(
              horizontal: TryptoSpacing.lg,
              vertical: TryptoSpacing.sm,
            ),
            leading: Container(
              width: 40,
              height: 40,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: theme.colorScheme.primary.withValues(alpha: 0.12),
                shape: BoxShape.circle,
              ),
              child: Icon(
                LucideIcons.user,
                size: 20,
                color: theme.colorScheme.primary,
              ),
            ),
            title: Text(
              user?.nickname ?? '-',
              style: theme.textTheme.titleMedium,
            ),
            subtitle: Text(
              '회원번호 ${user?.userId ?? '-'}',
              style: theme.textTheme.labelSmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            trailing: const Icon(LucideIcons.pencil, size: 16),
            onTap: user == null
                ? null
                : () async {
                    final changed = await showNicknameSheet(
                      context,
                      current: user.nickname,
                    );
                    if (!changed || !context.mounted) return;
                    showAppSnackbar(context, '닉네임을 변경했습니다.');
                  },
          ),
          const Divider(),
          Padding(
            padding: const EdgeInsets.fromLTRB(
              TryptoSpacing.lg,
              TryptoSpacing.md,
              TryptoSpacing.lg,
              TryptoSpacing.lg,
            ),
            child: Row(
              children: [
                Text(
                  '가입일',
                  style: theme.textTheme.labelLarge?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
                const Spacer(),
                // 조회가 실패해도 화면을 무너뜨리지 않는다. 웹과 같이 `-` 로 떨어진다.
                NumericText(
                  profile.maybeWhen(
                    data: (data) => ServerTime.formatKoreanDate(data.createdAt),
                    orElse: () => '-',
                  ),
                  size: 13,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _RoundCard extends ConsumerWidget {
  const _RoundCard();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final round = ref.watch(
      roundControllerProvider.select((state) => state.activeRound),
    );

    if (round == null) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(TryptoSpacing.lg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('현재 라운드', style: theme.textTheme.titleMedium),
              const SizedBox(height: TryptoSpacing.sm),
              Text(
                '진행 중인 라운드가 없습니다.',
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: TryptoSpacing.md),
              OutlinedButton(
                onPressed: () => context.go(Routes.roundNew),
                child: const Text('새 라운드 시작'),
              ),
            ],
          ),
        ),
      );
    }

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(TryptoSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(
                  '라운드 ${round.roundNumber}',
                  style: theme.textTheme.titleMedium,
                ),
                const SizedBox(width: TryptoSpacing.sm),
                _RoundStatusBadge(status: round.status),
              ],
            ),
            const SizedBox(height: TryptoSpacing.xs),
            Text(
              '시작일 ${ServerTime.formatKoreanDate(round.startedAt)}',
              style: theme.textTheme.labelSmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: TryptoSpacing.lg),
            // 3칸 그리드는 폭이 좁다. 시드머니를 전체 폭으로 빼고 나머지를 2열로 둔다(§7.5.2).
            // 금액은 축약하지 않는다.
            _RoundStat(
              label: '시드머니',
              value: '${formatGrouped(round.initialSeed)}원',
              size: 20,
            ),
            const SizedBox(height: TryptoSpacing.md),
            Row(
              children: [
                Expanded(
                  child: _RoundStat(
                    label: '긴급자금 상한',
                    value: '${formatGrouped(round.emergencyFundingLimit)}원',
                  ),
                ),
                Expanded(
                  child: _RoundStat(
                    label: '남은 충전',
                    value: '${round.emergencyChargeCount}회',
                  ),
                ),
              ],
            ),
            const SizedBox(height: TryptoSpacing.lg),
            Text('투자 원칙', style: theme.textTheme.titleMedium),
            const SizedBox(height: TryptoSpacing.sm),
            if (round.rules.isEmpty)
              Text(
                '설정된 원칙이 없습니다.',
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              )
            else
              // 5종 전부 처리한다 — 과거 라운드가 손절·익절을 가질 수 있다.
              for (final rule in round.rules)
                Padding(
                  padding: const EdgeInsets.symmetric(
                    vertical: TryptoSpacing.xs,
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          ruleLabels[rule.ruleType] ?? '알 수 없는 원칙',
                          style: theme.textTheme.labelLarge,
                        ),
                      ),
                      NumericText(
                        '${_threshold(rule.thresholdValue)}'
                        '${ruleUnits[rule.ruleType] ?? ''}',
                        size: 13,
                      ),
                    ],
                  ),
                ),
            const SizedBox(height: TryptoSpacing.lg),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: () => confirmEndRound(context, ref),
                icon: Icon(
                  LucideIcons.flagOff,
                  size: 16,
                  color: context.tryptoColors.negative,
                ),
                label: Text(
                  '라운드 종료',
                  style: TextStyle(color: context.tryptoColors.negative),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

String _threshold(double value) =>
    value == value.roundToDouble() ? '${value.toInt()}' : '$value';

class _RoundStatusBadge extends StatelessWidget {
  const _RoundStatusBadge({required this.status});

  final RoundStatus status;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.tryptoColors;

    final (String label, Color foreground, Color? background) = switch (status) {
      RoundStatus.active => (
        '진행중',
        theme.colorScheme.primary,
        theme.colorScheme.primary.withValues(alpha: 0.12),
      ),
      RoundStatus.bankrupt => (
        '파산',
        colors.negative,
        colors.negative.withValues(alpha: 0.15),
      ),
      RoundStatus.ended => (
        '종료',
        theme.colorScheme.onSurfaceVariant,
        theme.colorScheme.surfaceContainer,
      ),
      RoundStatus.unknown => (
        '알 수 없음',
        theme.colorScheme.onSurfaceVariant,
        null,
      ),
    };

    return TryptoBadge(
      label: label,
      foreground: foreground,
      background: background,
    );
  }
}

class _RoundStat extends StatelessWidget {
  const _RoundStat({required this.label, required this.value, this.size = 14});

  final String label;
  final String value;
  final double size;

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
        NumericText(value, size: size, weight: FontWeight.w700),
      ],
    );
  }
}

class _AccountActions extends ConsumerWidget {
  const _AccountActions();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final colors = context.tryptoColors;

    return Column(
      children: [
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            // 서버 호출이 실패해도 로컬 상태를 비운다. 인증이 비면 redirect 가 /login 으로 보낸다.
            onPressed: () => ref.read(authControllerProvider.notifier).logout(),
            icon: const Icon(LucideIcons.logOut, size: 16),
            label: const Text('로그아웃'),
          ),
        ),
        const SizedBox(height: TryptoSpacing.xl),
        TextButton(
          onPressed: () => _confirmDeleteAccount(context, ref),
          child: Text(
            '회원 탈퇴',
            style: theme.textTheme.labelMedium?.copyWith(
              color: colors.negative,
              decoration: TextDecoration.underline,
              decorationColor: colors.negative,
            ),
          ),
        ),
        const SizedBox(height: TryptoSpacing.sm),
      ],
    );
  }
}

/// 회원 탈퇴(사양서 R11). 웹에는 호출부가 없지만 **iOS 심사 지침상 필수**다.
///
/// 되돌릴 수 없으므로 확인을 2단계로 둔다: 안내 시트 → 최종 확인 다이얼로그. 성공하면 인증
/// 상태가 비고 redirect 가 `/login` 으로 보낸다.
Future<void> _confirmDeleteAccount(BuildContext context, WidgetRef ref) async {
  final proceed = await showModalBottomSheet<bool>(
    context: context,
    useSafeArea: true,
    builder: (context) => const _DeleteAccountSheet(),
  );
  if (proceed != true || !context.mounted) return;

  final confirmed = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('정말 탈퇴하시겠습니까?'),
      content: const Text('탈퇴 후에는 데이터를 복구할 수 없습니다.'),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(false),
          child: const Text('취소'),
        ),
        FilledButton(
          style: TryptoButtons.destructive,
          onPressed: () => Navigator.of(context).pop(true),
          child: const Text('탈퇴'),
        ),
      ],
    ),
  );
  if (confirmed != true || !context.mounted) return;

  // 성공하면 이 화면은 redirect 로 사라진다. 메신저를 미리 잡아 두어야 완료 안내를 띄울 수 있다.
  final messenger = ScaffoldMessenger.of(context);
  final error = await ref.read(authControllerProvider.notifier).deleteAccount();
  if (error != null) {
    if (!context.mounted) return;
    showAppSnackbar(context, error, isError: true);
    return;
  }
  messenger
    ..hideCurrentSnackBar()
    ..showSnackBar(const SnackBar(content: Text('회원 탈퇴가 완료되었습니다.')));
}

class _DeleteAccountSheet extends StatelessWidget {
  const _DeleteAccountSheet();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.tryptoColors;

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
                Icon(
                  LucideIcons.triangleAlert,
                  size: 18,
                  color: colors.negative,
                ),
                const SizedBox(width: TryptoSpacing.sm),
                Text('회원 탈퇴', style: theme.textTheme.titleLarge),
              ],
            ),
            const SizedBox(height: TryptoSpacing.md),
            for (final line in const [
              '진행 중인 라운드가 종료되고 모든 거래·자산·랭킹 기록이 삭제됩니다.',
              '탈퇴 후 30일 동안은 같은 소셜 계정으로 다시 가입할 수 없습니다.',
              '삭제된 데이터는 복구할 수 없습니다.',
            ])
              Padding(
                padding: const EdgeInsets.symmetric(vertical: TryptoSpacing.xs),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(
                      LucideIcons.dot,
                      size: 16,
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                    Expanded(
                      child: Text(
                        line,
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            const SizedBox(height: TryptoSpacing.xl),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                style: TryptoButtons.destructive,
                onPressed: () => Navigator.of(context).pop(true),
                child: const Text('탈퇴하기'),
              ),
            ),
            const SizedBox(height: TryptoSpacing.sm),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton(
                onPressed: () => Navigator.of(context).pop(false),
                child: const Text('취소'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
