import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:trypto/core/api/api_client.dart';
import 'package:trypto/core/auth/session_store.dart';
import 'package:trypto/core/router/guard.dart';
import 'package:trypto/core/theme/theme.dart';
import 'package:trypto/features/ranking/ranker_portfolio_sheet.dart';
import 'package:trypto/features/ranking/ranking_page.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('asRatio', () {
    // 서버가 비중을 0.35 로 주든 35 로 주든 35.0% 여야 한다(사양서 §6.2.7).
    test('1 이하는 비율 그대로 본다', () {
      expect(asRatio(0.35), 0.35);
      expect(asRatio(0), 0);
      expect(asRatio(1), 1);
    });

    test('1 초과는 퍼센트로 보고 100 으로 나눈다', () {
      expect(asRatio(35), 0.35);
      expect(asRatio(100), 1);
    });
  });

  group('canViewRankerPortfolio', () {
    test('100위까지만 열람할 수 있다', () {
      expect(canViewRankerPortfolio(1), isTrue);
      expect(canViewRankerPortfolio(100), isTrue);
      // 101위 이상은 서버가 403 을 낸다. 요청 자체를 보내지 않는다.
      expect(canViewRankerPortfolio(101), isFalse);
    });
  });

  // 더보기가 실패해도 hasNext 는 true 로 남는다. 스크롤 가드가 실패 상태를 보지 않으면 목록
  // 끝에 머무는 사용자의 스크롤마다 같은 요청이 되풀이된다. 웹은 사용자가 버튼을 눌러야만
  // 재시도한다(RankingPage.tsx:190-206·454-466).
  group('더보기 실패', () {
    late SessionStore store;
    late SessionExpiryNotifier expiry;
    late Dio dio;
    late DioAdapter adapter;
    late List<RequestOptions> requests;

    Map<String, dynamic> envelope(Object? data) => {
      'status': 200,
      'code': 'SUCCESS',
      'message': '',
      'data': data,
    };

    List<Map<String, dynamic>> rankers(int from, int count) => [
      for (var i = 0; i < count; i++)
        {
          'rank': from + i,
          'userId': 100 + from + i,
          'nickname': '랭커${from + i}',
          'profitRate': 20.0 - i,
          'tradeCount': 12,
        },
    ];

    /// 목록 조회 횟수. 통계(`/api/rankings/stats`)는 경로가 달라 섞이지 않는다.
    int rankingCalls() =>
        requests.where((request) => request.path == '/api/rankings').length;

    setUp(() {
      FlutterSecureStorage.setMockInitialValues({});
      store = SessionStore();
      expiry = SessionExpiryNotifier();
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
          '/api/rankings',
          (server) => server.reply(
            200,
            envelope({
              'content': rankers(1, 20),
              'nextCursor': 20,
              'hasNext': true,
            }),
          ),
        )
        ..onGet(
          '/api/rankings/stats',
          (server) => server.reply(
            200,
            envelope({
              'totalParticipants': 128,
              'maxProfitRate': 42.0,
              'avgProfitRate': 3.5,
            }),
          ),
        )
        // 나중에 등록한 경로가 이긴다. 커서가 붙은 2페이지만 실패시킨다.
        ..onGet(
          '/api/rankings',
          (server) => server.reply(500, {
            'status': 500,
            'code': 'INTERNAL_SERVER_ERROR',
            'message': '일시적인 오류입니다.',
            'data': null,
          }),
          queryParameters: {'period': 'DAILY', 'cursorRank': 20, 'size': 20},
        );
    });

    /// 랭킹 화면만 띄운다. 미인증이라 내 랭킹은 조회하지 않는다.
    Future<void> pumpRanking(WidgetTester tester) async {
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 3;
      addTearDown(tester.view.reset);

      final router = GoRouter(
        initialLocation: Routes.ranking,
        routes: [
          GoRoute(
            path: Routes.ranking,
            builder: (context, state) => const RankingPage(),
          ),
        ],
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            sessionStoreProvider.overrideWithValue(store),
            sessionExpiryProvider.overrideWithValue(expiry),
            dioProvider.overrideWithValue(dio),
          ],
          child: MaterialApp.router(
            theme: buildTryptoTheme(),
            routerConfig: router,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byType(RankingPage), findsOneWidget);
      expect(rankingCalls(), 1);
    }

    /// 목록 끝까지 끌어 더보기를 한 번 실패시킨다.
    Future<void> failLoadMore(WidgetTester tester) async {
      await tester.drag(find.byType(CustomScrollView), const Offset(0, -2000));
      await tester.pumpAndSettle();

      expect(find.text('추가 랭킹을 불러오지 못했습니다.'), findsOneWidget);
      expect(rankingCalls(), 2);
    }

    testWidgets('실패한 뒤에는 스크롤이 같은 요청을 다시 쏘지 않는다', (tester) async {
      await pumpRanking(tester);
      await failLoadMore(tester);

      // 실패 직후에도 사용자는 목록 하단 200px 안에 머문다. 여기서 조금만 움직여도
      // 스크롤 알림이 계속 들어온다.
      await tester.drag(find.byType(CustomScrollView), const Offset(0, 120));
      await tester.pumpAndSettle();
      await tester.drag(find.byType(CustomScrollView), const Offset(0, -120));
      await tester.pumpAndSettle();

      expect(rankingCalls(), 2);
      expect(find.text('추가 랭킹을 불러오지 못했습니다.'), findsOneWidget);
      expect(tester.takeException(), isNull);
    });

    testWidgets('다시 시도를 눌렀을 때만 한 번 더 조회한다', (tester) async {
      await pumpRanking(tester);
      await failLoadMore(tester);

      // 나중에 등록한 경로가 이긴다. 재시도부터는 2페이지가 성공한다.
      adapter.onGet(
        '/api/rankings',
        (server) => server.reply(
          200,
          envelope({
            'content': rankers(21, 5),
            'nextCursor': null,
            'hasNext': false,
          }),
        ),
        queryParameters: {'period': 'DAILY', 'cursorRank': 20, 'size': 20},
      );

      await tester.ensureVisible(find.text('다시 시도'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('다시 시도'));
      await tester.pumpAndSettle();

      expect(rankingCalls(), 3);
      expect(find.text('추가 랭킹을 불러오지 못했습니다.'), findsNothing);
      expect(tester.takeException(), isNull);
    });
  });
}
