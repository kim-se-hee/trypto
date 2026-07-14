import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/theme/theme.dart';
import 'features/auth/auth_controller.dart';
import 'features/auth/login_page.dart';
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
      theme: buildTryptoTheme(),
      // 라우터는 6단위에서 붙는다. 그전까지 인증 상태만으로 화면을 가른다.
      home: const _AuthGate(),
    );
  }
}

class _AuthGate extends ConsumerWidget {
  const _AuthGate();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    if (auth.isLoading) return const SplashPage();
    if (!auth.isAuthenticated) return const LoginPage();
    return const _SignedInPage();
  }
}

/// 6단위에서 탭 셸로 교체된다. 로그인·세션 복구·로그아웃을 눈으로 확인하기 위한 임시 화면이다.
class _SignedInPage extends ConsumerWidget {
  const _SignedInPage();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authControllerProvider).user;

    return Scaffold(
      appBar: AppBar(title: const Text('Trypto')),
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('${user?.nickname ?? ''} 님, 로그인되었습니다.'),
            const SizedBox(height: 16),
            OutlinedButton(
              onPressed: () =>
                  ref.read(authControllerProvider.notifier).logout(),
              child: const Text('로그아웃'),
            ),
          ],
        ),
      ),
    );
  }
}
