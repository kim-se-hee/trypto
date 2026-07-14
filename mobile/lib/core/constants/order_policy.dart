/// 서버 `OrderAmountPolicy` 의 주문 금액 하한·상한(사양서 §4.4.6).
///
/// 웹은 최소 주문 문구를 `5,000 {통화}` 로 하드코딩해 바이낸스에서도 `5,000 USDT` 를 보여준다.
/// 실제 서버 기준은 5 USDT 이므로 여기서 바로잡는다.
class OrderPolicy {
  const OrderPolicy._();

  static const double krwMin = 5000;
  static const double krwMax = 1000000000;
  static const double usdtMin = 5;

  static double minAmount(String baseCurrency) =>
      baseCurrency == 'USDT' ? usdtMin : krwMin;

  /// USDT 는 상한이 없다.
  static double? maxAmount(String baseCurrency) =>
      baseCurrency == 'USDT' ? null : krwMax;
}
