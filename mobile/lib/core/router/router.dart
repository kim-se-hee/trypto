import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../features/auth/auth_controller.dart';
import '../../features/auth/login_page.dart';
import '../../features/market/chart_fullscreen_page.dart';
import '../../features/market/coin_detail_page.dart';
import '../../features/market/market_page.dart';
import '../../features/mypage/mypage_page.dart';
import '../../features/portfolio/portfolio_page.dart';
import '../../features/ranking/ranking_page.dart';
import '../../features/regret/regret_page.dart';
import '../../features/round/round_controller.dart';
import '../../features/round/round_create_page.dart';
import '../../features/wallet/wallet_page.dart';
import '../../models/enums.dart';
import '../../splash_page.dart';
import 'guard.dart';
import 'refresh.dart';

final _rootNavigatorKey = GlobalKey<NavigatorState>();

/// 라우트 쿼리에서 오는 값이라 무엇이든 들어올 수 있다. 대소문자를 구분한다(`1M` 은 월봉).
CandleInterval _intervalOf(String? wire) => CandleInterval.values.firstWhere(
  (interval) => interval.wire == wire,
  orElse: () => CandleInterval.day1,
);

/// 소셜 콜백 스킴은 라우터에 등록하지 않는다(계획서 §4.3.1). `flutter_web_auth_2` 가 콜백
/// 인텐트를 자기 액티비티에서 소비해 `authenticate()` 의 반환값으로 준다. 라우터에도 스킴을
/// 들이면 같은 인텐트가 두 경로로 들어와 인가 코드가 두 번 교환된다(1회용이라 두 번째는 실패).
final routerProvider = Provider<GoRouter>((ref) {
  final router = GoRouter(
    navigatorKey: _rootNavigatorKey,
    initialLocation: Routes.splash,
    refreshListenable: ref.watch(routerRefreshProvider),
    redirect: (context, state) {
      final auth = ref.read(authControllerProvider);
      final round = ref.read(roundControllerProvider);
      return guard(
        loc: state.matchedLocation,
        authLoading: auth.isLoading,
        authed: auth.isAuthenticated,
        roundLoading: round.isLoading,
        hasActive: round.hasActive,
        hasEverStarted: round.hasEverStarted,
      );
    },
    // 예기치 않은 경로·딥링크 방어. 전역 redirect 가 곧바로 재평가되므로 인증된 사용자는
    // 로그인 화면을 보지 않고 /market 으로 넘어간다.
    onException: (context, state, router) => router.go(Routes.login),
    routes: [
      GoRoute(
        path: Routes.splash,
        builder: (context, state) => const SplashPage(),
      ),
      GoRoute(
        path: Routes.login,
        builder: (context, state) => const LoginPage(),
      ),
      GoRoute(
        path: Routes.roundNew,
        builder: (context, state) => const RoundCreatePage(),
      ),
      GoRoute(
        path: Routes.mypage,
        builder: (context, state) => const MypagePage(),
      ),
      StatefulShellRoute.indexedStack(
        builder: (context, state, shell) => _TabShell(shell: shell),
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: Routes.market,
                builder: (context, state) => const MarketPage(),
                routes: [
                  GoRoute(
                    path: 'coin/:symbol',
                    // 탭바 위를 덮는다. 마켓 브랜치는 선택 상태로 남아 티커 구독이 유지된다.
                    parentNavigatorKey: _rootNavigatorKey,
                    builder: (context, state) => CoinDetailPage(
                      symbol: state.pathParameters['symbol']!,
                    ),
                    routes: [
                      GoRoute(
                        path: 'chart',
                        parentNavigatorKey: _rootNavigatorKey,
                        builder: (context, state) => ChartFullscreenPage(
                          symbol: state.pathParameters['symbol']!,
                          interval: _intervalOf(
                            state.uri.queryParameters['interval'],
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: Routes.portfolio,
                builder: (context, state) => const PortfolioPage(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: Routes.wallet,
                builder: (context, state) => const WalletPage(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: Routes.ranking,
                builder: (context, state) => const RankingPage(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: Routes.regret,
                builder: (context, state) => const RegretPage(),
              ),
            ],
          ),
        ],
      ),
    ],
  );
  ref.onDispose(router.dispose);
  return router;
});

/// 하단 탭 5개. 마이페이지는 탭이 아니라 각 탭 앱바 우상단에서 push 한다 — 송금은 1급 액션이고
/// 마이페이지는 설정이다.
class _TabShell extends StatelessWidget {
  const _TabShell({required this.shell});

  final StatefulNavigationShell shell;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: shell,
      bottomNavigationBar: NavigationBar(
        selectedIndex: shell.currentIndex,
        onDestinationSelected: (index) => shell.goBranch(
          index,
          // 이미 선택된 탭을 다시 누르면 그 탭의 첫 화면으로 돌아간다.
          initialLocation: index == shell.currentIndex,
        ),
        destinations: const [
          NavigationDestination(
            icon: Icon(LucideIcons.chartCandlestick),
            label: '마켓',
          ),
          NavigationDestination(
            icon: Icon(LucideIcons.chartPie),
            label: '포트폴리오',
          ),
          NavigationDestination(
            icon: Icon(LucideIcons.arrowLeftRight),
            label: '입출금',
          ),
          NavigationDestination(icon: Icon(LucideIcons.trophy), label: '랭킹'),
          NavigationDestination(
            icon: Icon(LucideIcons.notebookPen),
            label: '복기',
          ),
        ],
      ),
    );
  }
}
