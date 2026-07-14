import 'order_policy.dart';

/// 거래소 상수의 단일 출처(사양서 R7).
///
/// 거래소 목록 조회 API 가 없다. `id` 는 서버 `seed-data.sql` 의 삽입 순서로 정해진 값이며
/// REST 경로(`/api/exchanges/{id}/coins`), STOMP 토픽(`/topic/tickers.{id}`),
/// 지갑 매핑(`wallet.exchangeId`), 긴급 자금 요청이 전부 이 값을 그대로 쓴다.
class Exchange {
  const Exchange({
    required this.id,
    required this.key,
    required this.name,
    required this.baseCurrency,
    required this.feeRate,
    required this.candleCode,
  });

  final int id;

  /// 라우트 쿼리 `?exchange=upbit` 에 실리는 값.
  final String key;

  final String name;
  final String baseCurrency;

  /// 서버 `exchange_market.fee_rate`. 표시용 예상 수수료는 `주문총액 × feeRate` 다.
  final double feeRate;

  /// 캔들 API 만 id 가 아니라 대문자 코드를 받는다.
  final String candleCode;

  double get minOrderAmount => OrderPolicy.minAmount(baseCurrency);

  double? get maxOrderAmount => OrderPolicy.maxAmount(baseCurrency);
}

class ExchangeIds {
  const ExchangeIds._();

  static const int upbit = 1;
  static const int bithumb = 2;
  static const int binance = 3;

  static const List<Exchange> all = [
    Exchange(
      id: upbit,
      key: 'upbit',
      name: '업비트',
      baseCurrency: 'KRW',
      feeRate: 0.0005,
      candleCode: 'UPBIT',
    ),
    Exchange(
      id: bithumb,
      key: 'bithumb',
      name: '빗썸',
      baseCurrency: 'KRW',
      feeRate: 0.0025,
      candleCode: 'BITHUMB',
    ),
    Exchange(
      id: binance,
      key: 'binance',
      name: '바이낸스',
      baseCurrency: 'USDT',
      feeRate: 0.001,
      candleCode: 'BINANCE',
    ),
  ];

  /// 서버가 준 `exchangeId` 는 이 표에 없을 수 있다(거래소가 추가되는 경우).
  /// 표시 계층이 직접 처리하도록 null 을 돌려준다.
  static Exchange? byId(int id) {
    for (final exchange in all) {
      if (exchange.id == id) return exchange;
    }
    return null;
  }

  /// 라우트 쿼리에서 오는 값이라 무엇이든 들어올 수 있다. 알 수 없으면 첫 거래소로 떨어진다.
  static Exchange byKey(String? key) {
    for (final exchange in all) {
      if (exchange.key == key) return exchange;
    }
    return all.first;
  }
}
