import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/widgets/empty_view.dart';
import '../../core/widgets/mypage_button.dart';

class WalletPage extends StatelessWidget {
  const WalletPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('입출금'), actions: const [MypageButton()]),
      body: const EmptyView(
        icon: LucideIcons.arrowLeftRight,
        message: '지갑과 송금은 14단위에서 붙는다.',
      ),
    );
  }
}
