import 'dart:io';

import '../../models/enums.dart';
import '../env.dart';

/// 소셜 로그인 설정값의 단일 출처(계획서 §4.3.4).
///
/// 두 제공자 모두 공식 SDK 로 앱에서 직접 토큰을 받는다 — 카카오는 네이티브 앱 키로 액세스
/// 토큰을, 구글은 serverClientId(웹 클라이언트 ID)로 ID 토큰을 받는다. 브라우저 인가 코드
/// 흐름(PKCE·커스텀 스킴 콜백)은 쓰지 않으므로 인가 URL·redirect URI 조립은 여기에 없다.
abstract final class AuthConfig {
  /// 구글의 Android 클라이언트와 iOS 클라이언트는 별개다. 서버가 플랫폼별 제공자 자격증명을
  /// 고르도록 로그인 요청에 이 값을 실어 보낸다.
  static ClientType get clientType =>
      Platform.isAndroid ? ClientType.android : ClientType.ios;

  static String label(SocialProvider provider) => switch (provider) {
    SocialProvider.kakao => '카카오',
    SocialProvider.google => '구글',
  };

  /// 버튼을 살리는 조건은 제공자별로 다르다(사양서 §2.4). 둘 다 SDK 로 동작한다 — 카카오는 네이티브
  /// 앱 키, 구글은 serverClientId(웹 클라이언트 ID)만 있으면 된다. 둘 다 기본값이 있어 항상 채워진다.
  static bool isConfigured(SocialProvider provider) => switch (provider) {
    SocialProvider.kakao => Env.kakaoNativeAppKey.isNotEmpty,
    SocialProvider.google => Env.googleServerClientId.isNotEmpty,
  };

  /// 비어 있는 `--dart-define` 키 목록. 디버그 빌드에서만 노출한다.
  static List<String> missingDefines(SocialProvider provider) =>
      switch (provider) {
        SocialProvider.kakao => [
          if (Env.kakaoNativeAppKey.isEmpty) 'KAKAO_NATIVE_APP_KEY',
        ],
        SocialProvider.google => [
          if (Env.googleServerClientId.isEmpty) 'GOOGLE_SERVER_CLIENT_ID',
        ],
      };
}
