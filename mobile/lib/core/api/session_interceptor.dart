import 'package:dio/dio.dart';

import '../auth/session_store.dart';

/// 세션 쿠키를 손으로 붙이고 회수한다(사양서 R2).
///
/// 네이티브 HTTP 클라이언트는 브라우저와 달리 `Set-Cookie` 를 자동 저장·재전송하지 않는다.
class SessionInterceptor extends Interceptor {
  SessionInterceptor(this._store);

  static const String _setCookieHeader = 'set-cookie';

  /// `SESSION=<값>` 에서 값만 떼어낸다. 뒤따르는 `Path`·`Max-Age` 같은 속성은 버린다.
  static final RegExp _sessionCookie = RegExp(
    r'(?:^|[;,\s])SESSION=([^;,\s]*)',
  );

  final SessionStore _store;

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final sessionId = _store.sessionId;
    if (sessionId != null) {
      options.headers['Cookie'] = 'SESSION=$sessionId';
    }
    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    _harvest(response);
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    final response = err.response;
    if (response != null) _harvest(response);
    handler.next(err);
  }

  /// 값이 비어 있으면(로그아웃·탈퇴의 `Max-Age=0`) 삭제, 있으면 저장한다.
  /// 만료 시각은 저장하지 않는다 — 서버의 슬라이딩 TTL 을 그대로 쓴다.
  void _harvest(Response response) {
    final cookies = response.headers[_setCookieHeader];
    if (cookies == null) return;
    for (final cookie in cookies) {
      final match = _sessionCookie.firstMatch(cookie);
      if (match == null) continue;
      final value = match.group(1) ?? '';
      if (value.isEmpty) {
        _store.clear();
      } else {
        _store.save(value);
      }
      return;
    }
  }
}
