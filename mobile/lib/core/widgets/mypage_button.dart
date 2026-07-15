import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../router/guard.dart';

/// 마이페이지 진입점. 탭이 아니라 각 탭 앱바 우상단에서 루트 내비게이터로 push 한다.
class MypageButton extends StatelessWidget {
  const MypageButton({super.key});

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: const Icon(LucideIcons.circleUser),
      tooltip: '마이페이지',
      onPressed: () => context.push(Routes.mypage),
    );
  }
}
