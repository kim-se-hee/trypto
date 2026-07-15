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
import 'package:trypto/features/market/coin_row.dart';
import 'package:trypto/features/market/order_history_tab.dart';
import 'package:trypto/features/market/order_sheet.dart';

import '../support/fake_stomp.dart';

/// 계획서 §7-⑧ — 시장가 매수를 고르면 수량 칸이 사라지고, 전송 바디에 `volume` 이 없고
/// `price` 에 총액이 실린다. 이 규칙을 어기면 서버가 400 이다.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late SessionStore store;
  late SessionExpiryNotifier expiry;
  late Dio dio;
  late DioAdapter adapter;
  late FakeStompService stomp;
  late List<RequestOptions> requests;

  Map<String, dynamic> envelope(Object? data, {String code = 'SUCCESS'}) => {
    'status': 200,
    'code': code,
    'message': '',
    'data': data,
  };

  Map<String, dynamic> coin(int id, String symbol, String name, double price) =>
      {
        'exchangeCoinId': id,
        'coinId': id,
        'coinSymbol': symbol,
        'coinName': name,
        'price': price,
        'changeRate': 0.01,
        'volume': 1e11,
      };

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
            coin(1, 'BTC', '비트코인', 96000000),
            coin(2, 'ETH', '이더리움', 5300000),
          ]),
        ),
      )
      ..onGet(
        '/api/candles',
        (server) => server.reply(200, envelope(<Object>[])),
        queryParameters: {
          'exchange': 'UPBIT',
          'coin': 'BTC',
          'interval': '1d',
          'limit': 90,
        },
      )
      ..onGet(
        '/api/orders/available',
        (server) => server.reply(
          200,
          envelope({'available': 10000000.0, 'currentPrice': 96000000.0}),
        ),
        queryParameters: {
          'walletId': 100,
          'exchangeCoinId': 1,
          'side': 'BUY',
        },
      )
      ..onGet(
        '/api/orders/available',
        (server) => server.reply(
          200,
          envelope({'available': 0.5, 'currentPrice': 96000000.0}),
        ),
        queryParameters: {
          'walletId': 100,
          'exchangeCoinId': 1,
          'side': 'SELL',
        },
      )
      ..onPost(
        '/api/orders',
        (server) => server.reply(
          201,
          envelope({
            'orderId': 55,
            'side': 'BUY',
            'orderType': 'MARKET',
            'quantity': 0.00052,
            'orderAmount': 50000.0,
            'fee': 25.0,
            'filledPrice': 96000000.0,
            'price': null,
            'status': 'FILLED',
            'createdAt': '2026-07-15T10:00:00',
            'filledAt': '2026-07-15T10:00:00',
          }, code: 'CREATED'),
        ),
        data: Matchers.any,
      );
  });

  void stubHistory(String status, List<Map<String, dynamic>> content) {
    adapter.onGet(
      '/api/orders',
      (server) => server.reply(
        200,
        envelope({'content': content, 'nextCursor': null, 'hasNext': false}),
      ),
      queryParameters: {
        'walletId': 100,
        'exchangeCoinId': 1,
        'status': status,
        'size': 20,
      },
    );
  }

  Future<void> openDetail(WidgetTester tester) async {
    // 기본 800×600 은 가로 화면이라 시트 안의 입력 칸이 뷰포트 밖으로 밀려 빌드되지 않는다.
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

    await tester.tap(find.byType(CoinRow).first);
    await tester.pumpAndSettle();
  }

  Future<void> openSheet(WidgetTester tester, String side) async {
    await tester.tap(find.widgetWithText(FilledButton, side));
    await tester.pumpAndSettle();
    expect(find.byType(OrderSheet), findsOneWidget);
  }

  Finder inSheet(Finder matching) =>
      find.descendant(of: find.byType(OrderSheet), matching: matching);

  Map<String, dynamic> placedOrder() => requests
      .lastWhere((request) => request.path == '/api/orders' && request.method == 'POST')
      .data as Map<String, dynamic>;

  testWidgets('시장가 매수 — 수량 칸이 사라지고 바디에 volume 이 없다', (tester) async {
    stubHistory('FILLED', []);
    await openDetail(tester);
    await openSheet(tester, '매수');

    // 지정가(기본)에서는 가격·수량·총액 3칸이다.
    expect(inSheet(find.text('주문 수량')), findsOneWidget);
    expect(inSheet(find.text('주문 총액')), findsOneWidget);

    await tester.tap(inSheet(find.text('시장가')));
    await tester.pumpAndSettle();

    expect(inSheet(find.text('주문 수량')), findsNothing);
    expect(inSheet(find.text('주문 총액')), findsOneWidget);
    expect(inSheet(find.byType(TextField)), findsOneWidget);

    await tester.enterText(inSheet(find.byType(TextField)), '50000');
    await tester.pumpAndSettle();
    await tester.tap(inSheet(find.widgetWithText(FilledButton, '매수')));
    await tester.pumpAndSettle();

    final body = placedOrder();
    expect(body.containsKey('volume'), isFalse);
    expect(body['price'], 50000.0);
    expect(body['orderType'], 'MARKET');
    expect(body['side'], 'BUY');
    expect(body['walletId'], 100);
    expect(body['exchangeCoinId'], 1);
    // 멱등키는 UUID v4 다. 형식이 틀리면 서버가 500 을 낸다.
    expect(
      body['clientOrderId'],
      matches(
        r'^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$',
      ),
    );

    expect(find.text('주문이 체결되었습니다.'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('지정가 매도 — 수량을 넣으면 총액이 따라오고 둘 다 전송된다', (tester) async {
    stubHistory('FILLED', []);
    await openDetail(tester);
    await openSheet(tester, '매도');

    final fields = inSheet(find.byType(TextField));
    await tester.enterText(fields.at(0), '96000000');
    await tester.pumpAndSettle();
    await tester.enterText(fields.at(1), '0.001');
    await tester.pumpAndSettle();

    // 총액 = 0.001 × 96,000,000 (지정가에서만 연동한다)
    expect(
      tester.widget<TextField>(fields.at(2)).controller?.text,
      '96,000',
    );

    await tester.tap(inSheet(find.widgetWithText(FilledButton, '매도')));
    await tester.pumpAndSettle();

    final body = placedOrder();
    expect(body['volume'], 0.001);
    expect(body['price'], 96000000.0);
    expect(body['orderType'], 'LIMIT');
    expect(body['side'], 'SELL');
    expect(tester.takeException(), isNull);
  });

  testWidgets('거래내역 — 미체결 주문을 취소하면 목록에서 사라진다', (tester) async {
    stubHistory('FILLED', []);
    stubHistory('PENDING', [
      {
        'orderId': 77,
        'exchangeCoinId': 1,
        'side': 'BUY',
        'orderType': 'LIMIT',
        'quantity': 0.001,
        'filledPrice': null,
        'price': 90000000.0,
        'orderAmount': null,
        'fee': null,
        'createdAt': '2026-07-15T10:00:00',
        'filledAt': null,
      },
    ]);
    adapter.onPost(
      '/api/orders/77/cancel',
      (server) =>
          server.reply(200, envelope({'orderId': 77, 'status': 'CANCELED'})),
      data: Matchers.any,
    );

    await openDetail(tester);
    await tester.tap(find.text('거래내역'));
    await tester.pumpAndSettle();
    expect(find.byType(OrderHistoryTab), findsOneWidget);
    expect(find.text('체결 내역이 없습니다.'), findsOneWidget);

    await tester.tap(find.text('미체결'));
    await tester.pumpAndSettle();
    expect(find.text('대기'), findsOneWidget);

    await tester.tap(find.text('취소'));
    await tester.pumpAndSettle();

    final cancel = requests.lastWhere(
      (request) => request.path == '/api/orders/77/cancel',
    );
    expect((cancel.data as Map<String, dynamic>)['walletId'], 100);
    expect(find.text('미체결 주문이 없습니다.'), findsOneWidget);
    expect(find.text('주문을 취소했습니다.'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
