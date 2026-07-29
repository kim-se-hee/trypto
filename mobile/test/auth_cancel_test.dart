import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:trypto/core/auth/session_store.dart';
import 'package:trypto/features/auth/auth_controller.dart';
import 'package:trypto/features/auth/social_login.dart';
import 'package:trypto/models/enums.dart';

/// 사용자가 제공자 화면에서 스스로 물러난 것은 실패가 아니다. 웹은 인가 팝업이 그냥 닫히면 버튼만
/// 되돌리고 오류를 세우지 않는다(`useSocialLogin`). 앱도 오류 배너·스낵바 없이 `idle` 로 돌아오는지,
/// 반대로 진짜 실패는 `failed` 로 남는지 못 박는다.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late SessionStore store;

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
    store = SessionStore();
  });

  /// 부팅 복구가 끝날 때까지 기다린다. 세션이 없으므로 곧 idle 로 내려앉는다. 이걸 기다리지 않으면
  /// 뒤늦게 끝난 복구가 로그인 결과 상태를 덮는다.
  Future<void> settleBoot(ProviderContainer container) async {
    for (var i = 0; i < 100; i++) {
      final status = container.read(authControllerProvider).status;
      if (status != AuthStatus.restoring) return;
      await Future<void>.delayed(Duration.zero);
    }
  }

  Future<AuthState> loginWith(
    SocialProvider provider,
    SocialLoginService service,
  ) async {
    final container = ProviderContainer(
      overrides: [
        sessionStoreProvider.overrideWithValue(store),
        socialLoginProvider.overrideWithValue(service),
      ],
    );
    addTearDown(container.dispose);

    final controller = container.read(authControllerProvider.notifier);
    await settleBoot(container);
    await controller.login(provider);
    return container.read(authControllerProvider);
  }

  /// SDK 자리에 예외만 돌려주는 가짜를 끼운다 — 취소 판별은 서비스가 하는 일이다.
  SocialLoginService kakaoThrowing(Object error) =>
      SocialLoginService(kakaoTokenProvider: () => Future<String>.error(error));

  SocialLoginService googleThrowing(Object error) => SocialLoginService(
    googleTokenProvider: () => Future<String?>.error(error),
  );

  test('카카오 동의 거부(access_denied)는 오류 없이 로그아웃 상태로 돌아온다', () async {
    final state = await loginWith(
      SocialProvider.kakao,
      kakaoThrowing(
        KakaoAuthException(AuthErrorCause.accessDenied, 'user canceled'),
      ),
    );

    expect(state.status, AuthStatus.idle);
    expect(state.errorMessage, isNull);
  });

  test('카카오톡 로그인 취소(CANCELED)도 오류로 남기지 않는다', () async {
    final state = await loginWith(
      SocialProvider.kakao,
      kakaoThrowing(
        PlatformException(code: 'CANCELED', message: 'User canceled login.'),
      ),
    );

    expect(state.status, AuthStatus.idle);
    expect(state.errorMessage, isNull);
  });

  test('구글 계정 시트를 닫으면 오류로 남기지 않는다', () async {
    final state = await loginWith(
      SocialProvider.google,
      googleThrowing(
        const GoogleSignInException(code: GoogleSignInExceptionCode.canceled),
      ),
    );

    expect(state.status, AuthStatus.idle);
    expect(state.errorMessage, isNull);
  });

  test('취소가 아닌 실패는 실패 문구를 그대로 남긴다', () async {
    final state = await loginWith(
      SocialProvider.kakao,
      kakaoThrowing(
        KakaoAuthException(AuthErrorCause.invalidGrant, 'invalid grant'),
      ),
    );

    expect(state.status, AuthStatus.failed);
    expect(state.errorMessage, '카카오 로그인이 취소되었거나 실패했습니다.');
  });

  test('구글 SDK 오류는 실패 문구를 그대로 남긴다', () async {
    final state = await loginWith(
      SocialProvider.google,
      googleThrowing(
        const GoogleSignInException(
          code: GoogleSignInExceptionCode.unknownError,
        ),
      ),
    );

    expect(state.status, AuthStatus.failed);
    expect(state.errorMessage, '구글 로그인이 취소되었거나 실패했습니다.');
  });
}
