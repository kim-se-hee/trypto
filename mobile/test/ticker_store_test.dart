import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/realtime/ticker_store.dart';
import 'package:trypto/models/exchange_coin.dart';
import 'package:trypto/models/ticker.dart';

/// 성능 계약(계획서 §7-3). 프레임 드랍은 눈으로 회귀를 잡을 수 없다. 불변식을 고정한다.
void main() {
  ExchangeCoin coin(String symbol, {double price = 100}) => ExchangeCoin(
    exchangeCoinId: 1,
    coinId: 1,
    coinSymbol: symbol,
    coinName: symbol,
    price: price,
    changeRate: 0,
    volume: 0,
  );

  Ticker tick(
    String symbol, {
    required double price,
    required int timestamp,
    double changeRate = 0,
    double quoteTurnover = 0,
  }) => Ticker(
    coinId: 1,
    symbol: symbol,
    price: price,
    changeRate: changeRate,
    quoteTurnover: quoteTurnover,
    timestamp: timestamp,
  );

  late TickerStore store;

  setUp(() {
    store = TickerStore()..switchExchange([coin('BTC'), coin('ETH')]);
  });

  tearDown(() => store.dispose());

  testWidgets('1,000틱을 ingest 해도 flush 전에는 알림이 0회다', (tester) async {
    var notifications = 0;
    store.row('BTC')!.addListener(() => notifications++);

    for (var i = 0; i < 1000; i++) {
      store.ingest([tick('BTC', price: 100.0 + i, timestamp: i)]);
    }
    expect(notifications, 0);

    await tester.pump();
    expect(notifications, 1);
  });

  testWidgets('같은 프레임의 같은 심볼은 마지막 값만 남는다', (tester) async {
    store.ingest([
      tick('BTC', price: 100, timestamp: 1),
      tick('BTC', price: 130, timestamp: 2),
      tick('BTC', price: 110, timestamp: 3),
    ]);
    await tester.pump();

    expect(store.row('BTC')!.value.price, 110);
  });

  testWidgets('목록 밖 심볼은 버린다', (tester) async {
    var notifications = 0;
    store.row('BTC')!.addListener(() => notifications++);

    store.ingest([tick('DOGE', price: 1, timestamp: 1)]);
    await tester.pump();

    expect(store.row('DOGE'), isNull);
    expect(notifications, 0);
  });

  testWidgets('값이 바뀌지 않은 심볼에는 알림이 가지 않는다', (tester) async {
    store.ingest([tick('BTC', price: 100, timestamp: 1)]);
    await tester.pump();

    var notifications = 0;
    store.row('BTC')!.addListener(() => notifications++);

    // 같은 가격에 재체결 — tickedAt 은 바뀌지만 CoinRowState 는 같다.
    store.ingest([tick('BTC', price: 100, timestamp: 2)]);
    await tester.pump();

    expect(notifications, 0);
  });

  testWidgets('리스너 없는 심볼은 플래시를 계산하지 않는다', (tester) async {
    store.ingest([tick('BTC', price: 100, timestamp: 1)]);
    await tester.pump();
    store.ingest([tick('BTC', price: 120, timestamp: 2)]);
    await tester.pump();

    expect(store.flash('BTC')!.value, isNull);
  });

  testWidgets('플래시는 방향을 판정하고 100ms 뒤 스스로 꺼진다', (tester) async {
    final flash = store.flash('BTC')!;
    final seen = <FlashDir?>[];
    flash.addListener(() => seen.add(flash.value));

    store.ingest([tick('BTC', price: 100, timestamp: 1)]);
    await tester.pump();
    expect(flash.value, isNull, reason: '첫 틱은 직전 tickedAt 이 없어 깜빡이지 않는다');

    store.ingest([tick('BTC', price: 120, timestamp: 2)]);
    await tester.pump();
    expect(flash.value, FlashDir.up);

    store.ingest([tick('BTC', price: 90, timestamp: 3)]);
    await tester.pump();
    expect(flash.value, FlashDir.down);

    // 같은 가격 재체결도 tickedAt 이 바뀌면 깜빡인다(사양서 §3.3.3).
    store.ingest([tick('BTC', price: 90, timestamp: 4)]);
    await tester.pump();
    expect(flash.value, FlashDir.same);

    // 틱이 끊겨도 잔여 플래시가 꺼진다 — 심볼별 Timer 없이 flush 루프가 쓸어 담는다.
    await tester.pump(const Duration(milliseconds: 150));
    expect(flash.value, isNull);
    expect(seen.last, isNull);
  });

  testWidgets('거래소를 전환하면 버퍼와 notifier 가 비워진다', (tester) async {
    store.ingest([tick('BTC', price: 100, timestamp: 1)]);
    store.switchExchange([coin('XRP', price: 700)]);
    await tester.pump();

    expect(store.row('BTC'), isNull);
    expect(store.row('XRP')!.value.price, 700);
    expect(store.orderDirty, isFalse);
  });

  testWidgets('비활성 상태에서는 틱을 받지 않는다', (tester) async {
    store.setActive(false);
    var notifications = 0;
    store.row('BTC')!.addListener(() => notifications++);

    store.ingest([tick('BTC', price: 999, timestamp: 1)]);
    await tester.pump();

    expect(notifications, 0);
    expect(store.row('BTC')!.value.price, 100);
  });

  testWidgets('관찰자는 매 틱 동기 호출되고 그리기 알림은 프레임당 1회다', (tester) async {
    final observer = _RecordingObserver();
    store.setRawObserver(observer);

    store.ingest([
      tick('BTC', price: 100, timestamp: 1),
      tick('BTC', price: 130, timestamp: 2),
    ]);
    store.ingest([
      tick('BTC', price: 90, timestamp: 3),
      tick('BTC', price: 110, timestamp: 4),
    ]);

    // 접기는 프레임을 기다리지 않는다. 기다리면 한 프레임 안의 체결이 사라진다.
    expect(observer.ticks.map((t) => t.price).toList(), [100, 130, 90, 110]);
    expect(observer.frames, 0);

    await tester.pump();
    expect(observer.frames, 1);

    store.clearRawObserver(observer);
    store.ingest([tick('BTC', price: 500, timestamp: 5)]);
    await tester.pump();
    expect(observer.ticks.length, 4);
    expect(observer.frames, 1);
  });

  testWidgets('관찰자를 등록하지 않으면 목록 경로는 그대로다', (tester) async {
    var notifications = 0;
    store.row('BTC')!.addListener(() => notifications++);

    for (var i = 0; i < 500; i++) {
      store.ingest([tick('BTC', price: 100.0 + i, timestamp: i)]);
      store.ingest([tick('ETH', price: 50.0 + i, timestamp: i)]);
    }
    await tester.pump();

    expect(notifications, 1);
    expect(store.row('BTC')!.value.price, 599);
    expect(store.row('ETH')!.value.price, 549);
  });

  test('decodeTickers 는 배열만 받아들이고 실패를 조용히 무시한다', () {
    expect(decodeTickers('not json'), isEmpty);
    expect(decodeTickers('{"symbol":"BTC"}'), isEmpty);
    expect(decodeTickers('[]'), isEmpty);

    final parsed = decodeTickers(
      '[{"coinId":1,"symbol":"BTC","price":152340000,'
      '"changeRate":0.0123,"quoteTurnover":8423199301.55,'
      '"timestamp":1735689600123}]',
    );
    expect(parsed, hasLength(1));
    expect(parsed.single.price, 152340000);
    expect(parsed.single.timestamp, 1735689600123);
  });

  test('CoinRowState 의 ==/hashCode 는 성능 계약이다', () {
    const a = CoinRowState(1, 2, 3);
    const b = CoinRowState(1, 2, 3);
    const c = CoinRowState(1, 2, 4);

    expect(a, b);
    expect(a.hashCode, b.hashCode);
    expect(a, isNot(c));
  });
}

class _RecordingObserver implements TickObserver {
  final List<Ticker> ticks = [];
  int frames = 0;

  @override
  void onTick(Ticker tick) => ticks.add(tick);

  @override
  void onFrame() => frames++;
}
