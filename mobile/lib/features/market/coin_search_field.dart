import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/theme/theme.dart';

/// 초성 질의(`ㅂㅌ`)와 자모 질의(`비트`)를 모두 받는다. 조합 중인 글자도 `onChanged` 로
/// 그대로 흘러오며, 자모 분해가 자판 입력 순서를 따르므로 그 상태에서도 맞아떨어진다.
class CoinSearchField extends StatelessWidget {
  const CoinSearchField({
    super.key,
    required this.controller,
    required this.onChanged,
  });

  final TextEditingController controller;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      onChanged: onChanged,
      textInputAction: TextInputAction.search,
      decoration: InputDecoration(
        hintText: '코인명/심볼 검색 (초성 가능)',
        prefixIcon: const Icon(LucideIcons.search, size: 16),
        prefixIconConstraints: const BoxConstraints(minWidth: 40),
        suffixIcon: ValueListenableBuilder(
          valueListenable: controller,
          builder: (context, value, _) => value.text.isEmpty
              ? const SizedBox.shrink()
              : IconButton(
                  icon: const Icon(LucideIcons.x, size: 16),
                  tooltip: '지우기',
                  onPressed: () {
                    controller.clear();
                    onChanged('');
                  },
                ),
        ),
        suffixIconConstraints: const BoxConstraints(minWidth: 40),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: TryptoSpacing.md,
          vertical: TryptoSpacing.md,
        ),
      ),
    );
  }
}
