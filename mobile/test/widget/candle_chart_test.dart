import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/realtime/ticker_store.dart';
import 'package:trypto/core/theme/theme.dart';
import 'package:trypto/features/market/candle_chart.dart';
import 'package:trypto/features/market/candle_painter.dart';
import 'package:trypto/features/market/candle_repository.dart';
import 'package:trypto/features/market/live_candles.dart';
import 'package:trypto/models/candle.dart';
import 'package:trypto/models/enums.dart';
import 'package:trypto/models/exchange_coin.dart';
import 'package:trypto/models/ticker.dart';

/// 10단위 완료 조건 ⑤⑥: 과거로 패닝해 실시간 봉이 화면 밖이면 repaint 가 0이고, 새 봉이
/// 열리고 15초 뒤 REST 재조회가 **정확히 1회** 나가며 보고 있던 구간이 그대로다.
void main() {
  const request = CandleRequest(
    exchangeCode: 'UPBIT',
    symbol: 'BTC',
    interval: CandleInterval.minute1,
  );

  DateTime at(int minute, [int second = 0]) =>
      DateTime(2026, 7, 15, 10, minute, second);

  /// 10:00 ~ 11:29. 최초 표시 개수(1분봉 40개)보다 많아야 패닝할 여지가 생긴다.
  List<Candle> server() => [
    for (var minute = 0; minute < 90; minute++)
      Candle(
        time: at(minute),
        open: 100 + minute.toDouble(),
        high: 110 + minute.toDouble(),
        low: 90 + minute.toDouble(),
        close: 105 + minute.toDouble(),
      ),
  ];

  Ticker tick(double price, DateTime time) => Ticker(
    coinId: 1,
    symbol: 'BTC',
    price: price,
    changeRate: 0,
    quoteTurnover: 0,
    timestamp: time.millisecondsSinceEpoch,
  );

  late _FakeCandleRepository repository;
  late ProviderContainer container;
  late TickerStore store;

  setUp(() {
    repository = _FakeCandleRepository(server());
    container = ProviderContainer(
      overrides: [candleRepositoryProvider.overrideWithValue(repository)],
    );
    store = container.read(tickerStoreProvider)
      ..switchExchange([
        const ExchangeCoin(
          exchangeCoinId: 1,
          coinId: 1,
          coinSymbol: 'BTC',
          coinName: '비트코인',
          price: 100,
          changeRate: 0,
          volume: 0,
        ),
      ]);
  });

  tearDown(() => container.dispose());

  CandlePainter painterOf(WidgetTester tester) => tester
      .widgetList<CustomPaint>(find.byType(CustomPaint))
      .map((paint) => paint.painter)
      .whereType<CandlePainter>()
      .first;

  Future<void> pumpChart(WidgetTester tester) async {
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: container,
        child: MaterialApp(
          theme: buildTryptoTheme(),
          home: const Scaffold(
            body: CandleChart(request: request, baseCurrency: 'KRW'),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('진입 시 캔들을 한 번 조회하고 최신 봉을 추종한다', (tester) async {
    await pumpChart(tester);

    expect(repository.calls, 1);
    final painter = painterOf(tester);
    expect(painter.endIndex, 90);
    expect(painter.startIndex, 50);
    expect(painter.liveVisible, isFalse, reason: '실시간 봉이 아직 없다');
  });

  testWidgets('새 봉이 열리면 화면이 따라가고 실시간 봉이 보인다', (tester) async {
    await pumpChart(tester);

    store.ingest([tick(500, at(90, 5))]);
    await tester.pump();

    final painter = painterOf(tester);
    expect(painter.endIndex, 91, reason: '추종 중이면 오른쪽 끝이 마지막 봉이다');
    expect(painter.liveVisible, isTrue);
    expect(painter.visible.last.close, 500);
  });

  testWidgets('과거로 패닝해 실시간 봉이 화면 밖이면 틱이 와도 repaint 가 0이다', (tester) async {
    await pumpChart(tester);

    await tester.drag(find.byType(CandleChart), const Offset(200, 0));
    await tester.pump();

    final panned = painterOf(tester);
    expect(panned.endIndex, lessThan(90));

    store.ingest([tick(500, at(90, 5))]);
    await tester.pump();

    final after = painterOf(tester);
    expect(after.endIndex, panned.endIndex, reason: '보고 있는 구간은 흔들리지 않는다');
    expect(after.liveVisible, isFalse);
    expect(after.shouldRepaint(panned), isFalse);

    // 같은 봉에 체결이 계속 쌓여도 마찬가지다.
    final before = painterOf(tester);
    for (var i = 0; i < 100; i++) {
      store.ingest([tick(500.0 + i, at(90, 10 + i ~/ 10))]);
    }
    await tester.pump();
    expect(painterOf(tester).shouldRepaint(before), isFalse);
  });

  testWidgets('새 봉이 열리고 15초 뒤 재조회가 정확히 1회 나가고 구간이 유지된다', (tester) async {
    await pumpChart(tester);
    await tester.drag(find.byType(CandleChart), const Offset(200, 0));
    await tester.pump();
    final panned = painterOf(tester);

    store.ingest([tick(500, at(90, 5))]);
    await tester.pump();

    // 같은 봉 안의 체결은 타이머를 다시 걸지 않는다.
    for (var i = 0; i < 50; i++) {
      store.ingest([tick(500.0 + i, at(90, 10 + i ~/ 10))]);
    }
    await tester.pump(const Duration(seconds: 10));
    expect(repository.calls, 1, reason: '아직 15초가 지나지 않았다');

    await tester.pump(const Duration(seconds: 6));
    await tester.pumpAndSettle();

    expect(repository.calls, 2);
    final reconciled = painterOf(tester);
    expect(reconciled.endIndex, panned.endIndex);
    expect(reconciled.startIndex, panned.startIndex);
  });

  testWidgets('재조회가 빈 배열을 주면 실시간 봉이 자리를 지킨다', (tester) async {
    await pumpChart(tester);
    store.ingest([tick(500, at(90, 5))]);
    await tester.pump();

    repository.candles = const [];
    await tester.pump(const Duration(seconds: 16));
    await tester.pumpAndSettle();

    expect(repository.calls, 2);
    final painter = painterOf(tester);
    expect(painter.endIndex, 91);
    expect(painter.visible.last.close, 500);
  });
}

class _FakeCandleRepository implements CandleRepository {
  _FakeCandleRepository(this.candles);

  List<Candle> candles;
  int calls = 0;

  @override
  Future<List<Candle>> getCandles({
    required String exchange,
    required String coin,
    required CandleInterval interval,
    int? limit,
    DateTime? cursor,
  }) async {
    calls++;
    return normalizeCandles(candles, interval);
  }
}
