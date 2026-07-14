import 'package:dio/dio.dart';

import '../auth/session_store.dart';
import 'api_exception.dart';

/// 세션 만료를 전역 처리한다(사양서 R10). 웹에는 없는 개선이다.
///
/// 판별식을 두 겹으로 좁힌 이유(계획서 §4.1.3): `SOCIAL_LOGIN_FAILED` 도 **401** 이다.
/// HTTP 401 만으로 판정하면 로그인 실패가 세션 폐기 경로를 타서, 로그인 화면에서
/// 인증 상태가 흔들리고 redirect 가 재평가된다.
///
/// 화면 이동은 하지 않는다. 세션이 비고 인증 상태가 무효화되면 라우터의 redirect 가
/// 알아서 로그인 화면으로 보낸다.
class UnauthorizedInterceptor extends Interceptor {
  UnauthorizedInterceptor({
    required SessionStore store,
    required SessionExpiryNotifier expiry,
  }) : _store = store,
       _expiry = expiry;

  final SessionStore _store;
  final SessionExpiryNotifier _expiry;

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    final exception = err.error;
    if (exception is ApiException &&
        _isSessionExpired(err.requestOptions, exception)) {
      _store.clear();
      _expiry.notifyExpired();
    }
    handler.next(err);
  }

  bool _isSessionExpired(RequestOptions request, ApiException exception) {
    // ① 인증 엔드포인트의 401 은 세션 만료가 아니라 로그인 실패다(SOCIAL_LOGIN_FAILED).
    if (request.path.startsWith('/api/auth/')) return false;
    // ② 서버가 세션 만료라고 말한 경우.
    if (exception.isUnauthenticated) return true;
    if (exception.status != 401) return false;
    // ③ 봉투가 없는 401(프록시·게이트웨이가 가로챈 경우). 서버 ErrorCode 가 실린 401 은
    //    ②에서 이미 갈렸으므로 여기 오는 것은 세션이 아닌 다른 사유가 아니다.
    return exception.code == null ||
        exception.code == ErrorCodes.invalidResponse;
  }
}
