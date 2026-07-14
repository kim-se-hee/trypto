import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/router/guard.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/app_snackbar.dart';
import 'round_controller.dart';

/// 확인은 **하단 시트 + 파괴적 액션**, 완료 안내는 **다이얼로그**다(사양서 §7.4.3).
///
/// 종료하면 지갑이 사라지므로 포트폴리오·입출금·복기가 전부 빈 상태가 된다. 웹은 실패해도
/// 아무 표시가 없다(R9) — 여기서는 스낵바로 알린다.
Future<void> confirmEndRound(BuildContext context, WidgetRef ref) async {
  final confirmed = await showModalBottomSheet<bool>(
    context: context,
    useSafeArea: true,
    builder: (context) => const _EndRoundSheet(),
  );
  if (confirmed != true || !context.mounted) return;

  final error = await ref.read(roundControllerProvider.notifier).endRound();
  if (!context.mounted) return;
  if (error != null) {
    showAppSnackbar(context, error, isError: true);
    return;
  }

  await showDialog<void>(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('라운드를 종료했습니다'),
      content: const Text('투자 복기에서 이번 라운드를 돌아볼 수 있습니다.\n새 라운드는 언제든 다시 시작할 수 있습니다.'),
      actions: [
        FilledButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('확인'),
        ),
      ],
    ),
  );
  if (!context.mounted) return;
  context.go(Routes.market);
}

class _EndRoundSheet extends StatelessWidget {
  const _EndRoundSheet();

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
                Icon(LucideIcons.triangleAlert, size: 18, color: colors.negative),
                const SizedBox(width: TryptoSpacing.sm),
                Text('라운드를 종료할까요?', style: theme.textTheme.titleLarge),
              ],
            ),
            const SizedBox(height: TryptoSpacing.md),
            Text(
              '종료하면 보유 코인과 잔고가 정산되고 더 이상 거래할 수 없습니다.\n'
              '이번 라운드의 기록은 투자 복기에 남습니다.',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: TryptoSpacing.xl),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                style: TryptoButtons.destructive,
                onPressed: () => Navigator.of(context).pop(true),
                child: const Text('라운드 종료'),
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
