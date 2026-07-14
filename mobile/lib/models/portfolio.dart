import 'package:json_annotation/json_annotation.dart';

part 'portfolio.g.dart';

/// `GET /api/wallets/{walletId}/portfolio`
@JsonSerializable(createToJson: false)
class MyHoldings {
  const MyHoldings({
    required this.exchangeId,
    required this.baseCurrencyBalance,
    required this.baseCurrencySymbol,
    required this.holdings,
  });

  factory MyHoldings.fromJson(Map<String, dynamic> json) =>
      _$MyHoldingsFromJson(json);

  final int exchangeId;

  /// 현금 잔고. 보유 코인이 0개여도 이 값은 있다(도넛이 현금 100%로 그려진다).
  final double baseCurrencyBalance;

  final String baseCurrencySymbol;
  final List<HoldingSnapshot> holdings;
}

/// 평가금액·수익률은 서버가 주지 않는다. `quantity`·`avgBuyPrice`·`currentPrice` 로
/// 클라이언트가 계산하며, 그 연산은 `Decimal` 로 승격해서 한다(계획서 §4.5.1).
@JsonSerializable(createToJson: false)
class HoldingSnapshot {
  const HoldingSnapshot({
    required this.coinId,
    required this.coinSymbol,
    required this.coinName,
    required this.quantity,
    required this.avgBuyPrice,
    required this.currentPrice,
  });

  factory HoldingSnapshot.fromJson(Map<String, dynamic> json) =>
      _$HoldingSnapshotFromJson(json);

  final int coinId;
  final String coinSymbol;
  final String coinName;
  final double quantity;
  final double avgBuyPrice;
  final double currentPrice;
}
