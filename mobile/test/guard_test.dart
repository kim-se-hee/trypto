import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/router/guard.dart';

/// 가드는 위젯 없이 표로 고정한다. 신규 사용자가 라운드 생성을 건너뛰거나, 콜드 스타트가
/// redirect loop 로 터지는 사고가 전부 이 함수 하나에서 난다.
void main() {
  String? redirect(
    String loc, {
    bool authLoading = false,
    bool authed = true,
    bool roundLoading = false,
    bool hasActive = true,
    bool hasEverStarted = true,
  }) => guard(
    loc: loc,
    authLoading: authLoading,
    authed: authed,
    roundLoading: roundLoading,
    hasActive: hasActive,
    hasEverStarted: hasEverStarted,
  );

  group('인증 복구 중', () {
    test('어느 경로에 있든 스플래시로 보낸다', () {
      expect(redirect(Routes.market, authLoading: true), Routes.splash);
      expect(redirect(Routes.login, authLoading: true), Routes.splash);
      expect(redirect(Routes.roundNew, authLoading: true), Routes.splash);
    });

    test('스플래시에서는 다시 스플래시로 보내지 않는다 (redirect loop 방어)', () {
      expect(redirect(Routes.splash, authLoading: true), isNull);
    });
  });

  group('미인증', () {
    test('보호 경로는 로그인으로 보낸다', () {
      expect(redirect(Routes.market, authed: false), Routes.login);
      expect(redirect(Routes.mypage, authed: false), Routes.login);
      expect(redirect(Routes.roundNew, authed: false), Routes.login);
      expect(redirect(Routes.splash, authed: false), Routes.login);
    });

    test('로그인 화면에서는 머문다', () {
      expect(redirect(Routes.login, authed: false), isNull);
    });

    test('라운드 로딩 상태는 미인증 판정을 뒤집지 못한다', () {
      expect(
        redirect(Routes.login, authed: false, roundLoading: true),
        isNull,
      );
    });
  });

  group('라운드 조회 중', () {
    test('스플래시에 머문다', () {
      expect(redirect(Routes.splash, roundLoading: true), isNull);
      expect(redirect(Routes.market, roundLoading: true), Routes.splash);
    });

    test('인증된 콜드 스타트가 /splash → /market → /splash 를 반복하지 않는다', () {
      // 인증 복구 완료 + 라운드 조회 중. "인증됨 → /market" 규칙이 앞에 오면 여기서 루프가 난다.
      expect(redirect(Routes.splash, roundLoading: true), isNull);
    });
  });

  group('인증 완료', () {
    test('로그인·스플래시에 머물지 않고 마켓으로 보낸다', () {
      expect(redirect(Routes.login), Routes.market);
      expect(redirect(Routes.splash), Routes.market);
    });

    test('활성 라운드가 있으면 라운드 생성 화면을 막는다', () {
      expect(redirect(Routes.roundNew), Routes.market);
    });

    test('활성 라운드가 없으면 라운드 생성 화면에 머문다', () {
      expect(
        redirect(Routes.roundNew, hasActive: false, hasEverStarted: false),
        isNull,
      );
      expect(
        redirect(Routes.roundNew, hasActive: false, hasEverStarted: true),
        isNull,
      );
    });

    test('라운드를 한 번도 시작하지 않았으면 생성 화면으로 보낸다', () {
      for (final loc in [
        Routes.market,
        Routes.portfolio,
        Routes.wallet,
        Routes.ranking,
        Routes.regret,
        Routes.mypage,
      ]) {
        expect(
          redirect(loc, hasActive: false, hasEverStarted: false),
          Routes.roundNew,
          reason: loc,
        );
      }
    });

    test('라운드를 끝낸 사용자는 활성 라운드 없이도 앱을 둘러본다', () {
      expect(
        redirect(Routes.market, hasActive: false, hasEverStarted: true),
        isNull,
      );
      expect(
        redirect(Routes.ranking, hasActive: false, hasEverStarted: true),
        isNull,
      );
    });

    test('활성 라운드가 있으면 모든 탭에 머문다', () {
      for (final loc in [
        Routes.market,
        Routes.portfolio,
        Routes.wallet,
        Routes.ranking,
        Routes.regret,
        Routes.mypage,
      ]) {
        expect(redirect(loc), isNull, reason: loc);
      }
    });

    test('탭 하위 경로도 통과한다', () {
      expect(redirect('/market/coin/BTC'), isNull);
      expect(redirect('/wallet/transfer'), isNull);
    });
  });
}
