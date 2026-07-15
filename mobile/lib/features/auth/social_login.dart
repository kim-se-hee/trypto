import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_web_auth_2/flutter_web_auth_2.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';

import '../../core/auth/auth_config.dart';
import '../../core/auth/pkce.dart';
import '../../models/enums.dart';
import '../../models/user.dart';

/// 인가 단계의 실패. [message] 는 그대로 사용자에게 보여준다(사양서 §2.2.4 의 6종 문구).
class SocialLoginException implements Exception {
  const SocialLoginException(this.message);

  final String message;

  @override
  String toString() => 'SocialLoginException($message)';
}

/// 콜백 URL 검증(사양서 §2.2.4). 인가 코드를 돌려주고, 실패는 사용자 문구를 담아 던진다.
///
/// state 검증은 **반드시 클라이언트가** 한다. 서버는 state 를 보지 않는다.
String verifySocialCallback(
  Uri callback, {
  required SocialProvider provider,
  required String? expectedState,
  required String? codeVerifier,
}) {
  final params = callback.queryParameters;

  final error = params['error'];
  if (error != null && error.isNotEmpty) {
    throw SocialLoginException('${AuthConfig.label(provider)} 로그인이 취소되었거나 실패했습니다.');
  }

  final code = params['code'];
  final state = params['state'];
  if (code == null || code.isEmpty || state == null || state.isEmpty) {
    throw const SocialLoginException('인가 정보가 올바르지 않습니다.');
  }
  if (expectedState == null || expectedState.isEmpty || state != expectedState) {
    throw const SocialLoginException('보안 검증(state)에 실패했습니다. 다시 시도해주세요.');
  }
  if (codeVerifier == null || codeVerifier.isEmpty) {
    throw const SocialLoginException('로그인 검증값이 없습니다. 다시 시도해주세요.');
  }
  return code;
}

/// PKCE 비밀값 보관소(계획서 §4.3.3).
///
/// 저장은 하되 **읽지 않는다.** 인가 도중 OS 가 프로세스를 죽이면 `await` 도 함께 사라지므로
/// 복구 경로를 만들지 않는다. 대신 교환 성공·실패 무관 즉시 삭제하고 부팅 시 무조건 삭제해
/// 오염값이 남지 않게 닫는다.
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

  Future<void> put({
    required SocialProvider provider,
    required String verifier,
    required String state,
  }) async {
    try {
      await _storage.write(key: _providerKey, value: provider.wire);
      await _storage.write(key: _verifierKey, value: verifier);
      await _storage.write(key: _stateKey, value: state);
    } catch (_) {
      // 저장 실패가 로그인을 막지 않는다. 진행 중인 인가는 메모리 값만으로 끝난다.
    }
  }

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

/// 구글 인가 화면 호출부. 테스트에서 플러그인을 대체할 수 있도록 함수로 받는다.
typedef WebAuthenticator =
    Future<String> Function({
      required String url,
      required String callbackUrlScheme,
    });

Future<String> _authenticateWithWebAuth({
  required String url,
  required String callbackUrlScheme,
}) => FlutterWebAuth2.authenticate(
  url: url,
  callbackUrlScheme: callbackUrlScheme,
);

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

/// 소셜 인가. 제공자로 분기한다 — 카카오는 공식 SDK 로 액세스 토큰을, 구글은 기존
/// flutter_web_auth_2 인가 코드 흐름(Android 는 Chrome Custom Tabs, iOS 는
/// `ASWebAuthenticationSession`)을 쓴다. 반환값은 곧바로 서버 교환에 넣는 [LoginRequest] 다.
class SocialLoginService {
  SocialLoginService({
    required OAuthSecrets secrets,
    WebAuthenticator authenticator = _authenticateWithWebAuth,
    KakaoTokenProvider kakaoTokenProvider = _authenticateWithKakao,
  }) : _secrets = secrets,
       _authenticate = authenticator,
       _kakaoToken = kakaoTokenProvider;

  final OAuthSecrets _secrets;
  final WebAuthenticator _authenticate;
  final KakaoTokenProvider _kakaoToken;

  Future<LoginRequest> authorize(SocialProvider provider) => switch (provider) {
    SocialProvider.kakao => _authorizeKakao(),
    SocialProvider.google => _authorizeGoogle(),
  };

  /// 카카오: SDK 가 앱에서 직접 액세스 토큰을 돌려준다. 취소·실패는 SDK 예외로 오므로
  /// 크래시 대신 다시 시도할 수 있는 사용자 문구로 감싼다.
  Future<LoginRequest> _authorizeKakao() async {
    const provider = SocialProvider.kakao;
    try {
      final accessToken = await _kakaoToken();
      return LoginRequest.kakao(
        accessToken: accessToken,
        clientType: AuthConfig.clientType,
      );
    } on SocialLoginException {
      rethrow;
    } catch (_) {
      throw SocialLoginException(
        '${AuthConfig.label(provider)} 로그인이 취소되었거나 실패했습니다.',
      );
    }
  }

  /// 구글: 브라우저 인가 코드 흐름. 콜백 URL 은 `authenticate()` 의 반환값으로 온다.
  Future<LoginRequest> _authorizeGoogle() async {
    const provider = SocialProvider.google;
    if (!AuthConfig.isConfigured(provider)) {
      throw SocialLoginException(
        '${AuthConfig.label(provider)} 로그인 설정이 완료되지 않았습니다.',
      );
    }

    final verifier = Pkce.verifier();
    final state = Pkce.state();
    await _secrets.put(provider: provider, verifier: verifier, state: state);

    try {
      final callback = await _authenticate(
        url: AuthConfig.authorizeUrl(
          provider,
          challenge: Pkce.challenge(verifier),
          state: state,
        ).toString(),
        callbackUrlScheme: AuthConfig.callbackScheme(provider),
      );
      final code = verifySocialCallback(
        Uri.parse(callback),
        provider: provider,
        expectedState: state,
        codeVerifier: verifier,
      );
      return LoginRequest.google(
        code: code,
        codeVerifier: verifier,
        clientType: AuthConfig.clientType,
      );
    } on PlatformException {
      // 사용자가 인가 화면을 닫으면 CANCELED 가 온다. 콜백 스킴이 OS 에 등록되지 않은 경우도
      // 여기로 떨어지므로, 크래시 대신 다시 시도할 수 있는 문구를 남긴다.
      throw SocialLoginException(
        '${AuthConfig.label(provider)} 로그인이 취소되었거나 실패했습니다.',
      );
    } finally {
      await _secrets.clear();
    }
  }
}

final oauthSecretsProvider = Provider<OAuthSecrets>((ref) => OAuthSecrets());

final socialLoginProvider = Provider<SocialLoginService>(
  (ref) => SocialLoginService(secrets: ref.watch(oauthSecretsProvider)),
);
