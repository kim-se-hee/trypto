/// `--dart-define` 주입값. 기본값은 안드로이드 에뮬레이터가 호스트 백엔드를 가리키는 개발 설정이다.
class Env {
  const Env._();

  static const apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );
  static const wsBaseUrl = String.fromEnvironment(
    'WS_BASE_URL',
    defaultValue: 'ws://10.0.2.2:8080/ws',
  );

  static const kakaoClientId = String.fromEnvironment('KAKAO_CLIENT_ID');

  /// 구글의 Android 클라이언트와 iOS 클라이언트는 별개다. 토큰 교환의 client_id 는
  /// 인가 요청에 쓴 것과 같아야 하므로 플랫폼별로 키를 나눈다.
  static const googleAndroidClientId = String.fromEnvironment(
    'GOOGLE_ANDROID_CLIENT_ID',
  );
  static const googleIosClientId = String.fromEnvironment(
    'GOOGLE_IOS_CLIENT_ID',
  );

  /// OAuth 콜백 스킴의 Dart 단일 출처. 제공자 콘솔이 커스텀 스킴을 거부하면
  /// 제공자별로 값이 갈릴 수 있으므로 키를 둘로 나눠 둔다.
  ///
  /// 값을 바꾸면 네이티브 등록처 두 곳도 같이 바꾼다.
  /// - android/app/build.gradle.kts : manifestPlaceholders["authCallbackScheme"]
  /// - ios/Runner/Info.plist        : CFBundleURLSchemes
  static const kakaoCallbackScheme = String.fromEnvironment(
    'KAKAO_CALLBACK_SCHEME',
    defaultValue: _defaultCallbackScheme,
  );
  static const googleCallbackScheme = String.fromEnvironment(
    'GOOGLE_CALLBACK_SCHEME',
    defaultValue: _defaultCallbackScheme,
  );

  static const _defaultCallbackScheme = 'trypto';
}
