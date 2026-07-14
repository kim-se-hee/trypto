import 'package:flutter/material.dart';

import '../theme/trypto_colors.dart';

/// 실패는 조용히 넘기지 않는다 (R9). 서버 `message` 는 이미 한국어 완성문이므로 그대로 띄운다.
void showAppSnackbar(
  BuildContext context,
  String message, {
  bool isError = false,
  SnackBarAction? action,
}) {
  final messenger = ScaffoldMessenger.of(context);
  messenger.hideCurrentSnackBar();
  messenger.showSnackBar(
    SnackBar(
      content: Text(message),
      action: action,
      duration: Duration(seconds: isError ? 4 : 2),
      backgroundColor: isError ? context.tryptoColors.negative : null,
    ),
  );
}
