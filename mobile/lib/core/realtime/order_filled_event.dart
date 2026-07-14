import 'package:json_annotation/json_annotation.dart';

import '../json/converters.dart';

part 'order_filled_event.g.dart';

/// `/user/queue/events` — **구독하지 않는다**(사양서 R3).
///
/// 서버가 WebSocket 세션에 `Principal` 을 부착하지 않아 `convertAndSendToUser` 가 보낸
/// 메시지는 전부 폐기된다. 웹에서도 동작하지 않으므로 기능 동등성에 영향이 없다.
///
/// 체결 반영은 REST 재조회로 확정한다(주문 제출·취소 직후, 화면 진입, 포그라운드 복귀,
/// 당김 새로고침).
///
/// 서버가 (1) 핸드셰이크에서 `SESSION` 쿠키를 읽어 `Principal` 을 붙이고 (2) 페이로드에
/// `walletId`/`coinId`/`side`/`fee` 를 추가하면, 목적지 `/user/queue/events`(리터럴
/// `/user/{id}/...` 가 아니다) 구독 한 줄로 켠다. 필드는 서버가 **실제로 보내는 것**만 담는다.
@JsonSerializable(createToJson: false)
class OrderFilledEvent {
  const OrderFilledEvent({
    required this.eventType,
    required this.orderId,
    required this.executedPrice,
    required this.quantity,
    required this.executedAt,
  });

  factory OrderFilledEvent.fromJson(Map<String, dynamic> json) =>
      _$OrderFilledEventFromJson(json);

  /// 항상 `ORDER_FILLED`.
  final String eventType;
  final int orderId;
  final double executedPrice;
  final double quantity;

  @KstDateTimeConverter()
  final DateTime executedAt;
}
