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
import 'package:trypto/features/market/coin_search_field.dart';
import 'package:trypto/features/market/market_controller.dart';
import 'package:trypto/features/market/market_page.dart';

import '../support/fake_stomp.dart';

/// 8단위 완료 조건: WS 없이 REST 스냅샷만으로 600행 목록이 뜨고 검색·필터·정렬이 동작한다.
/// 레이아웃 오버플로도 여기서 잡힌다(`takeException`).
///
/// 9단위: 토픽 구독이 거래소당 하나이고, 틱이 목록의 숫자와(1초 스로틀 뒤) 정렬을 바꾼다.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late SessionStore store;
  late SessionExpiryNotifier expiry;
  late Dio dio;
  late DioAdapter adapter;
  late FakeStompService stomp;

  Map<String, dynamic> envelope(Object? data) => {
    'status': 200,
    'code': 'SUCCESS',
    'message': '',
    'data': data,
  };

  Map<String, dynamic> coin(
    int id,
    String symbol,
    String name, {
    required double price,
    required double changeRate,
    required double volume,
  }) => {
    'exchangeCoinId': id,
    'coinId': id,
    'coinSymbol': symbol,
    'coinName': name,
    'price': price,
    'changeRate': changeRate,
    'volume': volume,
  };

  /// 이름 있는 4개 + 이름 없는 596개 = 600행.
  List<Map<String, dynamic>> coins() => [
    coin(1, 'BTC', '비트코인', price: 96000000, changeRate: 0.012, volume: 9e11),
    coin(2, 'ETH', '이더리움', price: 5300000, changeRate: -0.004, volume: 5e11),
    coin(3, 'SOL', '솔라나', price: 280000, changeRate: 0.031, volume: 3e11),
    coin(4, 'XRP', '리플', price: 0, changeRate: 0, volume: 0),
    for (var i = 5; i <= 600; i++)
      coin(
        i,
        'C$i',
        '코인$i',
        price: i.toDouble(),
        changeRate: i.isEven ? 0.01 : -0.01,
        volume: i.toDouble(),
      ),
  ];

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
    store = SessionStore();
    expiry = SessionExpiryNotifier();
    stomp = FakeStompService();
    dio = buildDio(store: store, expiry: expiry, baseUrl: 'http://test.local');
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
        (server) => server.reply(200, envelope(coins())),
      );
  });

  /// 부팅(인증 복구 → 라운드 조회 → /market)까지만 돌린다. 목록 응답을 늦추거나 실패시키는
  /// 테스트는 `pumpAndSettle` 을 쓸 수 없어 이 지점에서 멈춰야 한다.
  Future<void> boot(WidgetTester tester) async {
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
  }

  Future<void> pumpMarket(WidgetTester tester) async {
    await boot(tester);

    // 600행 응답은 50KB 를 넘어 dio 가 **백그라운드 아이솔레이트**에서 디코딩한다. fake async
    // 로는 완료되지 않으므로 실제 비동기 구간을 열어 준다. (프로덕션에서는 이 오프로딩 덕에
    // 목록 디코딩이 UI 스레드를 멈추지 않는다.)
    await tester.runAsync(
      () => Future<void>.delayed(const Duration(milliseconds: 300)),
    );
    await tester.pumpAndSettle();

    expect(find.byType(MarketPage), findsOneWidget);
  }

  /// 화면에 실제로 만들어진 행의 심볼. `itemExtent` 덕에 보이는 만큼만 빌드된다.
  List<String> renderedSymbols(WidgetTester tester) => tester
      .widgetList<CoinRow>(find.byType(CoinRow))
      .map((row) => row.symbol)
      .toList();

  testWidgets('스냅샷 600행이 거래대금 내림차순으로 뜬다', (tester) async {
    await pumpMarket(tester);

    final symbols = renderedSymbols(tester);
    expect(symbols, isNotEmpty);
    expect(symbols.take(3).toList(), ['BTC', 'ETH', 'SOL']);
    expect(tester.takeException(), isNull);
  });

  testWidgets('시세가 없는 코인은 현재가·거래대금을 - 로 표시한다', (tester) async {
    await pumpMarket(tester);

    await tester.enterText(find.byType(CoinSearchField), 'XRP');
    await tester.pumpAndSettle();

    // 현재가·전일대비(가격 0) + 거래대금(0) = 3칸. 주요 코인 카드(BTC/ETH/SOL)는 모두
    // 시세가 있어 `-` 를 내지 않는다.
    expect(renderedSymbols(tester), ['XRP']);
    expect(find.text('-'), findsNWidgets(3));
    expect(tester.takeException(), isNull);
  });

  testWidgets('시세를 받지 못한 코인은 주요 코인 카드에서도 가격·등락률이 - 다', (tester) async {
    // 나중에 등록한 경로가 이긴다. BTC 만 시세가 빈 짧은 목록으로 바꾼다.
    adapter.onGet(
      '/api/exchanges/1/coins',
      (server) => server.reply(
        200,
        envelope([
          coin(1, 'BTC', '비트코인', price: 0, changeRate: 0, volume: 0),
          coin(
            2,
            'ETH',
            '이더리움',
            price: 5300000,
            changeRate: -0.004,
            volume: 5e11,
          ),
        ]),
      ),
    );

    await boot(tester);
    await tester.pumpAndSettle();

    // BTC 카드(가격·등락률 2칸) + BTC 행(현재가·전일대비·거래대금 3칸) = 5칸.
    // 등락률을 0.00% 로 찍으면 실제로 0 인 코인과 구분되지 않는다.
    expect(renderedSymbols(tester), ['ETH', 'BTC']);
    expect(find.text('-'), findsNWidgets(5));
    expect(find.text('0.00%'), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets('목록을 불러오는 동안에도 필터 칩은 남는다', (tester) async {
    adapter.onGet(
      '/api/exchanges/1/coins',
      (server) => server.reply(
        200,
        envelope(<Object>[]),
        delay: const Duration(seconds: 3),
      ),
    );

    await boot(tester);
    // 로딩 인디케이터가 계속 도므로 `pumpAndSettle` 을 쓸 수 없다. 라우트 전환 애니메이션이
    // 끝날 만큼만 흘려보낸다.
    await tester.pump(const Duration(milliseconds: 500));

    expect(find.byType(MarketPage), findsOneWidget);
    expect(find.byType(CoinRow), findsNothing);
    expect(find.byType(CircularProgressIndicator), findsWidgets);
    expect(find.widgetWithText(ChoiceChip, '하락'), findsOneWidget);

    // 대기 중인 타이머를 남기면 테스트가 실패한다.
    await tester.pump(const Duration(seconds: 3));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });

  testWidgets('목록 조회가 실패해도 필터 칩은 남는다', (tester) async {
    adapter.onGet(
      '/api/exchanges/1/coins',
      (server) => server.reply(500, {
        'status': 500,
        'code': 'INTERNAL_SERVER_ERROR',
        'message': '일시적인 오류입니다.',
        'data': null,
      }),
    );

    await boot(tester);
    await tester.pumpAndSettle();

    expect(find.byType(MarketPage), findsOneWidget);
    expect(find.byType(CoinRow), findsNothing);
    expect(find.text('일시적인 오류입니다.'), findsOneWidget);
    expect(find.text('다시 시도'), findsOneWidget);
    expect(find.widgetWithText(ChoiceChip, '하락'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('초성으로 검색한다', (tester) async {
    await pumpMarket(tester);

    await tester.enterText(find.byType(CoinSearchField), 'ㅂㅌ');
    await tester.pumpAndSettle();

    expect(renderedSymbols(tester), ['BTC']);
    expect(tester.takeException(), isNull);
  });

  testWidgets('검색 결과가 없으면 빈 상태를 보여준다', (tester) async {
    await pumpMarket(tester);

    await tester.enterText(find.byType(CoinSearchField), '없는코인');
    await tester.pumpAndSettle();

    expect(find.byType(CoinRow), findsNothing);
    expect(find.text('검색 결과가 없습니다.'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('하락 필터는 내린 코인만 남긴다', (tester) async {
    await pumpMarket(tester);

    await tester.tap(find.widgetWithText(ChoiceChip, '하락'));
    await tester.pumpAndSettle();

    final symbols = renderedSymbols(tester);
    expect(symbols, isNotEmpty);
    expect(symbols, isNot(contains('BTC')));
    expect(symbols.first, 'ETH');
    expect(tester.takeException(), isNull);
  });

  testWidgets('정렬 키를 바꾸면 목록 순서가 바뀐다', (tester) async {
    await pumpMarket(tester);

    await tester.tap(find.byType(PopupMenuButton<MarketSortKey>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('현재가').last);
    await tester.pumpAndSettle();

    expect(renderedSymbols(tester).first, 'BTC');

    // 같은 키를 다시 누르면 방향만 뒤집는다 → 최저가가 맨 위로 온다.
    await tester.tap(find.byType(PopupMenuButton<MarketSortKey>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('현재가').last);
    await tester.pumpAndSettle();

    expect(renderedSymbols(tester).first, 'XRP');
    expect(tester.takeException(), isNull);
  });

  testWidgets('티커 토픽은 거래소당 하나만 구독한다', (tester) async {
    await pumpMarket(tester);

    expect(stomp.destinations, ['/topic/tickers.1']);
  });

  testWidgets('틱은 숫자를 즉시 바꾸고 정렬은 1초 스로틀 뒤에 따라온다', (tester) async {
    await pumpMarket(tester);
    expect(renderedSymbols(tester).take(2).toList(), ['BTC', 'ETH']);

    // ETH 의 거래대금이 BTC 를 앞지른다.
    stomp.emit(
      '/topic/tickers.1',
      '[{"coinId":2,"symbol":"ETH","price":5555555,"changeRate":0.02,'
          '"quoteTurnover":9.9e11,"timestamp":1735689600123}]',
    );
    await tester.pump();

    // 숫자는 그 프레임에 바뀐다. 목록 행과 주요 코인 카드가 같은 notifier 를 구독한다.
    expect(find.text('₩5,555,555'), findsNWidgets(2));
    // 자리는 아직 바뀌지 않는다 — 초당 60번 자리를 바꾸는 목록은 읽을 수 없다.
    expect(renderedSymbols(tester).take(2).toList(), ['BTC', 'ETH']);

    await tester.pump(const Duration(seconds: 1));
    expect(renderedSymbols(tester).take(2).toList(), ['ETH', 'BTC']);
    expect(tester.takeException(), isNull);
  });

  testWidgets('다른 탭으로 나가면 구독을 해제한다', (tester) async {
    await pumpMarket(tester);
    expect(stomp.destinations, isNotEmpty);

    await tester.tap(find.text('랭킹'));
    await tester.pumpAndSettle();

    expect(stomp.destinations, isEmpty);
  });
}

