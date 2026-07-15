import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/widgets/empty_view.dart';
import '../../core/widgets/mypage_button.dart';

class PortfolioPage extends StatelessWidget {
  const PortfolioPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('포트폴리오'),
        actions: const [MypageButton()],
      ),
      body: const EmptyView(
        icon: LucideIcons.chartPie,
        message: '보유 자산은 13단위에서 붙는다.',
      ),
    );
  }
}
