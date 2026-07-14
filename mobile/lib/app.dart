import 'package:flutter/material.dart';

import 'catalog_page.dart';
import 'core/theme/theme.dart';

class TryptoApp extends StatelessWidget {
  const TryptoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Trypto',
      debugShowCheckedModeBanner: false,
      // 다크 테마 토큰이 존재하지 않는다. 라이트 고정.
      themeMode: ThemeMode.light,
      theme: buildTryptoTheme(),
      // 라우터는 6단위에서 붙는다. 그전까지 카탈로그로 테마를 눈으로 검증한다.
      home: const CatalogPage(),
    );
  }
}
