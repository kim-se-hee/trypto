import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/theme/theme.dart';
import '../auth/auth_controller.dart';

/// 프로필·닉네임·피드백·회원 탈퇴는 17단위에서 붙는다. 로그아웃은 라우터 가드를 검증하는
/// 유일한 경로이므로 먼저 만든다 — 인증 상태가 비면 redirect 가 /login 으로 보낸다.
class MypagePage extends ConsumerWidget {
  const MypagePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authControllerProvider).user;
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('마이페이지')),
      body: ListView(
        padding: const EdgeInsets.all(TryptoSpacing.screen),
        children: [
          ListTile(
            leading: const Icon(LucideIcons.circleUser),
            title: Text(user?.nickname ?? '-'),
            subtitle: Text(
              '회원번호 ${user?.userId ?? '-'}',
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ),
          const SizedBox(height: TryptoSpacing.xl),
          OutlinedButton.icon(
            onPressed: () => ref.read(authControllerProvider.notifier).logout(),
            icon: const Icon(LucideIcons.logOut, size: 16),
            label: const Text('로그아웃'),
          ),
        ],
      ),
    );
  }
}
