import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/widgets/empty_view.dart';
import '../../core/widgets/mypage_button.dart';

class RegretPage extends StatelessWidget {
  const RegretPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('투자 복기'), actions: const [MypageButton()]),
      body: const EmptyView(
        icon: LucideIcons.notebookPen,
        message: '투자 복기는 16단위에서 붙는다.',
      ),
    );
  }
}
