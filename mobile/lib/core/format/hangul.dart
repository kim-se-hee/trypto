/// 웹 `frontend/src/lib/hangul.ts` + `MarketPage.tsx:56-99` 의 이식본(사양서 §4.2.5).
library;

const int _hangulFirst = 0xAC00; // '가'
const int _hangulLast = 0xD7A3; // '힣'
const int _jungsungCount = 21;
const int _jongsungCount = 28;

const List<String> _chosungTable = [
  'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', //
  'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
];
const List<String> _jungsungTable = [
  'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', //
  'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ',
];
const List<String> _jongsungTable = [
  '', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ', //
  'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
  'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
];

/// 겹자모는 자판에서 두 번에 나눠 눌리므로, 분해도 눌린 순서대로 푼다.
/// 이 덕분에 조합 중인 글자('빝' → ㅂㅣㅌ)가 완성된 이름('비트' → ㅂㅣㅌㅡ)의 앞부분과 맞아떨어진다.
const Map<String, String> _compound = {
  'ㄲ': 'ㄱㄱ', 'ㄳ': 'ㄱㅅ', 'ㄵ': 'ㄴㅈ', 'ㄶ': 'ㄴㅎ', 'ㄸ': 'ㄷㄷ', //
  'ㄺ': 'ㄹㄱ', 'ㄻ': 'ㄹㅁ', 'ㄼ': 'ㄹㅂ', 'ㄽ': 'ㄹㅅ', 'ㄾ': 'ㄹㅌ',
  'ㄿ': 'ㄹㅍ', 'ㅀ': 'ㄹㅎ', 'ㅃ': 'ㅂㅂ', 'ㅄ': 'ㅂㅅ', 'ㅆ': 'ㅅㅅ',
  'ㅉ': 'ㅈㅈ', 'ㅘ': 'ㅗㅏ', 'ㅙ': 'ㅗㅐ', 'ㅚ': 'ㅗㅣ', 'ㅝ': 'ㅜㅓ',
  'ㅞ': 'ㅜㅔ', 'ㅟ': 'ㅜㅣ', 'ㅢ': 'ㅡㅣ',
};

String _split(String jamo) => _compound[jamo] ?? jamo;

bool _isSyllable(int code) => code >= _hangulFirst && code <= _hangulLast;

final RegExp _consonantsOnly = RegExp(r'^[ㄱ-ㅎ]+$');

/// '비트코인' → 'ㅂㅌㅋㅇ'. 한글 음절이 아닌 글자는 그대로 둔다.
String toChosung(String text) {
  final buffer = StringBuffer();
  for (final rune in text.runes) {
    if (_isSyllable(rune)) {
      final offset = rune - _hangulFirst;
      buffer.write(_chosungTable[offset ~/ (_jungsungCount * _jongsungCount)]);
    } else {
      buffer.writeCharCode(rune);
    }
  }
  return buffer.toString();
}

/// '비트코인' → 'ㅂㅣㅌㅡㅋㅗㅇㅣㄴ'. 조합 중인 글자도 같은 규칙으로 풀린다.
String toJamo(String text) {
  final buffer = StringBuffer();
  for (final rune in text.runes) {
    if (_isSyllable(rune)) {
      final offset = rune - _hangulFirst;
      final cho = _chosungTable[offset ~/ (_jungsungCount * _jongsungCount)];
      final jung = _jungsungTable[(offset ~/ _jongsungCount) % _jungsungCount];
      final jong = _jongsungTable[offset % _jongsungCount];
      buffer
        ..write(_split(cho))
        ..write(_split(jung))
        ..write(_split(jong));
    } else {
      buffer.write(_split(String.fromCharCode(rune)));
    }
  }
  return buffer.toString();
}

/// 자음만으로 이루어진 질의인가. 참이면 초성 검색 경로를 탄다.
bool isChosungQuery(String text) => _consonantsOnly.hasMatch(text);

/// 코인 한글명의 검색 색인. 이름은 시세와 달리 바뀌지 않으므로 정적 목록을 받은 시점에 한 번만 만든다.
class HangulIndex {
  HangulIndex(String name)
    : chosung = toChosung(name).toLowerCase(),
      jamo = toJamo(name).toLowerCase();

  final String chosung;
  final String jamo;
}

/// 검색어 하나를 해석해 둔 것. 코인 600개를 훑는 동안 자모 변환을 반복하지 않는다.
class HangulQuery {
  HangulQuery(String input) : this._(input.trim().toLowerCase());

  HangulQuery._(this.text)
    : isChosung = isChosungQuery(text),
      jamo = toJamo(text);

  final String text;
  final bool isChosung;
  final String jamo;

  bool get isEmpty => text.isEmpty;

  /// 초성 질의는 `startsWith`, 자모 질의는 `contains` 다. 이 비대칭은 웹 동작 그대로다.
  bool matches(String symbol, HangulIndex index) {
    if (isEmpty) return true;
    if (symbol.toLowerCase().contains(text)) return true;
    return isChosung ? index.chosung.startsWith(text) : index.jamo.contains(jamo);
  }
}
