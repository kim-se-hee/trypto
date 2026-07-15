import 'package:decimal/decimal.dart';

import '../../core/constants/exchanges.dart';
import '../../core/format/formatters.dart';
import '../../core/json/decimal_x.dart';
import '../../models/enums.dart';
import '../../models/order.dart';
import 'order_target.dart';

/// 수량 표시·연동 자릿수(사양서 §4.4.3). 서버는 8자리까지 받지만 웹과 같은 6자리로 맞춘다.
const int kQuantityScale = 6;

/// 가격 스텝 버튼의 증감폭(§4.4.3).
final Decimal kPriceStep = Decimal.fromInt(1000);

/// 비율 버튼(§4.4.3).
const List<int> kOrderRatios = [10, 25, 50, 100];

enum OrderField { price, quantity, total }

/// 폼이 계산에 쓰는 바깥 값. 거래소 상수(수수료율·최소 주문 금액) + 현재가 + 주문 가능 잔고.
class OrderContext {
  const OrderContext({
    required this.exchange,
    required this.currentPrice,
    required this.availableBuy,
    required this.availableSell,
  });

  final Exchange exchange;

  final Decimal currentPrice;

  /// `GET /api/orders/available?side=BUY` — 기준통화 가용 잔고.
  final Decimal availableBuy;

  /// `side=SELL` — 해당 코인의 가용 수량(잠금분 제외).
  final Decimal availableSell;

  String get baseCurrency => exchange.baseCurrency;

  /// KRW 는 정수, USDT 는 소수 2자리다. 웹은 통화와 무관하게 0자리로 뭉갠다.
  int get amountScale => baseCurrency == 'USDT' ? 2 : 0;

  Decimal get feeRate => exchange.feeRate.dec;

  Decimal get minOrderAmount => exchange.minOrderAmount.dec;

  Decimal? get maxOrderAmount => exchange.maxOrderAmount?.dec;

  /// 매수 차감액은 `총액 × (1 + feeRate)` 다(사양서 §4.4.5). 수수료까지 내고 살 수 있는
  /// 최대 총액이며, 100% 비율 버튼의 상한이다.
  Decimal get maxSpend => (availableBuy / (Decimal.one + feeRate)).toDecimal(
    scaleOnInfinitePrecision: amountScale + 8,
  );

  String amountLabel(Decimal value) =>
      '${formatPrice(value.toDouble(), baseCurrency)} $baseCurrency';
}

/// 주문 입력의 순수 상태. 위젯 없이 테스트한다.
///
/// **`volume`·`price` 의 의미가 주문 모드마다 다르다**(사양서 §4.4.7). 시장가 매수는 `volume` 을
/// 보내면 거부되고 `price` 에 총액이 실리며, 시장가 매도는 `price` 를 보내면 거부된다. 조립
/// 규칙을 [toRequest] 한 곳에 가둔다.
class OrderForm {
  const OrderForm({
    required this.side,
    required this.orderType,
    required this.price,
    required this.quantity,
    required this.total,
    required this.lastEdited,
  });

  factory OrderForm.empty({
    required Side side,
    OrderType orderType = OrderType.limit,
  }) => OrderForm(
    side: side,
    orderType: orderType,
    price: Decimal.zero,
    quantity: Decimal.zero,
    total: Decimal.zero,
    lastEdited: OrderField.quantity,
  );

  final Side side;
  final OrderType orderType;

  /// 지정가. 시장가에서는 화면에 현재가가 고정 표시되고 이 값은 쓰이지 않는다.
  final Decimal price;

  final Decimal quantity;
  final Decimal total;

  /// 가격이 바뀌면 마지막으로 손댄 쪽을 기준으로 반대편을 다시 계산한다(§4.4.3).
  final OrderField lastEdited;

  bool get isLimit => orderType == OrderType.limit;

  bool get isMarketBuy => orderType == OrderType.market && side == Side.buy;

  bool get isMarketSell => orderType == OrderType.market && side == Side.sell;

  /// 시장가는 가격 칸이 현재가로 고정되고 비활성이다(§4.4.2).
  bool get priceEditable => isLimit;

  /// 시장가 매수는 "얼마어치"(총액)로, 시장가 매도는 "몇 개"(수량)로 주문한다.
  bool get showsQuantity => !isMarketBuy;

  bool get showsTotal => !isMarketSell;

  /// 수량↔총액 연동은 지정가에서만 한다(§4.4.3).
  bool get linked => isLimit;

  String get successMessage =>
      isLimit ? '주문이 등록되었습니다.' : '주문이 체결되었습니다.';

  Decimal displayPrice(Decimal currentPrice) =>
      isLimit && price > Decimal.zero ? price : currentPrice;

  OrderForm copyWith({
    Side? side,
    OrderType? orderType,
    Decimal? price,
    Decimal? quantity,
    Decimal? total,
    OrderField? lastEdited,
  }) => OrderForm(
    side: side ?? this.side,
    orderType: orderType ?? this.orderType,
    price: price ?? this.price,
    quantity: quantity ?? this.quantity,
    total: total ?? this.total,
    lastEdited: lastEdited ?? this.lastEdited,
  );

  /// 탭을 바꾸면 세 입력을 비운다(§4.4.3).
  OrderForm withSide(Side side) =>
      OrderForm.empty(side: side, orderType: orderType);

  OrderForm withOrderType(OrderType orderType, OrderContext ctx) =>
      copyWith(orderType: orderType)._relink(ctx);

  OrderForm withPrice(Decimal price, OrderContext ctx) =>
      copyWith(price: price)._relink(ctx);

  OrderForm withQuantity(Decimal quantity, OrderContext ctx) {
    final next = copyWith(quantity: quantity, lastEdited: OrderField.quantity);
    if (!next.linked) return next;
    return next.copyWith(
      total: _amountOf(quantity, next.displayPrice(ctx.currentPrice), ctx),
    );
  }

  OrderForm withTotal(Decimal total, OrderContext ctx) {
    final next = copyWith(total: total, lastEdited: OrderField.total);
    if (!next.linked) return next;
    return next.copyWith(
      quantity: _quantityOf(total, next.displayPrice(ctx.currentPrice)),
    );
  }

  /// 기준값은 현재 입력값이며, 비어 있으면 현재가다. 0 미만으로 내려가지 않는다(§4.4.3).
  OrderForm stepPrice(int direction, OrderContext ctx) {
    final base = price > Decimal.zero ? price : ctx.currentPrice;
    final next = base + kPriceStep * Decimal.fromInt(direction);
    return withPrice(next < Decimal.zero ? Decimal.zero : next, ctx);
  }

  /// 매수는 총액을, 매도는 수량을 채운다(§4.4.3).
  ///
  /// 매수 100% 에서 잔고를 그대로 채우면 수수료가 총액 **위에** 더 붙으므로 서버가 반드시
  /// `INSUFFICIENT_BALANCE` 를 낸다(웹의 결함). 살 수 있는 최대치로 가둔다 — 10/25/50% 에서는
  /// 이 상한에 닿지 않으므로 식은 웹과 같다.
  OrderForm applyRatio(int percent, OrderContext ctx) {
    final ratio = (Decimal.fromInt(percent) / Decimal.fromInt(100)).toDecimal();

    if (side == Side.buy) {
      final raw = ctx.availableBuy * ratio;
      final capped = raw < ctx.maxSpend ? raw : ctx.maxSpend;
      return withTotal(capped.floor(scale: ctx.amountScale), ctx);
    }
    final raw = ctx.availableSell * ratio;
    return withQuantity(raw.floor(scale: kQuantityScale), ctx);
  }

  /// 서버가 정산 기준으로 삼는 주문 금액. 수수료·최소 주문 검증과 예상 수수료가 이 값을 쓴다.
  Decimal amountOf(OrderContext ctx) {
    if (isMarketBuy) return total;
    return _amountOf(quantity, displayPrice(ctx.currentPrice), ctx);
  }

  /// 표시용 예상 수수료. 실제 수수료는 서버가 체결가로 계산한다(§4.4.5).
  Decimal feeOf(OrderContext ctx) =>
      (amountOf(ctx) * ctx.feeRate).round(scale: ctx.amountScale);

  /// 첫 실패 사유. null 이면 제출할 수 있다.
  ///
  /// 1~3 은 웹의 클라이언트 검증(§4.4.6)이고, 4~5 는 서버 `OrderAmountPolicy`·잔고 검사를
  /// 입력 단계로 당긴 것이다 — 웹은 여기서 400 을 받고 나서야 사용자에게 알린다.
  String? validate(OrderContext ctx) {
    if (isMarketBuy && total <= Decimal.zero) return '주문 총액을 입력해 주세요.';
    if (!isMarketBuy && quantity <= Decimal.zero) return '주문 수량을 입력해 주세요.';
    if (isLimit && price <= Decimal.zero) return '지정가를 입력해 주세요.';

    final amount = amountOf(ctx);
    if (amount < ctx.minOrderAmount) {
      return '최소 주문 금액은 ${ctx.amountLabel(ctx.minOrderAmount)} 입니다.';
    }
    final max = ctx.maxOrderAmount;
    if (max != null && amount > max) {
      return '최대 주문 금액은 ${ctx.amountLabel(max)} 입니다.';
    }

    if (side == Side.buy) {
      // 반올림하지 않고 정확히 비교한다. 100% 비율 버튼이 만든 총액이 이 검사에 걸리면 안 된다.
      final required = amount * (Decimal.one + ctx.feeRate);
      if (required > ctx.availableBuy) {
        return '주문 가능 금액을 초과했습니다. '
            '수수료를 포함해 ${ctx.amountLabel(required.round(scale: ctx.amountScale))} 가 필요합니다.';
      }
      return null;
    }
    if (quantity > ctx.availableSell) return '보유 수량을 초과했습니다.';
    return null;
  }

  /// `POST /api/orders` 바디. 모드별 필드 규칙을 어기면 서버가 400 이다.
  PlaceOrderRequest toRequest({
    required OrderTarget target,
    required String clientOrderId,
  }) => PlaceOrderRequest(
    clientOrderId: clientOrderId,
    walletId: target.walletId,
    exchangeCoinId: target.exchangeCoinId,
    side: side,
    orderType: orderType,
    // 시장가 매수는 volume 을 보내면 거부된다(OrderMode.rejectVolume).
    volume: isMarketBuy ? null : quantity.toDouble(),
    // 시장가 매도는 price 를 보내면 거부된다. 시장가 매수의 price 는 가격이 아니라 **총액**이다.
    price: isMarketSell ? null : (isMarketBuy ? total : price).toDouble(),
  );

  OrderForm _relink(OrderContext ctx) {
    if (!linked) return this;
    final priceOf = displayPrice(ctx.currentPrice);
    return lastEdited == OrderField.total
        ? copyWith(quantity: _quantityOf(total, priceOf))
        : copyWith(total: _amountOf(quantity, priceOf, ctx));
  }

  static Decimal _amountOf(Decimal quantity, Decimal price, OrderContext ctx) =>
      (quantity * price).round(scale: ctx.amountScale);

  static Decimal _quantityOf(Decimal total, Decimal price) {
    if (price <= Decimal.zero) return Decimal.zero;
    return (total / price)
        .toDecimal(scaleOnInfinitePrecision: kQuantityScale + 2)
        .round(scale: kQuantityScale);
  }
}

/// 콤마를 모두 제거한 뒤 숫자로 바꾸고, 숫자가 아니면 0 으로 본다(§4.4.3).
Decimal parseAmountInput(String text) {
  final cleaned = text.replaceAll(',', '').trim();
  if (cleaned.isEmpty) return Decimal.zero;
  final value = Decimal.tryParse(cleaned);
  if (value == null || value < Decimal.zero) return Decimal.zero;
  return value;
}
