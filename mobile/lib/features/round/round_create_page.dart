import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/widgets/empty_view.dart';
import '../auth/auth_controller.dart';

/// 시드·긴급자금·원칙 2단계 입력은 7단위에서 붙는다. 신규 사용자는 가드에 의해 이 화면에
/// 묶이므로, 그동안 빠져나갈 수 있도록 로그아웃만 열어 둔다.
class RoundCreatePage extends ConsumerWidget {
  const RoundCreatePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('라운드 시작'),
        actions: [
          IconButton(
            icon: const Icon(LucideIcons.logOut),
            tooltip: '로그아웃',
            onPressed: () =>
                ref.read(authControllerProvider.notifier).logout(),
          ),
        ],
      ),
      body: const EmptyView(
        icon: LucideIcons.flag,
        message: '라운드 생성은 7단위에서 붙는다.',
      ),
    );
  }
}
