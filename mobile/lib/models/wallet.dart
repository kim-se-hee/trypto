import 'package:json_annotation/json_annotation.dart';

part 'wallet.g.dart';

/// `GET /api/wallets/{walletId}/balances`
///
/// 코인 잔고에는 심볼·이름·현재가가 없다. 거래소 코인 목록(`coinId` 기준)과 합쳐야 화면이 된다.
@JsonSerializable(createToJson: false)
class WalletBalances {
  const WalletBalances({
    required this.exchangeId,
    required this.baseCurrencySymbol,
    required this.baseCurrencyAvailable,
    required this.baseCurrencyLocked,
    required this.balances,
  });

  factory WalletBalances.fromJson(Map<String, dynamic> json) =>
      _$WalletBalancesFromJson(json);

  final int exchangeId;

  /// `KRW` 또는 `USDT`.
  final String baseCurrencySymbol;

  final double baseCurrencyAvailable;

  /// 미체결 주문에 잠긴 금액.
  final double baseCurrencyLocked;

  final List<CoinBalance> balances;
}

@JsonSerializable(createToJson: false)
class CoinBalance {
  const CoinBalance({
    required this.coinId,
    required this.available,
    required this.locked,
  });

  factory CoinBalance.fromJson(Map<String, dynamic> json) =>
      _$CoinBalanceFromJson(json);

  final int coinId;
  final double available;
  final double locked;
}
