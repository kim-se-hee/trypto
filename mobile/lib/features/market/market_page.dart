import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/widgets/empty_view.dart';
import '../../core/widgets/mypage_button.dart';

class MarketPage extends StatelessWidget {
  const MarketPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('마켓'), actions: const [MypageButton()]),
      body: const EmptyView(
        icon: LucideIcons.chartCandlestick,
        message: '코인 목록은 8단위에서 붙는다.',
      ),
    );
  }
}
