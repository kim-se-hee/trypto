import 'package:decimal/decimal.dart';

/// double → Decimal 의 유일한 승격 통로(계획서 §4.5.1).
///
/// DTO 필드는 전부 double 로 받고 연산 지점에서만 Decimal 로 올린다. 티커 hot path 에
/// BigInt 연산이 스며들지 않게 하기 위해서다.
extension DecimalX on double {
  /// `toString()` 을 거치는 이유: double 의 최단 왕복 표기라 원값이 그대로 복원된다.
  /// 코인 먼지 수량(`1e-8`)처럼 지수 표기로 나오는 값도 Decimal 이 그대로 파싱한다.
  Decimal get dec => Decimal.parse(toString());
}
