import '../../core/constants/exchanges.dart';
import 'market_controller.dart';

/// 주문 대상 해석 실패 3종(사양서 §1.10.2, §4.4.1). 문구는 웹과 같다.
enum OrderTargetFailure {
  noRound('진행 중인 라운드가 없어 주문할 수 없습니다.'),
  coinUnlisted('이 코인은 아직 주문을 지원하지 않습니다.'),
  lookupFailed('주문 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');

  const OrderTargetFailure(this.message);

  final String message;
}

/// 주문·잔고 조회에 필요한 세 ID 의 묶음.
///
/// [key] 로 어느 (거래소, 코인) 의 것인지 함께 들고 다닌다 — 다른 코인으로 옮긴 직후 이전
/// 코인의 주문 대상이 잘못 쓰이는 일을 막는다(사양서 §4.4.1).
class OrderTarget {
  const OrderTarget({
    required this.exchange,
    required this.symbol,
    required this.walletId,
    required this.exchangeCoinId,
  });

  final Exchange exchange;
  final String symbol;
  final int walletId;

  /// 주문 API 는 `coinId` 가 아니라 `exchangeCoinId` 를 받는다. 송금 API 는 `coinId` 를 받는다.
  final int exchangeCoinId;

  int get exchangeId => exchange.id;

  String get key => '${exchange.key}:$symbol';

  @override
  bool operator ==(Object other) =>
      other is OrderTarget &&
      other.key == key &&
      other.walletId == walletId &&
      other.exchangeCoinId == exchangeCoinId;

  @override
  int get hashCode => Object.hash(key, walletId, exchangeCoinId);
}

class OrderTargetResult {
  const OrderTargetResult.resolved(OrderTarget this.target) : failure = null;

  const OrderTargetResult.failed(OrderTargetFailure this.failure)
    : target = null;

  final OrderTarget? target;
  final OrderTargetFailure? failure;
}

/// `exchangeId`(상수) → `walletId`(활성 라운드) → `exchangeCoinId`(코인 목록 캐시) 순서로
/// 유도한다(사양서 §1.10.2). [coins] 가 null 이면 목록 조회 자체가 실패한 것이다.
///
/// 심볼 비교는 **대소문자를 무시한다**.
OrderTargetResult resolveOrderTarget({
  required Exchange exchange,
  required String symbol,
  required int? walletId,
  required List<CoinEntry>? coins,
}) {
  if (walletId == null) {
    return const OrderTargetResult.failed(OrderTargetFailure.noRound);
  }
  if (coins == null) {
    return const OrderTargetResult.failed(OrderTargetFailure.lookupFailed);
  }

  final target = symbol.toUpperCase();
  for (final entry in coins) {
    if (entry.symbol.toUpperCase() != target) continue;
    return OrderTargetResult.resolved(
      OrderTarget(
        exchange: exchange,
        symbol: entry.symbol,
        walletId: walletId,
        exchangeCoinId: entry.coin.exchangeCoinId,
      ),
    );
  }
  return const OrderTargetResult.failed(OrderTargetFailure.coinUnlisted);
}
