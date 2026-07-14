import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../router/guard.dart';
import 'empty_view.dart';

/// 라운드가 없으면 지갑이 없고, 지갑이 없으면 조회할 것이 없다(사양서 §7.4.2).
/// 포트폴리오·입출금·복기가 같은 안내를 쓴다.
class NoRoundNotice extends StatelessWidget {
  const NoRoundNotice({super.key, required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return EmptyView(
      icon: LucideIcons.flag,
      message: message,
      action: FilledButton(
        onPressed: () => context.go(Routes.roundNew),
        child: const Text('새 라운드 시작'),
      ),
    );
  }
}
