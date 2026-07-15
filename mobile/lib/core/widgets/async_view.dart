import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../api/api_exception.dart';
import 'empty_view.dart';

/// `AsyncValue` → 로딩 / 오류+재시도 / 데이터. 실패를 조용히 넘기지 않는다(R9).
class AsyncView<T> extends StatelessWidget {
  const AsyncView({
    super.key,
    required this.value,
    required this.builder,
    this.onRetry,
  });

  final AsyncValue<T> value;
  final Widget Function(T data) builder;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return value.when(
      data: builder,
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (error, _) => EmptyView(
        icon: LucideIcons.circleAlert,
        message: error.asApiException.userMessage,
        action: onRetry == null
            ? null
            : OutlinedButton(onPressed: onRetry, child: const Text('다시 시도')),
      ),
    );
  }
}
