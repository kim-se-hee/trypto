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

  /// 구글은 안드로이드에서 커스텀 스킴 리다이렉트가 폐지돼(앱 사칭 위험) 공식 SDK(google_sign_in)로
  /// 네이티브 로그인한다. SDK 에 넘기는 serverClientId(= 웹 클라이언트 ID)가 ID 토큰의 대상(aud)이
  /// 되고, 백엔드가 같은 값으로 검증한다. 클라이언트 ID 는 비밀이 아니므로 기본값을 둔다.
  static const googleServerClientId = String.fromEnvironment(
    'GOOGLE_SERVER_CLIENT_ID',
    defaultValue: _googleServerClientId,
  );

  static const _googleServerClientId =
      '218669927577-c4kei34t4og2ddh9jq8a3i46fsr61rrp.apps.googleusercontent.com';

  /// 카카오 공식 SDK 초기화용 네이티브 앱 키. 카카오는 브라우저 리다이렉트가 불가해 SDK 로
  /// 앱에서 액세스 토큰을 받는다(커스텀 스킴 콜백을 쓰지 않는다).
  static const kakaoNativeAppKey = String.fromEnvironment(
    'KAKAO_NATIVE_APP_KEY',
    defaultValue: _kakaoNativeAppKey,
  );

  static const _kakaoNativeAppKey = 'df7305f4a85506b955eafc916218ca7b';
}
