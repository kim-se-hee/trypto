import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:trypto/core/api/api_client.dart';
import 'package:trypto/core/auth/session_store.dart';

/// 세션 헤더 부착·회수와 401 분리 규칙(계획서 §4.1.1, §4.1.3).
///
/// `SOCIAL_LOGIN_FAILED` 도 401 이라, HTTP 401 만으로 판정하면 **로그인 실패가 세션 폐기 경로를
/// 타서** 인증 상태가 흔들린다. 그 분리를 테스트로 못 박는다.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late SessionStore store;
  late SessionExpiryNotifier expiry;
  late Dio dio;
  late DioAdapter adapter;
  late int expiredCount;

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
    store = SessionStore();
    expiry = SessionExpiryNotifier();
    expiredCount = 0;
    expiry.addListener(() => expiredCount++);
    dio = buildDio(store: store, expiry: expiry, baseUrl: 'http://test.local');
    adapter = DioAdapter(dio: dio);
  });

  Map<String, dynamic> envelope(int status, String code, Object? data) => {
    'status': status,
    'code': code,
    'message': '',
    'data': data,
  };

  const jsonHeader = {
    'content-type': ['application/json'],
  };

  group('세션 헤더 부착', () {
    test('세션이 없으면 Cookie 헤더를 붙이지 않는다', () async {
      String? sentCookie = 'sentinel';
      adapter.onGet(
        '/api/rankings',
        (server) => server.replyCallback(200, (options) {
          sentCookie = options.headers['Cookie'] as String?;
          return envelope(200, 'SUCCESS', const []);
        }),
      );

      await dio.get('/api/rankings');

      expect(sentCookie, isNull);
    });

    test('세션이 있으면 Cookie: SESSION=<값> 을 붙인다', () async {
      await store.save('abc-123');
      String? sentCookie;
      adapter.onGet(
        '/api/users/me',
        (server) => server.replyCallback(200, (options) {
          sentCookie = options.headers['Cookie'] as String?;
          return envelope(200, 'SUCCESS', const {});
        }),
      );

      await dio.get('/api/users/me');

      expect(sentCookie, 'SESSION=abc-123');
    });
  });

  group('Set-Cookie 회수', () {
    test('로그인 응답에서 SESSION 값만 뽑아 저장한다. 만료 시각은 저장하지 않는다', () async {
      adapter.onPost(
        '/api/auth/kakao/login',
        (server) => server.reply(
          200,
          envelope(200, 'SUCCESS', {
            'userId': 1,
            'nickname': '트립토',
            'newUser': false,
          }),
          headers: {
            ...jsonHeader,
            'set-cookie': [
              'SESSION=s-9f2c; Path=/; Max-Age=604800; HttpOnly; SameSite=Lax',
            ],
          },
        ),
        data: Matchers.any,
      );

      await dio.post('/api/auth/kakao/login', data: const {});

      expect(store.sessionId, 's-9f2c');
    });

    test('빈 값 + Max-Age=0(로그아웃·탈퇴)이면 폐기한다', () async {
      await store.save('s-9f2c');
      adapter.onPost(
        '/api/auth/logout',
        (server) => server.reply(
          200,
          envelope(200, 'SUCCESS', null),
          headers: {
            ...jsonHeader,
            'set-cookie': ['SESSION=; Path=/; Max-Age=0'],
          },
        ),
      );

      await dio.post('/api/auth/logout');

      expect(store.hasSession, isFalse);
    });
  });

  group('401 분리', () {
    test('UNAUTHENTICATED 는 세션을 폐기하고 만료를 알린다', () async {
      await store.save('s-9f2c');
      adapter.onGet(
        '/api/users/me',
        (server) => server.reply(401, envelope(401, 'UNAUTHENTICATED', null)),
      );

      await expectLater(dio.get('/api/users/me'), throwsA(isA<DioException>()));

      expect(store.hasSession, isFalse);
      expect(expiredCount, 1);
    });

    test('SOCIAL_LOGIN_FAILED 는 401 이지만 세션을 유지한다 — 로그인 실패다', () async {
      await store.save('s-9f2c');
      adapter.onPost(
        '/api/auth/kakao/login',
        (server) =>
            server.reply(401, envelope(401, 'SOCIAL_LOGIN_FAILED', null)),
        data: Matchers.any,
      );

      await expectLater(
        dio.post('/api/auth/kakao/login', data: const {}),
        throwsA(isA<DioException>()),
      );

      expect(store.sessionId, 's-9f2c');
      expect(expiredCount, 0);
    });

    test('봉투 없는 401(프록시·게이트웨이)도 세션을 폐기한다', () async {
      await store.save('s-9f2c');
      adapter.onGet(
        '/api/rounds/active',
        (server) => server.reply(401, 'Unauthorized'),
      );

      await expectLater(
        dio.get('/api/rounds/active'),
        throwsA(isA<DioException>()),
      );

      expect(store.hasSession, isFalse);
      expect(expiredCount, 1);
    });

    test('403·409 는 세션을 건드리지 않는다', () async {
      await store.save('s-9f2c');
      adapter.onGet(
        '/api/rounds/active',
        (server) => server.reply(409, envelope(409, 'ROUND_NOT_ACTIVE', null)),
      );

      await expectLater(
        dio.get('/api/rounds/active'),
        throwsA(isA<DioException>()),
      );

      expect(store.sessionId, 's-9f2c');
      expect(expiredCount, 0);
    });

    test('병렬 요청이 동시에 401 을 받아도 폐기는 멱등하다', () async {
      await store.save('s-9f2c');
      adapter
        ..onGet(
          '/api/rounds/active',
          (server) => server.reply(401, envelope(401, 'UNAUTHENTICATED', null)),
        )
        ..onGet(
          '/api/rounds/summary',
          (server) => server.reply(401, envelope(401, 'UNAUTHENTICATED', null)),
        );

      await Future.wait([
        dio.get('/api/rounds/active').catchError((_) => Response(requestOptions: RequestOptions())),
        dio.get('/api/rounds/summary').catchError((_) => Response(requestOptions: RequestOptions())),
      ]);

      expect(store.hasSession, isFalse);
      expect(expiredCount, 2);
    });
  });
}
