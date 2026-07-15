import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/format/formatters.dart';

/// 사양서 §8.5 의 입출력 표를 그대로 케이스로 옮긴 것이다.
/// 웹과 한 글자라도 달라지면 여기서 걸린다.
void main() {
  group('formatKRW (§8.5.1)', () {
    const cases = <(double, String)>[
      (0, '0원'),
      (1234, '1,234원'),
      (9999, '9,999원'),
      (10000, '1만원'),
      (15000, '2만원'), // 1.5 를 반올림하므로 2
      (12345678, '1,235만원'),
      (99999999, '10,000만원'), // 1억 미만이라 만원 분기
      (100000000, '1억원'),
      (123456789, '1억 2,346만원'),
      (999999999, '9억 10,000만원'), // 만 자리가 10,000 으로 반올림되는 실제 동작
      (1000000000, '10억원'),
      (-50000, '-5만원'),
    ];
    for (final (input, expected) in cases) {
      test('$input → $expected', () => expect(formatKRW(input), expected));
    }
  });

  group('formatKRWCompact (§8.5.2)', () {
    const cases = <(double, String)>[
      (0, '0'),
      (1234, '1,234'),
      (1234.5678, '1,234.568'), // 소수 3자리까지
      (-1234, '-1,234'),
      (10000, '1만'),
      (100000000, '1억'),
      (123456789, '1억 2,346만'),
    ];
    for (final (input, expected) in cases) {
      test(
        '$input → $expected',
        () => expect(formatKRWCompact(input), expected),
      );
    }
  });

  group('formatCurrency (§8.5.3)', () {
    const cases = <(double, String, String)>[
      (1234.5, 'USDT', r'$1,234.50'),
      (0, 'USDT', r'$0.00'),
      (-12.345, 'USDT', r'$-12.35'), // 달러 기호 뒤에 마이너스가 온다
      (123456789, 'KRW', '1억 2,346만원'),
      (123456789, 'SOL', '1억 2,346만원'), // SOL 도 원화 분기로 떨어진다
    ];
    for (final (value, currency, expected) in cases) {
      test(
        '($value, $currency) → $expected',
        () => expect(formatCurrency(value, currency), expected),
      );
    }
  });

  group('formatCurrencyCompact (§8.5.4)', () {
    const cases = <(double, String, String)>[
      (1234.5, 'USDT', r'$1,234.50'),
      (123456789, 'KRW', '1억 2,346만'),
      (1234, 'KRW', '1,234'),
    ];
    for (final (value, currency, expected) in cases) {
      test(
        '($value, $currency) → $expected',
        () => expect(formatCurrencyCompact(value, currency), expected),
      );
    }
  });

  group('formatFiatEstimate (§8.5.5)', () {
    const cases = <(double, String, String)>[
      (1234.5, 'USDT', r'≈ $1,234.50'),
      (52340000, 'KRW', '≈ 5,234만원'),
    ];
    for (final (value, currency, expected) in cases) {
      test(
        '($value, $currency) → $expected',
        () => expect(formatFiatEstimate(value, currency), expected),
      );
    }
  });

  group('formatQuantity (§8.5.6)', () {
    const cases = <(double, String)>[
      (1234567.89, '1,234,568'),
      (1234.5, '1,234.50'),
      (1.5, '1.5000'),
      (12.345678, '12.3457'),
      (0.5, '0.5000'),
      (0.000012345678, '0.00001235'),
      (0, '0.0000'),
      (-5, '-5.0000'), // 음수는 마지막 분기로 떨어진다
    ];
    for (final (input, expected) in cases) {
      test('$input → $expected', () => expect(formatQuantity(input), expected));
    }
  });

  group('formatPrice (§8.5.7)', () {
    const cases = <(double, String, String)>[
      (84500000, 'KRW', '84,500,000'),
      (1234.5678, 'KRW', '1,234.568'),
      (0.5, 'KRW', '0.5'),
      (1234.5, 'USDT', '1,234.50'),
      (1.5, 'USDT', '1.50'),
      (1.23456, 'USDT', '1.2346'),
      (0.5, 'USDT', '0.5000'),
      (2.5, 'SOL', '2.5000'),
      (0.5, 'SOL', '0.5000'),
      (0.00001, 'SOL', '1.00e-5'),
    ];
    for (final (value, currency, expected) in cases) {
      test(
        '($value, $currency) → $expected',
        () => expect(formatPrice(value, currency), expected),
      );
    }

    test('통화 기호를 붙이지 않는다', () {
      expect(formatPrice(84500000, 'KRW'), isNot(contains('₩')));
    });
  });

  group('formatVolume (§8.5.8)', () {
    const cases = <(double, String, String)>[
      (123456789, 'KRW', '123,456,789'),
      (1234567.891, 'USDT', r'$1,234,567.891'),
      (1234.5, 'SOL', '◎1,234.5'),
    ];
    for (final (value, currency, expected) in cases) {
      test(
        '($value, $currency) → $expected',
        () => expect(formatVolume(value, currency), expected),
      );
    }
  });

  group('formatChangeRate (§8.5.9)', () {
    const cases = <(double, String)>[
      (0.0234, '+2.34%'),
      (-0.0125, '-1.25%'),
      (0, '0.00%'), // + 가 붙지 않는다
      (-0.00001, '-0.00%'), // 실제 동작. 그대로 재현한다
      (1.5, '+150.00%'),
    ];
    for (final (input, expected) in cases) {
      test(
        '$input → $expected',
        () => expect(formatChangeRate(input), expected),
      );
    }

    test('천단위 구분자가 없다', () {
      expect(formatChangeRate(12.3456), '+1234.56%');
    });
  });

  group('getCurrencySymbol (§8.5.10)', () {
    const cases = <(String, String)>[
      ('KRW', '₩'),
      ('SOL', '◎'),
      ('USDT', ''),
      ('ETH', ''),
    ];
    for (final (input, expected) in cases) {
      test(
        '$input → "$expected"',
        () => expect(getCurrencySymbol(input), expected),
      );
    }
  });

  group('smallAmountThreshold (§8.5.11)', () {
    test('KRW → 1000', () => expect(smallAmountThreshold('KRW'), 1000));
    test('USDT → 1', () => expect(smallAmountThreshold('USDT'), 1));
    test('키가 없으면 1', () => expect(smallAmountThreshold('SOL'), 1));
  });
}
