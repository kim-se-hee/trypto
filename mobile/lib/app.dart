import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/router/router.dart';
import 'core/theme/theme.dart';

class TryptoApp extends ConsumerWidget {
  const TryptoApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return MaterialApp.router(
      title: 'Trypto',
      debugShowCheckedModeBanner: false,
      // 다크 테마 토큰이 존재하지 않는다. 라이트 고정.
      themeMode: ThemeMode.light,
      theme: buildTryptoTheme(),
      routerConfig: ref.watch(routerProvider),
    );
  }
}
