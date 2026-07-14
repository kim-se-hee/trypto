// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'order_filled_event.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

OrderFilledEvent _$OrderFilledEventFromJson(Map<String, dynamic> json) =>
    OrderFilledEvent(
      eventType: json['eventType'] as String,
      orderId: (json['orderId'] as num).toInt(),
      executedPrice: (json['executedPrice'] as num).toDouble(),
      quantity: (json['quantity'] as num).toDouble(),
      executedAt: const KstDateTimeConverter().fromJson(
        json['executedAt'] as String,
      ),
    );
