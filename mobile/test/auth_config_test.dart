import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/auth/auth_config.dart';
import 'package:trypto/models/enums.dart';

void main() {
  group('AuthConfig', () {
    test('카카오는 네이티브 앱 키 기본값만으로 버튼이 살아 있다', () {
      // --dart-define 이 없어도 카카오는 SDK 로 동작하므로 네이티브 앱 키 기본값으로 설정 완료다.
      expect(AuthConfig.isConfigured(SocialProvider.kakao), isTrue);
      expect(AuthConfig.missingDefines(SocialProvider.kakao), isEmpty);
    });

    test('구글도 serverClientId 기본값만으로 버튼이 살아 있다', () {
      // 구글 역시 SDK 로 동작하며, SDK 가 요구하는 값은 serverClientId(웹 클라이언트 ID) 하나다.
      // 클라이언트 ID 는 비밀이 아니어서 Env 에 기본값을 두므로(env.dart), --dart-define 이 없는
      // 테스트 환경에서도 설정 완료다.
      expect(AuthConfig.isConfigured(SocialProvider.google), isTrue);
      expect(AuthConfig.missingDefines(SocialProvider.google), isEmpty);
    });

    test('버튼 활성 판정과 누락 키 목록은 언제나 같은 조건에서 갈린다', () {
      // 로그인 화면은 isConfigured 로 버튼을 잠그고 missingDefines 로 그 사유를 보여준다
      // (login_page.dart). 둘이 어긋나면 사유 없이 잠긴 버튼이나, 잠기지 않았는데 미설정 문구만
      // 뜨는 화면이 나온다. Env 값은 컴파일 타임 상수라 테스트에서 비울 수 없으므로 설정이 빠진
      // 상황을 직접 만드는 대신, 두 판정이 같은 조건을 보고 있는지를 지킨다.
      for (final provider in SocialProvider.values) {
        expect(
          AuthConfig.isConfigured(provider),
          AuthConfig.missingDefines(provider).isEmpty,
          reason: '${provider.wire}: 버튼 활성 판정과 누락 키 목록이 어긋난다',
        );
      }
    });
  });
}
