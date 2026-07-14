import 'package:json_annotation/json_annotation.dart';

import '../core/json/converters.dart';
import 'enums.dart';

part 'order.g.dart';

/// `POST /api/orders`
///
/// **[volume]·[price] 의 의미가 주문 모드마다 다르다.** 어기면 400 이다.
///
/// | 모드 | volume | price |
/// |---|---|---|
/// | MARKET + BUY | 보내면 안 됨 | 필수 — **총 주문 금액** |
/// | MARKET + SELL | 필수 — 매도 수량 | 보내면 안 됨 |
/// | LIMIT + BUY/SELL | 필수 — 수량 | 필수 — 지정가 |
///
/// null 이면 키 자체를 뺀다(`includeIfNull: false`). 조립 규칙은 `order_form.dart` 가 강제한다.
@JsonSerializable(createFactory: false, includeIfNull: false)
class PlaceOrderRequest {
  const PlaceOrderRequest({
    required this.clientOrderId,
    required this.walletId,
    required this.exchangeCoinId,
    required this.side,
    required this.orderType,
    this.volume,
    this.price,
  });

  /// 멱등키. 서버는 임의 문자열을 받지만 UUID v4 로 통일한다. 재시도 시 같은 값을 재사용한다.
  final String clientOrderId;

  final int walletId;
  final int exchangeCoinId;
  final Side side;
  final OrderType orderType;
  final double? volume;
  final double? price;

  Map<String, dynamic> toJson() => _$PlaceOrderRequestToJson(this);
}

/// 시장가는 즉시 `FILLED`, 지정가는 `PENDING` 으로 등록된다.
/// `clientOrderId` 가 중복이면 서버가 기존 주문을 조회해 정상 201 로 응답한다.
@JsonSerializable(createToJson: false)
class PlaceOrderResponse {
  const PlaceOrderResponse({
    required this.orderId,
    required this.side,
    required this.orderType,
    required this.quantity,
    required this.status,
    required this.createdAt,
    this.orderAmount,
    this.price,
    this.filledPrice,
    this.fee,
    this.filledAt,
  });

  factory PlaceOrderResponse.fromJson(Map<String, dynamic> json) =>
      _$PlaceOrderResponseFromJson(json);

  final int orderId;

  @JsonKey(unknownEnumValue: Side.unknown)
  final Side side;

  @JsonKey(unknownEnumValue: OrderType.unknown)
  final OrderType orderType;

  final double quantity;

  /// 미체결 주문은 체결 금액·수수료·체결가가 없다.
  final double? orderAmount;
  final double? fee;
  final double? filledPrice;

  /// 지정가에만 있다.
  final double? price;

  @JsonKey(unknownEnumValue: OrderStatus.unknown)
  final OrderStatus status;

  @KstDateTimeConverter()
  final DateTime createdAt;

  @NullableKstDateTimeConverter()
  final DateTime? filledAt;
}

/// `GET /api/orders` 의 항목 — **`status` 필드가 없다**(사양서 R4-9).
/// 웹은 요청 필터값으로 덮어써서 있는 척했다. 화면에 상태가 필요하면 필터 탭의 값을 그대로 쓴다.
@JsonSerializable(createToJson: false)
class OrderHistoryItem {
  const OrderHistoryItem({
    required this.orderId,
    required this.exchangeCoinId,
    required this.side,
    required this.orderType,
    required this.quantity,
    required this.createdAt,
    this.filledPrice,
    this.price,
    this.orderAmount,
    this.fee,
    this.filledAt,
  });

  factory OrderHistoryItem.fromJson(Map<String, dynamic> json) =>
      _$OrderHistoryItemFromJson(json);

  final int orderId;
  final int exchangeCoinId;

  @JsonKey(unknownEnumValue: Side.unknown)
  final Side side;

  @JsonKey(unknownEnumValue: OrderType.unknown)
  final OrderType orderType;

  final double quantity;

  /// 미체결(PENDING) 주문은 체결가·체결금액·수수료가 전부 null 이다.
  final double? filledPrice;
  final double? orderAmount;
  final double? fee;

  /// 지정가에만 있다.
  final double? price;

  @KstDateTimeConverter()
  final DateTime createdAt;

  @NullableKstDateTimeConverter()
  final DateTime? filledAt;
}

/// `GET /api/orders/available`
///
/// [available] 의 의미가 방향마다 다르다. `BUY` 면 견적 통화(KRW/USDT) 가용 잔고,
/// `SELL` 이면 해당 코인의 가용 수량이다(잠금분 제외).
@JsonSerializable(createToJson: false)
class OrderAvailability {
  const OrderAvailability({required this.available, required this.currentPrice});

  factory OrderAvailability.fromJson(Map<String, dynamic> json) =>
      _$OrderAvailabilityFromJson(json);

  final double available;
  final double currentPrice;
}

/// `POST /api/orders/{orderId}/cancel`
@JsonSerializable(createFactory: false)
class CancelOrderRequest {
  const CancelOrderRequest({required this.walletId});

  final int walletId;

  Map<String, dynamic> toJson() => _$CancelOrderRequestToJson(this);
}

@JsonSerializable(createToJson: false)
class CancelOrderResponse {
  const CancelOrderResponse({required this.orderId, required this.status});

  factory CancelOrderResponse.fromJson(Map<String, dynamic> json) =>
      _$CancelOrderResponseFromJson(json);

  final int orderId;

  @JsonKey(unknownEnumValue: OrderStatus.unknown)
  final OrderStatus status;
}
