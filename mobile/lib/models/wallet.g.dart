// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'wallet.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

WalletBalances _$WalletBalancesFromJson(Map<String, dynamic> json) =>
    WalletBalances(
      exchangeId: (json['exchangeId'] as num).toInt(),
      baseCurrencySymbol: json['baseCurrencySymbol'] as String,
      baseCurrencyAvailable: (json['baseCurrencyAvailable'] as num).toDouble(),
      baseCurrencyLocked: (json['baseCurrencyLocked'] as num).toDouble(),
      balances: (json['balances'] as List<dynamic>)
          .map((e) => CoinBalance.fromJson(e as Map<String, dynamic>))
          .toList(),
    );

CoinBalance _$CoinBalanceFromJson(Map<String, dynamic> json) => CoinBalance(
  coinId: (json['coinId'] as num).toInt(),
  available: (json['available'] as num).toDouble(),
  locked: (json['locked'] as num).toDouble(),
);
