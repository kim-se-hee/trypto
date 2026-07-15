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

  /// 구글 브라우저 인가 코드 흐름의 콜백 스킴(Dart 단일 출처). 값을 바꾸면 네이티브 등록처도 같이 바꾼다.
  /// - android/app/build.gradle.kts : manifestPlaceholders["authCallbackScheme"]
  /// - ios/Runner/Info.plist        : CFBundleURLSchemes
  static const googleCallbackScheme = String.fromEnvironment(
    'GOOGLE_CALLBACK_SCHEME',
    defaultValue: _defaultCallbackScheme,
  );

  /// 카카오는 SDK 로 전환해 브라우저 콜백 스킴을 쓰지 않는다. 이 값은 SDK 의 카카오톡 로그인
  /// 리다이렉트 처리기(`AuthCodeHandlerActivity`)가 쓰는 `kakao{네이티브앱키}://oauth` 스킴을
  /// 문서화할 뿐이며, 실제 등록은 AndroidManifest 에 직접 박아 둔다.
  static const kakaoCallbackScheme = String.fromEnvironment(
    'KAKAO_CALLBACK_SCHEME',
    defaultValue: 'kakao$_kakaoNativeAppKey',
  );

  /// 카카오 공식 SDK 초기화용 네이티브 앱 키. 카카오는 브라우저 리다이렉트가 불가해 SDK 로
  /// 앱에서 액세스 토큰을 받는다(커스텀 스킴 콜백을 쓰지 않는다).
  static const kakaoNativeAppKey = String.fromEnvironment(
    'KAKAO_NATIVE_APP_KEY',
    defaultValue: _kakaoNativeAppKey,
  );

  static const _kakaoNativeAppKey = 'df7305f4a85506b955eafc916218ca7b';
  static const _defaultCallbackScheme = 'trypto';
}
