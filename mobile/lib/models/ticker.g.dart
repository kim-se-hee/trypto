// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'ticker.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Ticker _$TickerFromJson(Map<String, dynamic> json) => Ticker(
  coinId: (json['coinId'] as num).toInt(),
  symbol: json['symbol'] as String,
  price: (json['price'] as num).toDouble(),
  changeRate: (json['changeRate'] as num).toDouble(),
  quoteTurnover: (json['quoteTurnover'] as num).toDouble(),
  timestamp: (json['timestamp'] as num).toInt(),
);
