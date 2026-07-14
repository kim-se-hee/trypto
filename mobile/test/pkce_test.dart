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
    test('redirect URI 는 서버 설정값과 같은 문자열을 만든다', () {
      expect(
        AuthConfig.redirectUri(SocialProvider.kakao),
        'trypto://auth/kakao/callback',
      );
      expect(
        AuthConfig.redirectUri(SocialProvider.google),
        'trypto://auth/google/callback',
      );
    });

    test('인가 URL 은 PKCE 규격대로 조립된다', () {
      final url = AuthConfig.authorizeUrl(
        SocialProvider.kakao,
        challenge: 'challenge-value',
        state: 'state-value',
      );

      expect(url.origin, 'https://kauth.kakao.com');
      expect(url.path, '/oauth/authorize');
      expect(url.queryParameters['response_type'], 'code');
      expect(url.queryParameters['code_challenge'], 'challenge-value');
      expect(url.queryParameters['code_challenge_method'], 'S256');
      expect(url.queryParameters['state'], 'state-value');
      expect(
        url.queryParameters['redirect_uri'],
        'trypto://auth/kakao/callback',
      );
      expect(url.queryParameters.containsKey('scope'), isFalse);
    });

    test('구글만 openid scope 를 붙인다', () {
      final url = AuthConfig.authorizeUrl(
        SocialProvider.google,
        challenge: 'c',
        state: 's',
      );

      expect(url.origin, 'https://accounts.google.com');
      expect(url.queryParameters['scope'], 'openid');
    });

    test('clientId 가 비면 버튼을 살리지 않는다', () {
      // --dart-define 이 없는 테스트 환경에서는 두 제공자 모두 미설정이다.
      expect(AuthConfig.isConfigured(SocialProvider.kakao), isFalse);
      expect(
        AuthConfig.missingDefines(SocialProvider.kakao),
        contains('KAKAO_CLIENT_ID'),
      );
    });
  });
}
