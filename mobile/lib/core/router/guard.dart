/// 라우트 경로 상수. 가드 판별식이 문자열 비교이므로 오타가 곧 무한 리다이렉트다.
abstract final class Routes {
  static const String splash = '/splash';
  static const String login = '/login';
  static const String roundNew = '/round/new';
  static const String mypage = '/mypage';
  static const String market = '/market';
  static const String portfolio = '/portfolio';
  static const String wallet = '/wallet';
  static const String ranking = '/ranking';
  static const String regret = '/regret';
}

/// 웹의 가드 3종(사양서 §2.7.2)을 하나로 합친 순수 함수. 위젯 없이 표로 테스트한다.
///
/// 두 가지를 반드시 지킨다(계획서 §4.4).
///
/// 1. 로딩 분기는 `loc == X ? null : X` 로 쓴다. `if (loading) return '/splash';` 로 쓰면
///    `/splash` 에 있을 때도 `/splash` 를 반환해 `GoException: redirect loop detected` 가 난다.
/// 2. "인증됨 → `/splash`·`/login` 이면 `/market`" 은 **round 로딩 검사보다 뒤에** 온다. 앞에
///    두면 로그인된 사용자의 콜드 스타트(인증 복구 완료 + 라운드 조회 중)가
///    `/splash → /market → /splash` 를 반복한다.
String? guard({
  required String loc,
  required bool authLoading,
  required bool authed,
  required bool roundLoading,
  required bool hasActive,
  required bool hasEverStarted,
}) {
  if (authLoading) return loc == Routes.splash ? null : Routes.splash;
  if (!authed) return loc == Routes.login ? null : Routes.login;
  if (roundLoading) return loc == Routes.splash ? null : Routes.splash;
  if (loc == Routes.login || loc == Routes.splash) return Routes.market;
  if (loc == Routes.roundNew) return hasActive ? Routes.market : null;
  if (!hasActive && !hasEverStarted) return Routes.roundNew;
  return null;
}
