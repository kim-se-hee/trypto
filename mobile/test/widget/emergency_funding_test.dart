import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:trypto/app.dart';
import 'package:trypto/core/api/api_client.dart';
import 'package:trypto/core/auth/session_store.dart';
import 'package:trypto/core/realtime/stomp_service.dart';
import 'package:trypto/features/round/emergency_funding_sheet.dart';

import '../support/fake_stomp.dart';

/// 12단위 완료 조건: 3회를 모두 쓰면 버튼이 비활성된다.
/// 상한(100만원) 초과는 **입력 단계에서** 막는다 — 웹은 제출 시점에 실패하고도 아무 말이 없다.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late SessionStore store;
  late SessionExpiryNotifier expiry;
  late Dio dio;
  late DioAdapter adapter;
  late FakeStompService stomp;
  late List<RequestOptions> requests;

  Map<String, dynamic> envelope(Object? data) => {
    'status': 200,
    'code': 'SUCCESS',
    'message': '',
    'data': data,
  };

  void stubRound({required int chargeCount}) {
    adapter.onGet(
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
          'emergencyChargeCount': chargeCount,
          'startedAt': '2026-07-01T09:00:00',
          'endedAt': null,
          'rules': <Object>[],
          'wallets': [
            {'walletId': 100, 'exchangeId': 1},
          ],
        }),
      ),
    );
  }

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
    store = SessionStore();
    expiry = SessionExpiryNotifier();
    stomp = FakeStompService();
    requests = [];
    dio = buildDio(store: store, expiry: expiry, baseUrl: 'http://test.local');
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          requests.add(options);
          handler.next(options);
        },
      ),
    );
    adapter = DioAdapter(dio: dio);

    adapter
      ..onGet(
        '/api/users/me',
        (server) => server.reply(
          200,
          envelope({
            'userId': 1,
            'nickname': '테스터',
            'createdAt': '2026-07-01T09:00:00',
          }),
        ),
      )
      ..onGet(
        '/api/rounds/summary',
        (server) => server.reply(200, envelope({'totalRoundCount': 1})),
      )
      ..onGet(
        '/api/exchanges/1/coins',
        (server) => server.reply(
          200,
          envelope([
            {
              'exchangeCoinId': 1,
              'coinId': 1,
              'coinSymbol': 'BTC',
              'coinName': '비트코인',
              'price': 96000000.0,
              'changeRate': 0.01,
              'volume': 1e11,
            },
          ]),
        ),
      );
  });

  Finder inSheet(Finder matching) => find.descendant(
    of: find.byType(EmergencyFundingSheet),
    matching: matching,
  );

  Future<void> pumpMarket(WidgetTester tester) async {
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);

    await store.save('session-id');
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sessionStoreProvider.overrideWithValue(store),
          sessionExpiryProvider.overrideWithValue(expiry),
          dioProvider.overrideWithValue(dio),
          stompServiceProvider.overrideWithValue(stomp),
        ],
        child: const TryptoApp(),
      ),
    );
    for (var i = 0; i < 10; i++) {
      await tester.pump(const Duration(milliseconds: 50));
    }
    await tester.pumpAndSettle();
  }

  testWidgets('배너에 남은 횟수가 뜨고 상한 초과는 확정 버튼을 막는다', (tester) async {
    stubRound(chargeCount: 3);
    await pumpMarket(tester);

    expect(find.text('긴급 자금 · 남은 횟수 3회'), findsOneWidget);

    await tester.tap(find.widgetWithText(FilledButton, '투입'));
    await tester.pumpAndSettle();
    expect(find.byType(EmergencyFundingSheet), findsOneWidget);

    final confirm = find.widgetWithText(FilledButton, '투입 확정');
    // 아무것도 넣지 않으면 비활성이다.
    expect(tester.widget<FilledButton>(confirm).onPressed, isNull);

    await tester.enterText(inSheet(find.byType(TextField)), '1000001');
    await tester.pumpAndSettle();

    expect(
      find.text('상한을 초과했습니다. 1,000,000원 이하로 입력해주세요.'),
      findsOneWidget,
    );
    expect(tester.widget<FilledButton>(confirm).onPressed, isNull);
    // 상한 초과는 네트워크 호출 없이 막는다.
    expect(
      requests.any((request) => request.path.contains('emergency-funding')),
      isFalse,
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets('프리셋으로 투입하면 남은 횟수가 줄고 성공을 알린다', (tester) async {
    stubRound(chargeCount: 3);
    adapter.onPost(
      '/api/rounds/10/emergency-funding',
      (server) => server.reply(
        200,
        envelope({
          'roundId': 10,
          'exchangeId': 1,
          'chargedAmount': 500000.0,
          'remainingChargeCount': 2,
        }),
      ),
      data: Matchers.any,
    );

    await pumpMarket(tester);
    await tester.tap(find.widgetWithText(FilledButton, '투입'));
    await tester.pumpAndSettle();

    // 프리셋은 상한의 25% / 50% / 100%.
    await tester.tap(find.widgetWithText(OutlinedButton, '50만'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, '투입 확정'));
    await tester.pumpAndSettle();

    final body = requests
        .lastWhere((request) => request.path.contains('emergency-funding'))
        .data as Map<String, dynamic>;
    expect(body['exchangeId'], 1);
    expect(body['amount'], 500000.0);
    // 멱등키는 UUID v4 필수 — 형식이 틀리면 서버가 500 을 낸다.
    expect(
      body['idempotencyKey'],
      matches(
        r'^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$',
      ),
    );

    expect(find.byType(EmergencyFundingSheet), findsNothing);
    expect(find.text('50만원을 투입했습니다.'), findsOneWidget);
    expect(find.text('긴급 자금 · 남은 횟수 2회'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('3회를 모두 쓰면 배너 버튼이 비활성된다', (tester) async {
    stubRound(chargeCount: 0);
    await pumpMarket(tester);

    expect(find.text('긴급 자금 · 3회를 모두 사용했습니다'), findsOneWidget);
    expect(
      tester.widget<FilledButton>(find.widgetWithText(FilledButton, '투입')).onPressed,
      isNull,
    );
    expect(tester.takeException(), isNull);
  });
}
