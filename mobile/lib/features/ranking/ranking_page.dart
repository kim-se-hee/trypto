import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/widgets/empty_view.dart';
import '../../core/widgets/mypage_button.dart';

class RankingPage extends StatelessWidget {
  const RankingPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('랭킹'), actions: const [MypageButton()]),
      body: const EmptyView(
        icon: LucideIcons.trophy,
        message: '랭킹은 15단위에서 붙는다.',
      ),
    );
  }
}
