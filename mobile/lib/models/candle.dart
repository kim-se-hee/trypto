import 'package:json_annotation/json_annotation.dart';

import '../core/json/converters.dart';

part 'candle.g.dart';

/// `GET /api/candles` — 비인증.
///
/// **`time` 만 UTC Instant(`Z` 포함)** 이고 나머지 API 의 시각은 전부 오프셋 없는 서버 로컬시각이다
/// (사양서 R6). 웹의 `time ?? timestamp` 방어는 사문이다 — 서버는 `time` 만 내린다(R4-10).
///
/// OHLC 는 즉시 픽셀 좌표로 뭉개지므로 `double` 로 둔다.
@JsonSerializable(createToJson: false)
class Candle {
  const Candle({
    required this.time,
    required this.open,
    required this.high,
    required this.low,
    required this.close,
  });

  factory Candle.fromJson(Map<String, dynamic> json) => _$CandleFromJson(json);

  @InstantConverter()
  final DateTime time;

  final double open;
  final double high;
  final double low;
  final double close;
}
