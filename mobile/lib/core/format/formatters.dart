import 'package:intl/intl.dart';

/// 웹 `frontend/src/lib/formatters.ts` 의 이식본(사양서 §8.5). 모든 화면은 이 파일의 함수만 쓴다.
///
/// 로케일을 `en_US` 로 못 박는다. 여기 쓰는 패턴은 전부 숫자와 구분자뿐이라 ko-KR 과 출력이
/// 같고(§8.5.0), 로케일 데이터를 초기화하지 않아도 어디서나 같은 결과가 나온다.
const String _locale = 'en_US';

/// `NumberFormat` 생성자는 패턴 문자열을 파싱한다. 함수 안에서 만들면 티커 갱신마다
/// 프레임당 수백 µs 를 그냥 버린다(계획서 §4.2.5-9). 최상위에 한 번만 만든다.
final NumberFormat _grp = NumberFormat('#,##0.###', _locale); // 소수 0~3 (기본)
final NumberFormat _int0 = NumberFormat('#,##0', _locale); // 소수 0
final NumberFormat _fix2 = NumberFormat('#,##0.00', _locale); // 소수 정확히 2
final NumberFormat _fix4 = NumberFormat('#,##0.0000', _locale); // 소수 정확히 4
final NumberFormat _f2to4 = NumberFormat('#,##0.00##', _locale); // 소수 2~4
final NumberFormat _f4to8 = NumberFormat('#,##0.0000####', _locale); // 소수 4~8

const double _eok = 100000000;
const double _man = 10000;

/// 원화, 단위 포함 (카드·요약용). §8.5.1
String formatKRW(double value) {
  final abs = value.abs();
  final sign = value < 0 ? '-' : '';
  if (abs >= _eok) {
    final eok = (abs / _eok).floor();
    final man = ((abs % _eok) / _man).round();
    if (man > 0) return '$sign$eok억 ${_int0.format(man)}만원';
    return '$sign$eok억원';
  }
  if (abs >= _man) return '$sign${_int0.format((abs / _man).round())}만원';
  return '$sign${_int0.format(abs.round())}원';
}

/// 원화, 단위 없음 (테이블용). §8.5.2
///
/// 1만 미만에서만 [formatKRW] 와 갈린다 — 부호 있는 원본 값을 반올림 없이 그대로 찍는다.
String formatKRWCompact(double value) {
  final abs = value.abs();
  final sign = value < 0 ? '-' : '';
  if (abs >= _eok) {
    final eok = (abs / _eok).floor();
    final man = ((abs % _eok) / _man).round();
    if (man > 0) return '$sign$eok억 ${_int0.format(man)}만';
    return '$sign$eok억';
  }
  if (abs >= _man) return '$sign${_int0.format((abs / _man).round())}만';
  return _grp.format(value);
}

/// 통화별 금액, 단위 포함. §8.5.3
///
/// USDT 는 템플릿이 `$${값}` 이라 음수에서 기호 뒤에 마이너스가 온다(`$-12.35`). 웹 동작 그대로다.
String formatCurrency(double value, String baseCurrency) {
  if (baseCurrency == 'USDT') return '\$${_fix2.format(value)}';
  return formatKRW(value);
}

/// 통화별 금액, 단위 없음. §8.5.4
String formatCurrencyCompact(double value, String baseCurrency) {
  if (baseCurrency == 'USDT') return '\$${_fix2.format(value)}';
  return formatKRWCompact(value);
}

/// 법정통화 환산 표시. §8.5.5
String formatFiatEstimate(double value, String baseCurrency) =>
    '≈ ${formatCurrency(value, baseCurrency)}';

/// 코인 수량. 조건은 절댓값이 아니라 원본 값 기준이라 음수는 마지막 분기로 떨어진다. §8.5.6
String formatQuantity(double quantity) {
  if (quantity >= 1000000) return _int0.format(quantity);
  if (quantity >= 1000) return _fix2.format(quantity);
  if (quantity >= 1) return _fix4.format(quantity);
  return _f4to8.format(quantity);
}

/// 가격. **통화 기호를 붙이지 않는다** — 필요하면 호출부가 [getCurrencySymbol] 을 앞에 잇는다. §8.5.7
String formatPrice(double price, String baseCurrency) {
  if (baseCurrency == 'SOL') {
    if (price >= 1) return _fix4.format(price);
    if (price >= 0.0001) return _f4to8.format(price);
    return price.toStringAsExponential(2);
  }
  if (baseCurrency == 'USDT') {
    if (price >= 100) return _fix2.format(price);
    if (price >= 1) return _f2to4.format(price);
    return _fix4.format(price);
  }
  return _grp.format(price);
}

/// 거래대금. 억/만 축약을 하지 않는다. §8.5.8
String formatVolume(double volume, String baseCurrency) {
  if (baseCurrency == 'SOL') return '◎${_grp.format(volume)}';
  if (baseCurrency == 'USDT') return '\$${_grp.format(volume)}';
  return _grp.format(volume);
}

/// 변동률. 입력은 비율이다(0.0234 = 2.34%). 0 에는 `+` 가 붙지 않고, 천단위 구분자도 없다. §8.5.9
String formatChangeRate(double rate) {
  final percent = rate * 100;
  final sign = percent > 0 ? '+' : '';
  return '$sign${percent.toStringAsFixed(2)}%';
}

/// 통화 기호. USDT 는 빈 문자열이다. §8.5.10
String getCurrencySymbol(String baseCurrency) => switch (baseCurrency) {
  'KRW' => '₩',
  'SOL' => '◎',
  _ => '',
};

/// 소액 자산 필터 기준값. §8.5.11
const Map<String, double> smallAmountThresholds = {'KRW': 1000, 'USDT': 1};

double smallAmountThreshold(String baseCurrency) =>
    smallAmountThresholds[baseCurrency] ?? 1;
