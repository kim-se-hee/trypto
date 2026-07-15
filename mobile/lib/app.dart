import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/router/router.dart';
import 'core/theme/theme.dart';
import 'features/auth/auth_controller.dart';
import 'features/market/market_controller.dart';
import 'features/mypage/user_repository.dart';
import 'features/portfolio/portfolio_repository.dart';
import 'features/regret/regret_repository.dart';
import 'features/wallet/wallet_assets.dart';

class TryptoApp extends ConsumerWidget {
  const TryptoApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // 로그아웃·세션 만료·회원 탈퇴 시 사용자 경계를 넘는 캐시를 전부 비운다(계획서 §4.1.3).
    // 세션은 SessionStore 가, 라운드는 RoundController 가(인증을 watch 한다), 티커 구독은 마켓
    // 화면이 dispose 에서 끊는다. 남는 것이 아래 조회 캐시들이다 — autoDispose 가 아니라
    // 리스너가 없어도 살아 있으므로 여기서 명시적으로 무효화한다.
    ref.listen(authControllerProvider.select((auth) => auth.isAuthenticated), (
      previous,
      next,
    ) {
      if (previous != true || next) return;
      ref.invalidate(marketCoinsProvider);
      ref.invalidate(userProfileProvider);
      ref.invalidate(portfolioProvider);
      ref.invalidate(walletSnapshotProvider);
      ref.invalidate(regretProvider);
    });

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
