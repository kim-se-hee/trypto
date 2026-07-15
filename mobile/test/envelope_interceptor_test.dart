import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:trypto/core/api/api_client.dart';
import 'package:trypto/core/api/api_exception.dart';
import 'package:trypto/core/auth/session_store.dart';
import 'package:trypto/features/round/round_repository.dart';

/// 봉투 언랩은 계약 회귀의 유일한 방어선이다(계획서 §7-2). 성공 판정이 3중 조건
/// (`2xx && code ∈ {SUCCESS, CREATED}`)이라 어느 하나를 놓쳐도 조용히 틀린다.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late Dio dio;
  late DioAdapter adapter;

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
    dio = buildDio(
      store: SessionStore(),
      expiry: SessionExpiryNotifier(),
      baseUrl: 'http://test.local',
    );
    adapter = DioAdapter(dio: dio);
  });

  Map<String, dynamic> envelope(
    int status,
    String code,
    Object? data, {
    String message = '조회 성공',
  }) => {'status': status, 'code': code, 'message': message, 'data': data};

  group('성공 경로', () {
    test('200 SUCCESS — data 만 남기고 봉투를 벗긴다', () async {
      adapter.onGet(
        '/api/users/me',
        (server) => server.reply(
          200,
          envelope(200, 'SUCCESS', {'userId': 7, 'nickname': '트립토'}),
        ),
      );

      final response = await dio.get('/api/users/me');

      expect(response.data, {'userId': 7, 'nickname': '트립토'});
    });

    test('201 CREATED — 주문·라운드·송금 생성 경로', () async {
      adapter.onPost(
        '/api/orders',
        (server) => server.reply(
          201,
          envelope(201, 'CREATED', {'orderId': 42}, message: '주문이 체결되었습니다.'),
        ),
        data: Matchers.any,
      );

      final response = await dio.post('/api/orders', data: const {});

      expect(response.data, {'orderId': 42});
    });

    test('201 SUCCESS — 피드백만 201 에 SUCCESS 를 싣는다. data 는 null 이다', () async {
      adapter.onPost(
        '/api/feedbacks',
        (server) => server.reply(201, envelope(201, 'SUCCESS', null)),
        data: Matchers.any,
      );

      final response = await dio.post('/api/feedbacks', data: const {});

      expect(response.data, isNull);
    });
  });

  group('실패 경로', () {
    test('2xx 인데 봉투가 아니면 INVALID_RESPONSE 다', () async {
      adapter.onGet(
        '/api/users/me',
        (server) => server.reply(200, {'unexpected': 'shape'}),
      );

      final error = await dio.get('/api/users/me').onApiError();

      expect(error.code, ErrorCodes.invalidResponse);
      expect(error.status, 200);
    });

    test('2xx 인데 code 가 성공 집합 밖이면 예외다', () async {
      adapter.onGet(
        '/api/users/me',
        (server) => server.reply(
          200,
          envelope(200, 'SOMETHING_ELSE', null, message: '알 수 없는 상태입니다.'),
        ),
      );

      final error = await dio.get('/api/users/me').onApiError();

      expect(error.code, 'SOMETHING_ELSE');
      expect(error.userMessage, '알 수 없는 상태입니다.');
    });

    test('4xx 봉투는 서버 message 를 그대로 사용자 문구로 쓴다', () async {
      adapter.onPut(
        '/api/users/me/nickname',
        (server) => server.reply(
          400,
          envelope(
            400,
            'INVALID_NICKNAME_LENGTH',
            null,
            message: '닉네임은 2자 이상 20자 이하여야 합니다.',
          ),
        ),
        data: Matchers.any,
      );

      final error = await dio
          .put('/api/users/me/nickname', data: const {})
          .onApiError();

      expect(error.status, 400);
      expect(error.code, 'INVALID_NICKNAME_LENGTH');
      expect(error.userMessage, '닉네임은 2자 이상 20자 이하여야 합니다.');
    });

    test('message 가 비면 코드별 기본 문구로 떨어진다', () async {
      adapter.onGet(
        '/api/users/me',
        (server) =>
            server.reply(500, envelope(500, 'INTERNAL_SERVER_ERROR', null, message: '')),
      );

      final error = await dio.get('/api/users/me').onApiError();

      expect(error.userMessage, '일시적인 오류입니다. 잠시 후 다시 시도해 주세요.');
    });

    test('응답 자체가 없으면 NETWORK_ERROR 다', () async {
      adapter.onGet(
        '/api/users/me',
        (server) => server.throws(
          -1,
          DioException.connectionError(
            requestOptions: RequestOptions(path: '/api/users/me'),
            reason: '연결할 수 없음',
          ),
        ),
      );

      final error = await dio.get('/api/users/me').onApiError();

      expect(error.code, ErrorCodes.networkError);
      expect(error.userMessage, '네트워크에 연결할 수 없습니다.');
    });
  });

  group('ROUND_NOT_ACTIVE', () {
    test('409 는 예외가 아니라 null 이다 — 신규 사용자의 앱 진입 경로', () async {
      adapter.onGet(
        '/api/rounds/active',
        (server) => server.reply(
          409,
          envelope(409, 'ROUND_NOT_ACTIVE', null, message: '진행 중인 라운드가 없습니다.'),
        ),
      );

      await expectLater(RoundRepository(dio).getActiveRound(), completion(isNull));
    });

    test('그 밖의 실패는 그대로 던진다', () async {
      adapter.onGet(
        '/api/rounds/active',
        (server) => server.reply(
          403,
          envelope(403, 'ROUND_ACCESS_DENIED', null, message: '해당 라운드에 접근할 수 없습니다.'),
        ),
      );

      await expectLater(
        RoundRepository(dio).getActiveRound(),
        throwsA(
          isA<ApiException>().having(
            (e) => e.code,
            'code',
            'ROUND_ACCESS_DENIED',
          ),
        ),
      );
    });
  });
}

extension on Future<Object?> {
  /// 인터셉터를 통과한 오류는 항상 ApiException 이어야 한다.
  Future<ApiException> onApiError() async {
    try {
      await this;
    } on DioException catch (error) {
      return (error.error ?? error).asApiException;
    }
    fail('ApiException 이 발생해야 한다');
  }
}
