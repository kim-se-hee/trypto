import 'package:decimal/decimal.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/json/decimal_x.dart';

/// 계획서 §4.5.1 의 경계값. double → Decimal 승격에서 값이 새면 잔고·수수료가 조용히 어긋난다.
void main() {
  group('DecimalX.dec', () {
    const cases = <(double, String)>[
      (0.00012345, '0.00012345'),
      (1e-8, '0.00000001'), // BTC 먼지 수량. Dart 는 이 값을 "1e-8" 로 출력한다
      (152340000.5, '152340000.5'),
      (1000000000, '1000000000'),
      (0.12345678, '0.12345678'), // 코인 수량의 최대 소수 8자리
      (0, '0'),
      (-0.5, '-0.5'),
    ];
    for (final (input, expected) in cases) {
      test('$input → $expected', () {
        expect(input.dec, Decimal.parse(expected));
      });
    }

    test('1e-8 은 지수 표기로 문자열화된다 — 승격 통로가 이것을 파싱해야 한다', () {
      expect((1e-8).toString(), '1e-8');
      expect((1e-8).dec, Decimal.parse('0.00000001'));
    });

    test('double 로 되돌리면 원값이 복원된다', () {
      const values = [0.00012345, 1e-8, 152340000.5, 1000000000.0, 0.12345678];
      for (final value in values) {
        expect(value.dec.toDouble(), value);
      }
    });
  });

  group('나눗셈 스케일', () {
    test('무한소수는 스케일을 명시해야 Decimal 이 된다', () {
      final quotient = (Decimal.fromInt(1) / Decimal.fromInt(3)).toDecimal(
        scaleOnInfinitePrecision: 8,
      );
      expect(quotient, Decimal.parse('0.33333333'));
    });

    test('총액 ÷ 가격 = 수량 (소수 8자리)', () {
      final quantity = (100000.0.dec / 84500000.0.dec).toDecimal(
        scaleOnInfinitePrecision: 8,
      );
      expect(quantity, Decimal.parse('0.00118343'));
    });
  });

  group('연산 정밀도', () {
    test('double 로는 어긋나는 덧셈이 Decimal 에서는 맞는다', () {
      expect(0.1 + 0.2 == 0.3, isFalse);
      expect(0.1.dec + 0.2.dec, 0.3.dec);
    });

    test('수수료 = 총액 × 수수료율', () {
      expect(100000.0.dec * 0.0005.dec, Decimal.parse('50'));
    });
  });
}
