/**
 * 한글 검색용 문자열 변환.
 *
 * 완성형 한글(가~힣)은 코드값이 규칙적으로 배열되어 있다.
 *   코드 = 0xAC00 + (초성 × 21 + 중성) × 28 + 종성
 * 덕분에 사전이나 형태소 분석 없이 나눗셈만으로 자모를 얻는다.
 */

const HANGUL_FIRST = 0xac00; // '가'
const HANGUL_LAST = 0xd7a3; // '힣'
const JUNGSUNG_COUNT = 21;
const JONGSUNG_COUNT = 28;

const CHOSUNG = [
  "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ",
  "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ",
];

const JUNGSUNG = [
  "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ",
  "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ",
];

const JONGSUNG = [
  "", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ",
  "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ",
  "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ",
];

// 겹자모는 자판에서 두 번에 나눠 눌린다. 분해할 때도 눌린 순서대로 풀어야
// 조합 중인 글자가 완성된 이름의 앞부분과 맞아떨어진다.
// 예: '비트' 를 치는 도중의 '빝' → ㅂㅣㅌ 이고, 이는 '비트'(ㅂㅣㅌㅡ)의 앞부분이다.
const COMPOUND: Record<string, string> = {
  "ㄲ": "ㄱㄱ", "ㄳ": "ㄱㅅ", "ㄵ": "ㄴㅈ", "ㄶ": "ㄴㅎ", "ㄸ": "ㄷㄷ",
  "ㄺ": "ㄹㄱ", "ㄻ": "ㄹㅁ", "ㄼ": "ㄹㅂ", "ㄽ": "ㄹㅅ", "ㄾ": "ㄹㅌ",
  "ㄿ": "ㄹㅍ", "ㅀ": "ㄹㅎ", "ㅃ": "ㅂㅂ", "ㅄ": "ㅂㅅ", "ㅆ": "ㅅㅅ",
  "ㅉ": "ㅈㅈ", "ㅘ": "ㅗㅏ", "ㅙ": "ㅗㅐ", "ㅚ": "ㅗㅣ", "ㅝ": "ㅜㅓ",
  "ㅞ": "ㅜㅔ", "ㅟ": "ㅜㅣ", "ㅢ": "ㅡㅣ",
};

const CONSONANTS_ONLY = /^[ㄱ-ㅎ]+$/;

function split(jamo: string): string {
  return COMPOUND[jamo] ?? jamo;
}

function isSyllable(code: number): boolean {
  return code >= HANGUL_FIRST && code <= HANGUL_LAST;
}

/** '비트코인' → 'ㅂㅌㅋㅇ'. 한글이 아닌 글자는 그대로 둔다. */
export function toChosung(text: string): string {
  let result = "";
  for (const char of text) {
    const code = char.charCodeAt(0);
    if (isSyllable(code)) {
      const offset = code - HANGUL_FIRST;
      result += CHOSUNG[Math.floor(offset / (JUNGSUNG_COUNT * JONGSUNG_COUNT))];
    } else {
      result += char;
    }
  }
  return result;
}

/**
 * '비트코인' → 'ㅂㅣㅌㅡㅋㅗㅇㅣㄴ'. 한글이 아닌 글자는 그대로 둔다.
 *
 * 조합 중인 글자도 같은 규칙으로 풀리므로, 타이핑 도중의 입력이 완성된 이름의
 * 앞부분과 그대로 맞아떨어진다.
 */
export function toJamo(text: string): string {
  let result = "";
  for (const char of text) {
    const code = char.charCodeAt(0);
    if (isSyllable(code)) {
      const offset = code - HANGUL_FIRST;
      const chosung = CHOSUNG[Math.floor(offset / (JUNGSUNG_COUNT * JONGSUNG_COUNT))];
      const jungsung = JUNGSUNG[Math.floor(offset / JONGSUNG_COUNT) % JUNGSUNG_COUNT];
      const jongsung = JONGSUNG[offset % JONGSUNG_COUNT];
      result += split(chosung) + split(jungsung) + split(jongsung);
    } else {
      result += split(char);
    }
  }
  return result;
}

/** 자음만 입력했는지. 'ㅂㅌ' 처럼 자음뿐이면 이름 원문에는 없는 글자라 초성과 맞춰야 한다. */
export function isChosungQuery(text: string): boolean {
  return CONSONANTS_ONLY.test(text);
}
