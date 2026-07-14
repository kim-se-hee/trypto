import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:trypto/app.dart';
import 'package:trypto/core/api/api_client.dart';
import 'package:trypto/core/auth/session_store.dart';
import 'package:trypto/core/realtime/stomp_service.dart';
import 'package:trypto/features/auth/login_page.dart';
import 'package:trypto/features/market/market_page.dart';
import 'package:trypto/features/round/round_create_page.dart';

import 'support/fake_stomp.dart';

/// 콜드 스타트 3경로. 가드 단위 테스트가 판별식을 고정한다면, 여기서는 **부팅 첫 프레임에
/// redirect loop 가 나지 않는다**는 것을 실제 GoRouter 로 확인한다(6단위 완료 조건).
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late SessionStore store;
  late SessionExpiryNotifier expiry;
  late Dio dio;
  late DioAdapter adapter;

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
    store = SessionStore();
    expiry = SessionExpiryNotifier();
    dio = buildDio(store: store, expiry: expiry, baseUrl: 'http://test.local');
    adapter = DioAdapter(dio: dio);
  });

  Map<String, dynamic> envelope(Object? data, {String code = 'SUCCESS'}) => {
    'status': 200,
    'code': code,
    'message': '',
    'data': data,
  };

  void mockMe() => adapter.onGet(
    '/api/users/me',
    (server) => server.reply(
      200,
      envelope({
        'userId': 1,
        'nickname': '테스터',
        'createdAt': '2026-07-01T09:00:00',
      }),
    ),
  );

  void mockSummary(int totalRoundCount) => adapter.onGet(
    '/api/rounds/summary',
    (server) =>
        server.reply(200, envelope({'totalRoundCount': totalRoundCount})),
  );

  void mockActiveRound() => adapter.onGet(
    '/api/rounds/active',
    (server) => server.reply(
      200,
      envelope({
        'roundId': 10,
        'userId': 1,
        'roundNumber': 1,
        'status': 'ACTIVE',
        'initialSeed': 10000000.0,
        'emergencyFundingLimit': 1000000.0,
        'emergencyChargeCount': 3,
        'startedAt': '2026-07-01T09:00:00',
        'endedAt': null,
        'rules': <Object>[],
        'wallets': [
          {'walletId': 100, 'exchangeId': 1},
        ],
      }),
    ),
  );

  void mockNoActiveRound() => adapter.onGet(
    '/api/rounds/active',
    (server) => server.reply(409, {
      'status': 409,
      'code': 'ROUND_NOT_ACTIVE',
      'message': '진행 중인 라운드가 없습니다.',
      'data': null,
    }),
  );

  Future<void> pumpApp(WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sessionStoreProvider.overrideWithValue(store),
          sessionExpiryProvider.overrideWithValue(expiry),
          dioProvider.overrideWithValue(dio),
          stompServiceProvider.overrideWithValue(FakeStompService()),
        ],
        child: const TryptoApp(),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('세션이 없으면 로그인 화면에서 멈춘다', (tester) async {
    await pumpApp(tester);

    expect(find.byType(LoginPage), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('세션이 있고 활성 라운드가 있으면 마켓 탭으로 들어간다', (tester) async {
    await store.save('session-id');
    mockMe();
    mockActiveRound();
    mockSummary(1);

    await pumpApp(tester);

    expect(find.byType(MarketPage), findsOneWidget);
    expect(find.text('포트폴리오'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('라운드를 한 번도 시작하지 않았으면 라운드 생성으로 보낸다', (tester) async {
    await store.save('session-id');
    mockMe();
    mockNoActiveRound();
    mockSummary(0);

    await pumpApp(tester);

    expect(find.byType(RoundCreatePage), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('세션 복구가 실패하면 세션을 폐기하고 로그인으로 보낸다', (tester) async {
    await store.save('stale-session-id');
    adapter.onGet(
      '/api/users/me',
      (server) => server.reply(401, {
        'status': 401,
        'code': 'UNAUTHENTICATED',
        'message': '로그인이 필요합니다.',
        'data': null,
      }),
    );

    await pumpApp(tester);

    expect(find.byType(LoginPage), findsOneWidget);
    expect(store.hasSession, isFalse);
    expect(tester.takeException(), isNull);
  });
}
