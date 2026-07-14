import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../auth/session_store.dart';
import '../env.dart';
import 'envelope_interceptor.dart';
import 'session_interceptor.dart';
import 'unauthorized_interceptor.dart';

/// Dio 조립(계획서 §4.1). 인터셉터 순서가 곧 계약이다.
///
/// 1. [SessionInterceptor]      — 요청에 Cookie 부착, 응답에서 Set-Cookie 회수·폐기
/// 2. [EnvelopeInterceptor]     — 봉투 언랩, 실패를 ApiException 으로 통일
/// 3. [UnauthorizedInterceptor] — 세션 만료 판정(2번이 만든 ApiException 을 읽는다)
///
/// 자동 재시도 인터셉터를 넣지 않는다(계획서 §4.1.5). 자동 재시도는 멱등키 규약을 흐린다.
/// 재시도는 사용자의 명시적 조작으로만 하고, 그때 반드시 같은 키를 재사용한다.
Dio buildDio({
  required SessionStore store,
  required SessionExpiryNotifier expiry,
  String? baseUrl,
}) {
  final dio = Dio(
    BaseOptions(
      baseUrl: baseUrl ?? Env.apiBaseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 15),
      sendTimeout: const Duration(seconds: 15),
      responseType: ResponseType.json,
      // 바디가 없으면 Content-Type 을 붙이지 않는다(사양서 §1.11.7).
      contentType: null,
    ),
  );
  dio.interceptors.addAll([
    SessionInterceptor(store),
    EnvelopeInterceptor(),
    UnauthorizedInterceptor(store: store, expiry: expiry),
  ]);
  return dio;
}

final dioProvider = Provider<Dio>((ref) {
  final dio = buildDio(
    store: ref.watch(sessionStoreProvider),
    expiry: ref.watch(sessionExpiryProvider),
  );
  ref.onDispose(dio.close);
  return dio;
});
