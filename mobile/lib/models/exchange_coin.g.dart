// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'exchange_coin.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ExchangeCoin _$ExchangeCoinFromJson(Map<String, dynamic> json) => ExchangeCoin(
  exchangeCoinId: (json['exchangeCoinId'] as num).toInt(),
  coinId: (json['coinId'] as num).toInt(),
  coinSymbol: json['coinSymbol'] as String,
  coinName: json['coinName'] as String,
  price: (json['price'] as num).toDouble(),
  changeRate: (json['changeRate'] as num).toDouble(),
  volume: (json['volume'] as num).toDouble(),
);
