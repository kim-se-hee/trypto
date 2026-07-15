import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// 세션 ID 보관소(사양서 R2).
///
/// 쿠키 jar 를 쓰지 않는 이유: 서버의 Redis TTL 은 요청마다 7일로 갱신되는 슬라이딩 방식이지만
/// 쿠키의 `Max-Age` 는 로그인 시점에만 발급된다. 쿠키 만료를 그대로 흉내 내면 매일 앱을 써도
/// 로그인 7일째에 강제 로그아웃된다. 그래서 값만 보관하고 만료 시각은 저장하지 않는다.
class SessionStore {
  SessionStore({FlutterSecureStorage? storage})
    : _storage =
          storage ??
          const FlutterSecureStorage(
            aOptions: AndroidOptions(encryptedSharedPreferences: true),
          );

  static const String _key = 'session_id';

  final FlutterSecureStorage _storage;

  /// 동기 캐시. `onRequest` 는 절대 await 하지 않는다 — 요청마다 플랫폼 채널을 왕복하면
  /// 그 지연이 프레임에 얹힌다.
  String? _cached;

  String? get sessionId => _cached;

  bool get hasSession => _cached != null;

  /// runApp() 이전에 1회 호출한다. 복호화 실패(앱 업데이트·키 회전 시 실제로 발생한다)는
  /// '세션 없음' 으로 강등한다. 부팅 경로에서 예외가 새면 앱이 뜨지 않는다.
  Future<void> load() async {
    try {
      _cached = await _storage.read(key: _key);
    } catch (_) {
      _cached = null;
      await _safeDelete();
    }
  }

  Future<void> save(String sessionId) async {
    _cached = sessionId;
    try {
      await _storage.write(key: _key, value: sessionId);
    } catch (_) {
      // 메모리 캐시는 살아 있으므로 이번 실행 동안의 요청은 통과한다.
    }
  }

  /// 멱등. 병렬 요청이 동시에 401 을 받아도 안전하다.
  Future<void> clear() async {
    if (_cached == null) return;
    _cached = null;
    await _safeDelete();
  }

  Future<void> _safeDelete() async {
    try {
      await _storage.delete(key: _key);
    } catch (_) {
      // 지우지 못해도 캐시가 비었으므로 요청에 실리지 않는다.
    }
  }
}

/// 세션 만료 방송. 인터셉터는 인증·라운드 상태를 직접 알지 못하고, 그 상태들이 여기에
/// 리스너로 붙는다(5·7단위). 리스너가 없어도 세션 폐기는 이미 끝나 있다.
class SessionExpiryNotifier extends ChangeNotifier {
  void notifyExpired() => notifyListeners();
}

/// main() 에서 선적재한 인스턴스로 override 한다.
final sessionStoreProvider = Provider<SessionStore>(
  (ref) => throw UnimplementedError('main() 에서 override 한다'),
);

final sessionExpiryProvider = Provider<SessionExpiryNotifier>((ref) {
  final notifier = SessionExpiryNotifier();
  ref.onDispose(notifier.dispose);
  return notifier;
});
