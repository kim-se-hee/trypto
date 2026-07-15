import 'package:json_annotation/json_annotation.dart';

part 'ticker.g.dart';

/// `/topic/tickers.{exchangeId}` 로 오는 실시간 시세(사양서 §3.2.1). REST 응답이 아니다.
///
/// 초당 수백 건이 배열로 도착한다. 필드를 `double` 로 두는 것은 타협이 아니라 결정이다 —
/// 이 경로에 `Decimal`(BigInt 연산)을 올리면 프레임이 무너진다(계획서 §4.5.1).
///
/// 완전성이 보장되지 않는 **스냅샷 스트림**이다. 서버가 큐 포화 시 오래된 메시지를 버린다.
/// 델타를 누산하지 말고 항상 최신 값으로 덮어쓴다.
@JsonSerializable(createToJson: false)
class Ticker {
  const Ticker({
    required this.coinId,
    required this.symbol,
    required this.price,
    required this.changeRate,
    required this.quoteTurnover,
    required this.timestamp,
  });

  factory Ticker.fromJson(Map<String, dynamic> json) => _$TickerFromJson(json);

  final int coinId;
  final String symbol;
  final double price;
  final double changeRate;

  /// 거래대금. 마켓 목록의 '거래대금' 열이며 코인 목록의 `volume` 자리에 들어간다.
  final double quoteTurnover;

  /// epoch millis. 같은 값이면 같은 틱이다(플래시 판정에 쓴다).
  final int timestamp;
}
