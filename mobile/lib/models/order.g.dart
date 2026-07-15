// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'order.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Map<String, dynamic> _$PlaceOrderRequestToJson(PlaceOrderRequest instance) =>
    <String, dynamic>{
      'clientOrderId': instance.clientOrderId,
      'walletId': instance.walletId,
      'exchangeCoinId': instance.exchangeCoinId,
      'side': _$SideEnumMap[instance.side]!,
      'orderType': _$OrderTypeEnumMap[instance.orderType]!,
      'volume': ?instance.volume,
      'price': ?instance.price,
    };

const _$SideEnumMap = {
  Side.buy: 'BUY',
  Side.sell: 'SELL',
  Side.unknown: 'UNKNOWN',
};

const _$OrderTypeEnumMap = {
  OrderType.market: 'MARKET',
  OrderType.limit: 'LIMIT',
  OrderType.unknown: 'UNKNOWN',
};

PlaceOrderResponse _$PlaceOrderResponseFromJson(
  Map<String, dynamic> json,
) => PlaceOrderResponse(
  orderId: (json['orderId'] as num).toInt(),
  side: $enumDecode(_$SideEnumMap, json['side'], unknownValue: Side.unknown),
  orderType: $enumDecode(
    _$OrderTypeEnumMap,
    json['orderType'],
    unknownValue: OrderType.unknown,
  ),
  quantity: (json['quantity'] as num).toDouble(),
  status: $enumDecode(
    _$OrderStatusEnumMap,
    json['status'],
    unknownValue: OrderStatus.unknown,
  ),
  createdAt: const KstDateTimeConverter().fromJson(json['createdAt'] as String),
  orderAmount: (json['orderAmount'] as num?)?.toDouble(),
  price: (json['price'] as num?)?.toDouble(),
  filledPrice: (json['filledPrice'] as num?)?.toDouble(),
  fee: (json['fee'] as num?)?.toDouble(),
  filledAt: const NullableKstDateTimeConverter().fromJson(
    json['filledAt'] as String?,
  ),
);

const _$OrderStatusEnumMap = {
  OrderStatus.filled: 'FILLED',
  OrderStatus.pending: 'PENDING',
  OrderStatus.canceled: 'CANCELED',
  OrderStatus.failed: 'FAILED',
  OrderStatus.unknown: 'UNKNOWN',
};

OrderHistoryItem _$OrderHistoryItemFromJson(
  Map<String, dynamic> json,
) => OrderHistoryItem(
  orderId: (json['orderId'] as num).toInt(),
  exchangeCoinId: (json['exchangeCoinId'] as num).toInt(),
  side: $enumDecode(_$SideEnumMap, json['side'], unknownValue: Side.unknown),
  orderType: $enumDecode(
    _$OrderTypeEnumMap,
    json['orderType'],
    unknownValue: OrderType.unknown,
  ),
  quantity: (json['quantity'] as num).toDouble(),
  createdAt: const KstDateTimeConverter().fromJson(json['createdAt'] as String),
  filledPrice: (json['filledPrice'] as num?)?.toDouble(),
  price: (json['price'] as num?)?.toDouble(),
  orderAmount: (json['orderAmount'] as num?)?.toDouble(),
  fee: (json['fee'] as num?)?.toDouble(),
  filledAt: const NullableKstDateTimeConverter().fromJson(
    json['filledAt'] as String?,
  ),
);

OrderAvailability _$OrderAvailabilityFromJson(Map<String, dynamic> json) =>
    OrderAvailability(
      available: (json['available'] as num).toDouble(),
      currentPrice: (json['currentPrice'] as num).toDouble(),
    );

Map<String, dynamic> _$CancelOrderRequestToJson(CancelOrderRequest instance) =>
    <String, dynamic>{'walletId': instance.walletId};

CancelOrderResponse _$CancelOrderResponseFromJson(Map<String, dynamic> json) =>
    CancelOrderResponse(
      orderId: (json['orderId'] as num).toInt(),
      status: $enumDecode(
        _$OrderStatusEnumMap,
        json['status'],
        unknownValue: OrderStatus.unknown,
      ),
    );
