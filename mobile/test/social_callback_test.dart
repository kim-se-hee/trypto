import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/features/auth/social_login.dart';
import 'package:trypto/models/enums.dart';

/// 사양서 §2.2.4 의 검증 실패 메시지 표. 문구를 바꾸면 이 테스트가 깨진다.
void main() {
  String verify(String url, {String? expectedState = 'S', String? verifier = 'V'}) =>
      verifySocialCallback(
        Uri.parse(url),
        provider: SocialProvider.kakao,
        expectedState: expectedState,
        codeVerifier: verifier,
      );

  Matcher throwsMessage(String message) => throwsA(
    isA<SocialLoginException>().having((e) => e.message, 'message', message),
  );

  test('정상 콜백은 인가 코드를 돌려준다', () {
    expect(verify('trypto://auth/kakao/callback?code=C&state=S'), 'C');
  });

  test('제공자가 error 를 반환하면 취소·실패 문구', () {
    expect(
      () => verify('trypto://auth/kakao/callback?error=access_denied'),
      throwsMessage('카카오 로그인이 취소되었거나 실패했습니다.'),
    );
  });

  test('제공자 라벨은 제공자별로 다르다', () {
    expect(
      () => verifySocialCallback(
        Uri.parse('trypto://auth/google/callback?error=access_denied'),
        provider: SocialProvider.google,
        expectedState: 'S',
        codeVerifier: 'V',
      ),
      throwsMessage('구글 로그인이 취소되었거나 실패했습니다.'),
    );
  });

  test('code 가 없으면 인가 정보 오류', () {
    expect(
      () => verify('trypto://auth/kakao/callback?state=S'),
      throwsMessage('인가 정보가 올바르지 않습니다.'),
    );
  });

  test('state 가 없으면 인가 정보 오류', () {
    expect(
      () => verify('trypto://auth/kakao/callback?code=C'),
      throwsMessage('인가 정보가 올바르지 않습니다.'),
    );
  });

  test('state 가 불일치하면 보안 검증 실패', () {
    expect(
      () => verify('trypto://auth/kakao/callback?code=C&state=OTHER'),
      throwsMessage('보안 검증(state)에 실패했습니다. 다시 시도해주세요.'),
    );
  });

  test('저장된 state 가 없어도 보안 검증 실패', () {
    expect(
      () => verify(
        'trypto://auth/kakao/callback?code=C&state=S',
        expectedState: null,
      ),
      throwsMessage('보안 검증(state)에 실패했습니다. 다시 시도해주세요.'),
    );
  });

  test('verifier 가 없으면 검증값 부재 문구', () {
    expect(
      () => verify('trypto://auth/kakao/callback?code=C&state=S', verifier: ''),
      throwsMessage('로그인 검증값이 없습니다. 다시 시도해주세요.'),
    );
  });
}
