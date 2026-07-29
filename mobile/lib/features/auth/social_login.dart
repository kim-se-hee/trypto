import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';

import '../../core/auth/auth_config.dart';
import '../../core/env.dart';
import '../../models/enums.dart';
import '../../models/user.dart';

/// 인가 단계의 실패. [message] 는 그대로 사용자에게 보여준다(사양서 §2.2.4 의 6종 문구).
class SocialLoginException implements Exception {
  const SocialLoginException(this.message);

  final String message;

  @override
  String toString() => 'SocialLoginException($message)';
}

/// 사용자가 인가를 스스로 그만둔 경우. 실패가 아니므로 사용자 문구를 담지 않는다 — 웹은 인가
/// 팝업이 그냥 닫히면 버튼만 되돌리고 오류를 세우지 않는다(`useSocialLogin` 의 팝업 감시).
/// [SocialLoginException] 을 상속하지 않으므로 `catch (_)` 앞에서 따로 걸러야 한다.
class SocialLoginCanceled implements Exception {
  const SocialLoginCanceled();

  @override
  String toString() => 'SocialLoginCanceled()';
}

/// 옛 브라우저 인가 코드 흐름이 남긴 PKCE 비밀값 청소기(계획서 §4.3.3).
///
/// 두 제공자 모두 공식 SDK 로 전환해 **더는 아무것도 저장하지 않는다.** 다만 이전 버전에서
/// 저장해 둔 값이 기기에 남아 있을 수 있으므로, 부팅 때 무조건 지워 오염값을 닫는다.
class OAuthSecrets {
  OAuthSecrets({FlutterSecureStorage? storage})
    : _storage =
          storage ??
          const FlutterSecureStorage(
            aOptions: AndroidOptions(encryptedSharedPreferences: true),
          );

  static const String _providerKey = 'oauth_provider';
  static const String _verifierKey = 'oauth_code_verifier';
  static const String _stateKey = 'oauth_state';

  final FlutterSecureStorage _storage;

  Future<void> clear() async {
    try {
      await _storage.delete(key: _providerKey);
      await _storage.delete(key: _verifierKey);
      await _storage.delete(key: _stateKey);
    } catch (_) {
      // 지우지 못해도 읽는 경로가 없으므로 인가에 쓰이지 않는다.
    }
  }
}

/// 구글 SDK 토큰 획득부. 테스트에서 SDK 를 대체할 수 있도록 함수로 받는다. null 을 돌려주면 SDK 가
/// 토큰 없이 반환한 것으로, 취소·실패와 같게 다룬다.
typedef GoogleTokenProvider = Future<String?> Function();

/// `initialize` 는 앱 수명 동안 한 번이면 된다. 첫 구글 로그인에서 만들어 두고 재사용한다.
Future<void>? _googleInitialized;

/// 구글 공식 SDK 로 네이티브 로그인해 ID 토큰을 받는다. `serverClientId`(웹 클라이언트 ID)가 ID
/// 토큰의 대상(aud)이 되며, 백엔드가 같은 값으로 검증한다.
Future<String?> _authenticateWithGoogle() async {
  _googleInitialized ??= GoogleSignIn.instance.initialize(
    serverClientId: Env.googleServerClientId,
  );
  await _googleInitialized;
  final account = await GoogleSignIn.instance.authenticate();
  return account.authentication.idToken;
}

/// 구글 SDK 는 취소를 예외 코드로 구분해 알린다. 나머지 코드는 전부 실패로 다룬다.
bool _isGoogleCanceled(Object error) =>
    error is GoogleSignInException &&
    error.code == GoogleSignInExceptionCode.canceled;

/// 카카오 SDK 토큰 획득부. 테스트에서 SDK 를 대체할 수 있도록 함수로 받는다.
typedef KakaoTokenProvider = Future<String> Function();

/// 카카오톡이 깔려 있으면 앱 대 앱으로, 없으면 카카오 계정 웹 로그인으로 토큰을 받는다
/// (에뮬레이터엔 카카오톡이 없어 실제로는 account 경로가 쓰인다).
Future<String> _authenticateWithKakao() async {
  final installed = await isKakaoTalkInstalled();
  final token = installed
      ? await UserApi.instance.loginWithKakaoTalk()
      : await UserApi.instance.loginWithKakaoAccount();
  return token.accessToken;
}

/// 카카오는 취소를 두 갈래로 알린다 — 카카오계정 웹 로그인에서 동의를 거부하면 인가 오류
/// (`access_denied`)로, 카카오톡 앱·커스텀 탭을 그냥 닫으면 네이티브 채널 오류(`CANCELED`)로 온다.
bool _isKakaoCanceled(Object error) =>
    (error is KakaoAuthException &&
        error.error == AuthErrorCause.accessDenied) ||
    (error is PlatformException && error.code == 'CANCELED');

/// 소셜 인가. 제공자로 분기한다 — 둘 다 공식 SDK 로 앱에서 직접 토큰을 받는다. 카카오는 액세스
/// 토큰을, 구글은 ID 토큰을 돌려준다. 구글도 SDK 인 이유는 안드로이드에서 커스텀 스킴 리다이렉트가
/// 폐지돼(앱 사칭 위험) 브라우저 인가 코드 흐름을 못 쓰기 때문이다. 반환값은 곧바로 서버 교환에
/// 넣는 [LoginRequest] 다.
class SocialLoginService {
  SocialLoginService({
    KakaoTokenProvider kakaoTokenProvider = _authenticateWithKakao,
    GoogleTokenProvider googleTokenProvider = _authenticateWithGoogle,
  }) : _kakaoToken = kakaoTokenProvider,
       _googleToken = googleTokenProvider;

  final KakaoTokenProvider _kakaoToken;
  final GoogleTokenProvider _googleToken;

  Future<LoginRequest> authorize(SocialProvider provider) => switch (provider) {
    SocialProvider.kakao => _authorizeKakao(),
    SocialProvider.google => _authorizeGoogle(),
  };

  /// 카카오: SDK 가 앱에서 직접 액세스 토큰을 돌려준다. 취소·실패는 SDK 예외로 오므로
  /// 크래시 대신 다시 시도할 수 있는 사용자 문구로 감싼다. 단 사용자가 스스로 그만둔 것은
  /// 실패가 아니므로 [SocialLoginCanceled] 로 갈라 문구를 세우지 않는다.
  Future<LoginRequest> _authorizeKakao() async {
    const provider = SocialProvider.kakao;
    try {
      final accessToken = await _kakaoToken();
      return LoginRequest.kakao(
        accessToken: accessToken,
        clientType: AuthConfig.clientType,
      );
    } on SocialLoginCanceled {
      rethrow;
    } on SocialLoginException {
      rethrow;
    } catch (error) {
      if (_isKakaoCanceled(error)) throw const SocialLoginCanceled();
      throw SocialLoginException(
        '${AuthConfig.label(provider)} 로그인이 취소되었거나 실패했습니다.',
      );
    }
  }

  /// 구글: 공식 SDK 가 앱에서 직접 ID 토큰을 돌려준다. 취소·중단·실패는 SDK 예외
  /// (`GoogleSignInException`)로 오므로 크래시 대신 다시 시도할 수 있는 사용자 문구로 감싼다.
  /// 그중 사용자가 계정 시트를 닫은 것(`canceled`)만 [SocialLoginCanceled] 로 갈라낸다.
  Future<LoginRequest> _authorizeGoogle() async {
    const provider = SocialProvider.google;
    if (!AuthConfig.isConfigured(provider)) {
      throw SocialLoginException(
        '${AuthConfig.label(provider)} 로그인 설정이 완료되지 않았습니다.',
      );
    }
    try {
      final idToken = await _googleToken();
      if (idToken == null || idToken.isEmpty) {
        throw SocialLoginException(
          '${AuthConfig.label(provider)} 로그인이 취소되었거나 실패했습니다.',
        );
      }
      return LoginRequest.googleToken(
        idToken: idToken,
        clientType: AuthConfig.clientType,
      );
    } on SocialLoginCanceled {
      rethrow;
    } on SocialLoginException {
      rethrow;
    } catch (error) {
      if (_isGoogleCanceled(error)) throw const SocialLoginCanceled();
      throw SocialLoginException(
        '${AuthConfig.label(provider)} 로그인이 취소되었거나 실패했습니다.',
      );
    }
  }
}

final oauthSecretsProvider = Provider<OAuthSecrets>((ref) => OAuthSecrets());

final socialLoginProvider = Provider<SocialLoginService>(
  (ref) => SocialLoginService(),
);
