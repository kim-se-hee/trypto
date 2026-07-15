import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/features/ranking/ranker_portfolio_sheet.dart';

void main() {
  group('asRatio', () {
    // 서버가 비중을 0.35 로 주든 35 로 주든 35.0% 여야 한다(사양서 §6.2.7).
    test('1 이하는 비율 그대로 본다', () {
      expect(asRatio(0.35), 0.35);
      expect(asRatio(0), 0);
      expect(asRatio(1), 1);
    });

    test('1 초과는 퍼센트로 보고 100 으로 나눈다', () {
      expect(asRatio(35), 0.35);
      expect(asRatio(100), 1);
    });
  });

  group('canViewRankerPortfolio', () {
    test('100위까지만 열람할 수 있다', () {
      expect(canViewRankerPortfolio(1), isTrue);
      expect(canViewRankerPortfolio(100), isTrue);
      // 101위 이상은 서버가 403 을 낸다. 요청 자체를 보내지 않는다.
      expect(canViewRankerPortfolio(101), isFalse);
    });
  });
}
