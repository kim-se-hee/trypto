import 'package:flutter/material.dart';

import 'splash_page.dart';

class TryptoApp extends StatelessWidget {
  const TryptoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Trypto',
      debugShowCheckedModeBanner: false,
      // 다크 테마 토큰이 존재하지 않는다. 라이트 고정.
      themeMode: ThemeMode.light,
      theme: ThemeData(brightness: Brightness.light),
      home: const SplashPage(),
    );
  }
}
