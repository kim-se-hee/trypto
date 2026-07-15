import 'dart:io';

import '../../models/enums.dart';
import '../env.dart';

/// 인가 요청 값의 단일 출처(계획서 §4.3.4).
///
/// [redirectUri] 가 만드는 문자열은 서버 설정값(`{KAKAO|GOOGLE}_{ANDROID|IOS}_REDIRECT_URI`)과
/// **문자 단위로 같아야 한다.** 토큰 교환의 `redirect_uri` 는 서버가 자기 설정값으로 채우는데,
/// OAuth2 규격상 그 값이 인가 요청에 쓴 값과 다르면 제공자가 `redirect_uri_mismatch` 로 거절한다.
abstract final class AuthConfig {
  static const String _kakaoAuthorizeUrl =
      'https://kauth.kakao.com/oauth/authorize';
  static const String _googleAuthorizeUrl =
      'https://accounts.google.com/o/oauth2/v2/auth';

  /// 구글의 Android 클라이언트와 iOS 클라이언트는 별개이고, 토큰 교환의 `client_id` 는 인가
  /// 요청에 쓴 것과 같아야 한다. 그래서 서버에 보낼 [clientType] 과 [clientId] 가 **같은 판별식**
  /// 에서 나온다 — 둘이 어긋나면 제공자가 `unauthorized_client` 를 낸다.
  static ClientType get clientType =>
      Platform.isAndroid ? ClientType.android : ClientType.ios;

  static String label(SocialProvider provider) => switch (provider) {
    SocialProvider.kakao => '카카오',
    SocialProvider.google => '구글',
  };

  static String callbackScheme(SocialProvider provider) => switch (provider) {
    SocialProvider.kakao => Env.kakaoCallbackScheme,
    SocialProvider.google => Env.googleCallbackScheme,
  };

  /// 브라우저 인가 코드 흐름(구글)의 콜백 URI. 서버 설정값(`GOOGLE_{ANDROID|IOS}_REDIRECT_URI`)과
  /// 문자 단위로 같아야 토큰 교환이 성립한다. 카카오는 SDK 로 전환해 이 경로를 쓰지 않는다.
  static String redirectUri(SocialProvider provider) =>
      '${callbackScheme(provider)}://auth/${provider.wire}/callback';

  static String clientId(SocialProvider provider) => switch (provider) {
    SocialProvider.kakao => Env.kakaoClientId,
    SocialProvider.google => clientType == ClientType.android
        ? Env.googleAndroidClientId
        : Env.googleIosClientId,
  };

  /// 버튼을 살리는 조건은 제공자별로 다르다(사양서 §2.4). 카카오는 SDK 로 동작하므로 네이티브 앱
  /// 키만 있으면 되고(기본값이 있어 항상 채워진다), 구글은 `clientId` 와 콜백 스킴이 모두 있어야 한다.
  static bool isConfigured(SocialProvider provider) => switch (provider) {
    SocialProvider.kakao => Env.kakaoNativeAppKey.isNotEmpty,
    SocialProvider.google =>
      clientId(provider).isNotEmpty && callbackScheme(provider).isNotEmpty,
  };

  /// 비어 있는 `--dart-define` 키 목록. 디버그 빌드에서만 노출한다.
  static List<String> missingDefines(SocialProvider provider) =>
      switch (provider) {
        SocialProvider.kakao => [
          if (Env.kakaoNativeAppKey.isEmpty) 'KAKAO_NATIVE_APP_KEY',
        ],
        SocialProvider.google => [
          if (clientId(provider).isEmpty) _clientIdKey(provider),
          if (callbackScheme(provider).isEmpty) _callbackSchemeKey(provider),
        ],
      };

  static Uri authorizeUrl(
    SocialProvider provider, {
    required String challenge,
    required String state,
  }) {
    final authorizeUrl = switch (provider) {
      SocialProvider.kakao => _kakaoAuthorizeUrl,
      SocialProvider.google => _googleAuthorizeUrl,
    };
    return Uri.parse(authorizeUrl).replace(
      queryParameters: {
        'client_id': clientId(provider),
        'redirect_uri': redirectUri(provider),
        'response_type': 'code',
        'state': state,
        'code_challenge': challenge,
        'code_challenge_method': 'S256',
        if (provider == SocialProvider.google) 'scope': 'openid',
      },
    );
  }

  static String _clientIdKey(SocialProvider provider) => switch (provider) {
    SocialProvider.kakao => 'KAKAO_CLIENT_ID',
    SocialProvider.google => clientType == ClientType.android
        ? 'GOOGLE_ANDROID_CLIENT_ID'
        : 'GOOGLE_IOS_CLIENT_ID',
  };

  static String _callbackSchemeKey(SocialProvider provider) =>
      switch (provider) {
        SocialProvider.kakao => 'KAKAO_CALLBACK_SCHEME',
        SocialProvider.google => 'GOOGLE_CALLBACK_SCHEME',
      };
}
