import 'package:json_annotation/json_annotation.dart';

part 'exchange_coin.g.dart';

/// `GET /api/exchanges/{exchangeId}/coins` — 비인증. 거래소당 최대 600행이 온다.
///
/// 마켓 목록의 스냅샷이자 `exchangeCoinId` 해석표다(사양서 §1.10.2-3). 주문 API 는 `coinId` 가
/// 아니라 `exchangeCoinId` 를, 송금 API 는 `coinId` 를 받는다 — 둘을 혼동하지 않는다.
///
/// 입출금 화면은 여기 실린 [price] 를 현재가로 쓴다. 웹은 0 으로 하드코딩해 총자산·환산액이
/// 전부 무의미해졌다(사양서 R9).
@JsonSerializable(createToJson: false)
class ExchangeCoin {
  const ExchangeCoin({
    required this.exchangeCoinId,
    required this.coinId,
    required this.coinSymbol,
    required this.coinName,
    required this.price,
    required this.changeRate,
    required this.volume,
  });

  factory ExchangeCoin.fromJson(Map<String, dynamic> json) =>
      _$ExchangeCoinFromJson(json);

  final int exchangeCoinId;
  final int coinId;
  final String coinSymbol;
  final String coinName;

  final double price;

  /// 비율이다(0.0123 = 1.23%). 랭킹의 `profitRate` 와 단위가 다르다.
  final double changeRate;

  final double volume;
}
