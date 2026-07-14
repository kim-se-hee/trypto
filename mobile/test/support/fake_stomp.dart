import 'package:flutter/foundation.dart';
import 'package:trypto/core/realtime/stomp_service.dart';

/// 실제 소켓을 열지 않는다. 구독 목적지를 노출하고 프레임을 손으로 밀어 넣는다.
///
/// 위젯 테스트에서 진짜 [StompService] 를 쓰면 WS 연결에 실패하고 백오프 타이머가 남아
/// `A Timer is still pending` 으로 죽는다.
class FakeStompService implements StompService {
  final Map<String, StompFrameHandler> _handlers = {};

  List<String> get destinations => _handlers.keys.toList();

  void emit(String destination, String body) =>
      _handlers[destination]?.call(body);

  @override
  bool get connected => true;

  @override
  void connect() {}

  @override
  VoidCallback subscribe(String destination, StompFrameHandler handler) {
    _handlers[destination] = handler;
    return () => _handlers.remove(destination);
  }

  @override
  void forceReconnect() {}

  @override
  void dispose() => _handlers.clear();
}
