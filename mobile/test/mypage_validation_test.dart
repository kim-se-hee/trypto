import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/api/api_exception.dart';
import 'package:trypto/features/mypage/feedback_card.dart';
import 'package:trypto/features/mypage/nickname_sheet.dart';

void main() {
  group('validateNickname', () {
    test('2~20자만 통과한다', () {
      expect(validateNickname('가나'), isNull);
      expect(validateNickname('가' * 20), isNull);
    });

    test('경계 밖은 막는다', () {
      expect(validateNickname('가'), isNotNull);
      expect(validateNickname(''), isNotNull);
      expect(validateNickname('가' * 21), isNotNull);
    });

    // 서버도 strip 후 길이를 잰다. 공백만 채워 통과시킬 수 없다.
    test('길이는 trim 기준이다', () {
      expect(validateNickname('  가  '), isNotNull);
      expect(validateNickname('  가나  '), isNull);
    });
  });

  group('validateFeedback', () {
    test('trim 기준 20~1000자만 통과한다', () {
      expect(validateFeedback('가' * 19), isNotNull);
      expect(validateFeedback('가' * 20), isNull);
      expect(validateFeedback('가' * 1000), isNull);
      expect(validateFeedback('가' * 1001), isNotNull);
    });

    test('공백은 길이에 들어가지 않는다', () {
      expect(validateFeedback('${'가' * 19} '), isNotNull);
      expect(validateFeedback('  ${'가' * 20}  '), isNull);
    });
  });

  // 닉네임 저장·피드백 전송은 `on ApiException` 하나로만 실패를 받는다. 응답을 DTO 로 캐스팅하다
  // 나는 `TypeError` 처럼 Dio 를 거치지 않는 예외가 그대로 새어 나가면 로딩 플래그가 켜진 채로
  // 굳으므로, apiCall 이 모든 예외를 ApiException 으로 좁혀야 한다.
  group('apiCall', () {
    test('Dio 를 거치지 않는 예외도 ApiException 으로 좁힌다', () async {
      final Object body = '봉투가 아닌 응답';
      await expectLater(
        apiCall(() async => body as Map<String, dynamic>),
        throwsA(
          isA<ApiException>().having(
            (error) => error.userMessage,
            'userMessage',
            '요청 처리 중 오류가 발생했습니다.',
          ),
        ),
      );
    });

    test('이미 ApiException 이면 서버 문구를 그대로 둔다', () async {
      await expectLater(
        apiCall<void>(
          () async => throw const ApiException(
            status: 409,
            message: '이미 사용 중인 닉네임입니다.',
          ),
        ),
        throwsA(
          isA<ApiException>().having(
            (error) => error.userMessage,
            'userMessage',
            '이미 사용 중인 닉네임입니다.',
          ),
        ),
      );
    });
  });
}
