import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/format/hangul.dart';

/// 사양서 §4.2.5. 초성은 `startsWith`, 자모는 `contains` 라는 비대칭이 핵심이다.
void main() {
  group('toChosung', () {
    const cases = <(String, String)>[
      ('비트코인', 'ㅂㅌㅋㅇ'),
      ('이더리움', 'ㅇㄷㄹㅇ'),
      ('리플', 'ㄹㅍ'),
      ('도지코인', 'ㄷㅈㅋㅇ'),
      ('BTC', 'BTC'), // 한글이 아닌 글자는 그대로 둔다
      ('', ''),
    ];
    for (final (input, expected) in cases) {
      test('"$input" → "$expected"', () => expect(toChosung(input), expected));
    }
  });

  group('toJamo', () {
    const cases = <(String, String)>[
      ('비트코인', 'ㅂㅣㅌㅡㅋㅗㅇㅣㄴ'),
      ('비트', 'ㅂㅣㅌㅡ'),
      ('빝', 'ㅂㅣㅌ'), // 조합 중인 글자
      ('왔', 'ㅇㅗㅏㅅㅅ'), // 겹모음·겹받침을 자판 입력 순서대로 푼다
      ('의', 'ㅇㅡㅣ'),
      ('ㅘ', 'ㅗㅏ'), // 낱자로 들어온 겹모음
      ('BTC', 'BTC'), // 영문은 항등 함수
    ];
    for (final (input, expected) in cases) {
      test('"$input" → "$expected"', () => expect(toJamo(input), expected));
    }

    test('조합 중인 글자의 자모가 완성된 이름의 앞부분과 맞아떨어진다', () {
      expect(toJamo('비트코인').startsWith(toJamo('빝')), isTrue);
    });
  });

  group('isChosungQuery', () {
    const cases = <(String, bool)>[
      ('ㅂㅌ', true),
      ('ㄹㅍ', true),
      ('비트', false),
      ('ㅂㅣ', false), // 모음이 섞이면 초성 질의가 아니다
      ('btc', false),
      ('', false),
    ];
    for (final (input, expected) in cases) {
      test(
        '"$input" → $expected',
        () => expect(isChosungQuery(input), expected),
      );
    }
  });

  group('검색 (HangulQuery × HangulIndex)', () {
    const coins = <(String, String)>[
      ('BTC', '비트코인'),
      ('ETH', '이더리움'),
      ('XRP', '리플'),
      ('SOL', '솔라나'),
      ('DOGE', '도지코인'),
    ];
    final index = {
      for (final (symbol, name) in coins) symbol: HangulIndex(name),
    };

    List<String> search(String input) {
      final query = HangulQuery(input);
      return [
        for (final (symbol, _) in coins)
          if (query.matches(symbol, index[symbol]!)) symbol,
      ];
    }

    test('초성 질의는 앞부분만 맞춘다', () {
      expect(search('ㅂㅌ'), ['BTC']);
    });

    test('초성이 중간에 걸리는 것은 매칭되지 않는다 (startsWith 비대칭)', () {
      // '비트코인'/'도지코인' 의 초성에 'ㅋㅇ' 이 들어 있지만 앞부분이 아니다.
      expect(search('ㅋㅇ'), isEmpty);
    });

    test('초성 질의 — 리플', () {
      expect(search('ㄹㅍ'), ['XRP']);
    });

    test('자모 질의는 부분 일치다', () {
      expect(search('비트'), ['BTC']);
    });

    test('자모 질의가 이름 중간에 걸린다', () {
      expect(search('코인'), ['BTC', 'DOGE']);
    });

    test('조합 중인 자모로도 찾는다', () {
      expect(search('빝'), ['BTC']);
    });

    test('심볼 부분 일치는 대소문자를 가리지 않는다', () {
      expect(search('btc'), ['BTC']);
      expect(search('BTC'), ['BTC']);
      expect(search('sol'), ['SOL']);
    });

    test('앞뒤 공백을 버린다', () {
      expect(search('  btc  '), ['BTC']);
    });

    test('빈 질의는 전부 통과시킨다', () {
      expect(search(''), coins.map((c) => c.$1).toList());
      expect(search('   '), coins.map((c) => c.$1).toList());
    });

    test('일치하는 것이 없으면 빈 목록', () {
      expect(search('ㅎㅎㅎ'), isEmpty);
    });
  });
}
