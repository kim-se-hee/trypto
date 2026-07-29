import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/auth/auth_config.dart';
import 'package:trypto/core/auth/pkce.dart';
import 'package:trypto/models/enums.dart';

void main() {
  group('Pkce', () {
    test('verifier 는 43자 base64url 이다', () {
      final verifier = Pkce.verifier();

      expect(verifier.length, 43);
      expect(verifier, matches(RegExp(r'^[A-Za-z0-9\-_]+$')));
    });

    test('state 는 22자 base64url 이다', () {
      final state = Pkce.state();

      expect(state.length, 22);
      expect(state, matches(RegExp(r'^[A-Za-z0-9\-_]+$')));
    });

    test('challenge 는 RFC 7636 A.2 의 S256 시험값과 일치한다', () {
      const verifier = 'dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk';

      expect(
        Pkce.challenge(verifier),
        'E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM',
      );
    });

    test('verifier 는 호출마다 다르다', () {
      expect(Pkce.verifier(), isNot(Pkce.verifier()));
    });
  });

  group('AuthConfig', () {
    // 두 제공자 모두 공식 SDK 로 전환해 앱에서 직접 토큰을 받는다(social_login.dart). 따라서
    // redirectUri·authorizeUrl 은 현재 로그인 경로에서 호출되지 않으며, 아래 두 시험은 남아 있는
    // 브라우저 인가 코드 흐름 조립 규칙만 고정한다.
    test('구글 redirect URI 는 서버 설정값과 같은 문자열을 만든다', () {
      expect(
        AuthConfig.redirectUri(SocialProvider.google),
        'trypto://auth/google/callback',
      );
    });

    test('구글 인가 URL 은 PKCE 규격대로 조립된다', () {
      final url = AuthConfig.authorizeUrl(
        SocialProvider.google,
        challenge: 'challenge-value',
        state: 'state-value',
      );

      expect(url.origin, 'https://accounts.google.com');
      expect(url.path, '/o/oauth2/v2/auth');
      expect(url.queryParameters['response_type'], 'code');
      expect(url.queryParameters['code_challenge'], 'challenge-value');
      expect(url.queryParameters['code_challenge_method'], 'S256');
      expect(url.queryParameters['state'], 'state-value');
      expect(
        url.queryParameters['redirect_uri'],
        'trypto://auth/google/callback',
      );
      // 구글만 openid scope 를 붙인다.
      expect(url.queryParameters['scope'], 'openid');
    });

    test('카카오는 네이티브 앱 키 기본값만으로 버튼이 살아 있다', () {
      // --dart-define 이 없어도 카카오는 SDK 로 동작하므로 네이티브 앱 키 기본값으로 설정 완료다.
      expect(AuthConfig.isConfigured(SocialProvider.kakao), isTrue);
      expect(AuthConfig.missingDefines(SocialProvider.kakao), isEmpty);
    });

    test('구글도 serverClientId 기본값만으로 버튼이 살아 있다', () {
      // 구글 역시 SDK 로 동작하며, SDK 가 요구하는 값은 serverClientId(웹 클라이언트 ID) 하나다.
      // 클라이언트 ID 는 비밀이 아니어서 Env 에 기본값을 두므로(env.dart), --dart-define 이 없는
      // 테스트 환경에서도 설정 완료다. 안드로이드·iOS 클라이언트 ID 와 콜백 스킴은 브라우저 인가
      // 코드 흐름에만 쓰이며 버튼 활성 판정에서 빠졌다.
      expect(AuthConfig.isConfigured(SocialProvider.google), isTrue);
      expect(AuthConfig.missingDefines(SocialProvider.google), isEmpty);
    });

    test('버튼 활성 판정과 누락 키 목록은 언제나 같은 조건에서 갈린다', () {
      // 로그인 화면은 isConfigured 로 버튼을 잠그고 missingDefines 로 그 사유를 보여준다
      // (login_page.dart). 둘이 어긋나면 사유 없이 잠긴 버튼이나, 잠기지 않았는데 미설정 문구만
      // 뜨는 화면이 나온다. Env 값은 컴파일 타임 상수라 테스트에서 비울 수 없으므로 설정이 빠진
      // 상황을 직접 만드는 대신, 두 판정이 같은 조건을 보고 있는지를 지킨다.
      for (final provider in SocialProvider.values) {
        expect(
          AuthConfig.isConfigured(provider),
          AuthConfig.missingDefines(provider).isEmpty,
          reason: '${provider.wire}: 버튼 활성 판정과 누락 키 목록이 어긋난다',
        );
      }
    });
  });
}
