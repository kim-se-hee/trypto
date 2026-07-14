// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'portfolio.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

MyHoldings _$MyHoldingsFromJson(Map<String, dynamic> json) => MyHoldings(
  exchangeId: (json['exchangeId'] as num).toInt(),
  baseCurrencyBalance: (json['baseCurrencyBalance'] as num).toDouble(),
  baseCurrencySymbol: json['baseCurrencySymbol'] as String,
  holdings: (json['holdings'] as List<dynamic>)
      .map((e) => HoldingSnapshot.fromJson(e as Map<String, dynamic>))
      .toList(),
);

HoldingSnapshot _$HoldingSnapshotFromJson(Map<String, dynamic> json) =>
    HoldingSnapshot(
      coinId: (json['coinId'] as num).toInt(),
      coinSymbol: json['coinSymbol'] as String,
      coinName: json['coinName'] as String,
      quantity: (json['quantity'] as num).toDouble(),
      avgBuyPrice: (json['avgBuyPrice'] as num).toDouble(),
      currentPrice: (json['currentPrice'] as num).toDouble(),
    );
