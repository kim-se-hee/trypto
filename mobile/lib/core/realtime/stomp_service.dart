import 'dart:async';
import 'dart:math' as math;

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../env.dart';

typedef StompFrameHandler = void Function(String body);

class _Registration {
  _Registration(this.destination, this.handler);

  final String destination;
  final StompFrameHandler handler;

  /// 소켓이 끊기면 무효화한다. 재연결 후 중복 구독이 생기지 않게 한다.
  StompUnsubscribe? cancel;
}

/// 앱 전역 싱글톤(계획서 §4.2.1). raw WS + STOMP 1.2 이며 SockJS 가 아니다.
///
/// **수신 전용**이다 — 서버에 `@MessageMapping` 이 하나도 없다. 쓰기는 전부 REST 다.
class StompService {
  StompService({
    String? url,
    Stream<List<ConnectivityResult>>? connectivity,
    bool observeLifecycle = true,
  }) : _url = url ?? Env.wsBaseUrl {
    if (observeLifecycle) {
      _lifecycle = AppLifecycleListener(onStateChange: _onLifecycle);
    }
    _network = (connectivity ?? Connectivity().onConnectivityChanged).listen(
      _onNetworkChanged,
    );
    _client = _build();
  }

  final String _url;
  final List<_Registration> _registry = [];

  late StompClient _client;
  AppLifecycleListener? _lifecycle;
  late final StreamSubscription<List<ConnectivityResult>> _network;

  Timer? _retry;
  Timer? _networkDebounce;
  DateTime? _hiddenAt;
  int _attempts = 0;
  bool _intentionalClose = false;
  bool _started = false;
  bool _disposed = false;

  bool get connected => _client.connected;

  void connect() {
    if (_started || _disposed) return;
    _started = true;
    _client.activate();
  }

  /// 아직 연결되지 않았으면 레지스트리에만 넣어 둔다. `onConnect` 가 전량 재구독한다.
  VoidCallback subscribe(String destination, StompFrameHandler handler) {
    connect();
    final registration = _Registration(destination, handler);
    _registry.add(registration);
    _activate(registration);

    return () {
      _registry.remove(registration);
      registration.cancel?.call();
      registration.cancel = null;
    };
  }

  /// 백오프 타이머를 취소하고 소켓을 즉시 버린다. `deactivate()` 의 정상 종료 절차는 좀비
  /// 소켓에서 오래 걸리므로 기다리지 않는다.
  void forceReconnect() {
    if (!_started || _disposed) return;
    _closeSocket();
    _client = _build();
    _attempts = 0;
    _client.activate();
  }

  void dispose() {
    if (_disposed) return;
    _disposed = true;
    _retry?.cancel();
    _networkDebounce?.cancel();
    _network.cancel();
    _lifecycle?.dispose();
    _registry.clear();
    _intentionalClose = true;
    _client.deactivate();
  }

  StompClient _build() {
    late StompClient client;
    client = StompClient(
      config: StompConfig(
        url: _url,
        heartbeatIncoming: const Duration(seconds: 10),
        // 서버 수신 기준은 10초다. 모바일은 OS 타이머 유예로 주기가 밀리므로 2초 마진을 둔다.
        heartbeatOutgoing: const Duration(seconds: 8),
        connectionTimeout: const Duration(seconds: 5),
        // 내장 재연결은 고정 지연만 지원한다. 지수 백오프는 직접 건다.
        reconnectDelay: Duration.zero,
        onConnect: (_) => _onConnect(client),
        onWebSocketDone: () => _onWebSocketDone(client),
        onWebSocketError: (error) => debugPrint('[STOMP] socket error: $error'),
        onStompError: (frame) =>
            debugPrint('[STOMP] error frame: ${frame.headers['message']}'),
      ),
    );
    return client;
  }

  void _onConnect(StompClient client) {
    if (!identical(client, _client)) return;
    _attempts = 0;
    // 자동 복원되지 않는다. 레지스트리를 전량 재구독한다.
    for (final registration in _registry) {
      _activate(registration);
    }
  }

  void _onWebSocketDone(StompClient client) {
    // 폐기된 클라이언트가 뒤늦게 닫히는 경우가 있다. 그 done 으로 백오프를 무장하면 이중
    // 연결이 생긴다.
    if (!identical(client, _client) || _disposed) return;
    _invalidate();
    if (_intentionalClose) {
      _intentionalClose = false;
      return;
    }
    _attempts++;
    // 첫 대기는 2초다(웹 구현과 동일).
    final ms = math.min(1000 * math.pow(2, _attempts).toInt(), 30000);
    _retry?.cancel();
    _retry = Timer(Duration(milliseconds: ms), () {
      if (!_disposed) _client.activate();
    });
  }

  void _activate(_Registration registration) {
    if (!_client.connected) return;
    registration.cancel = _client.subscribe(
      destination: registration.destination,
      callback: (frame) {
        final body = frame.body;
        if (body != null) registration.handler(body);
      },
    );
  }

  void _invalidate() {
    for (final registration in _registry) {
      registration.cancel = null;
    }
  }

  /// 백오프를 무장하지 않는 종료. `_intentionalClose` 는 동기 done 을, 클라이언트 동일성
  /// 검사는 비동기 done 을 각각 막는다.
  void _closeSocket() {
    _retry?.cancel();
    _intentionalClose = true;
    _client.deactivate();
    _invalidate();
    _intentionalClose = false;
  }

  void _onLifecycle(AppLifecycleState state) {
    if (_disposed) return;

    // paused 만 본다. iOS 는 알림 센터를 살짝 내려도 inactive/hidden 이 뜬다.
    if (state == AppLifecycleState.paused) {
      _hiddenAt = DateTime.now();
      if (_started) _closeSocket();
      return;
    }
    if (state != AppLifecycleState.resumed) return;

    final hiddenAt = _hiddenAt;
    _hiddenAt = null;
    if (!_started) return;

    final elapsed = hiddenAt == null
        ? Duration.zero
        : DateTime.now().difference(hiddenAt);
    // 하트비트 10초의 2배를 넘겼으면 서버가 이미 세션을 끊었다고 본다. connected 만 보면
    // ERROR 프레임 처리 전이라 좀비 연결을 잡지 못한다.
    if (elapsed > const Duration(seconds: 20) || !connected) forceReconnect();
  }

  /// Wi-Fi ↔ 셀룰러 전환 직후 소켓은 살아 보이지만 죽어 있다. resume 직후에도 이벤트가
  /// 튀므로 디바운스가 없으면 재연결이 중복 발화한다.
  void _onNetworkChanged(List<ConnectivityResult> results) {
    if (_disposed || !_started) return;
    if (results.every((result) => result == ConnectivityResult.none)) return;
    _networkDebounce?.cancel();
    _networkDebounce = Timer(const Duration(seconds: 1), forceReconnect);
  }
}

final stompServiceProvider = Provider<StompService>((ref) {
  final service = StompService();
  ref.onDispose(service.dispose);
  return service;
});
