import 'package:flutter_test/flutter_test.dart';
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
}
