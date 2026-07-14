import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/realtime/ticker_store.dart';
import 'package:trypto/core/theme/theme.dart';
import 'package:trypto/features/market/coin_row.dart';
import 'package:trypto/models/exchange_coin.dart';
import 'package:trypto/models/ticker.dart';

/// 설계의 핵심 주장을 코드로 못 박는 유일한 장치(계획서 §7-7).
///
/// 한 심볼에만 틱이 오면 **그 행의 숫자 위젯만 다시 만들어진다.** 위젯 인스턴스는 빌드마다
/// 새로 생성되므로, 인스턴스 동일성이 곧 "리빌드하지 않았다"는 증거다.
void main() {
  ExchangeCoin coin(String symbol, double price) => ExchangeCoin(
    exchangeCoinId: 1,
    coinId: 1,
    coinSymbol: symbol,
    coinName: symbol,
    price: price,
    changeRate: 0,
    volume: 1000,
  );

  Ticker tick(String symbol, double price, int timestamp) => Ticker(
    coinId: 1,
    symbol: symbol,
    price: price,
    changeRate: 0.01,
    quoteTurnover: 2000,
    timestamp: timestamp,
  );

  late TickerStore store;

  setUp(() {
    store = TickerStore()
      ..switchExchange([coin('BTC', 100000), coin('ETH', 5000)]);
  });

  tearDown(() => store.dispose());

  CoinNumbers numbersOf(WidgetTester tester, String symbol) => tester.widget<CoinNumbers>(
    find.descendant(
      of: find.byWidgetPredicate(
        (widget) => widget is CoinRow && widget.symbol == symbol,
      ),
      matching: find.byType(CoinNumbers),
    ),
  );

  Future<void> pumpRows(WidgetTester tester) => tester.pumpWidget(
    MaterialApp(
      theme: buildTryptoTheme(),
      home: Scaffold(
        body: ListView(
          itemExtent: kCoinRowHeight,
          children: [
            for (final symbol in ['BTC', 'ETH'])
              CoinRow(
                symbol: symbol,
                name: symbol,
                row: store.row(symbol)!,
                flash: store.flash(symbol)!,
                baseCurrency: 'KRW',
              ),
          ],
        ),
      ),
    ),
  );

  testWidgets('틱이 온 행의 숫자만 다시 빌드되고 이웃 행은 그대로다', (tester) async {
    await pumpRows(tester);

    final btcBefore = numbersOf(tester, 'BTC');
    final ethBefore = numbersOf(tester, 'ETH');

    store.ingest([tick('BTC', 123456, 1)]);
    await tester.pump();

    final btcAfter = numbersOf(tester, 'BTC');
    final ethAfter = numbersOf(tester, 'ETH');

    expect(identical(btcBefore, btcAfter), isFalse);
    expect(identical(ethBefore, ethAfter), isTrue);
    expect(btcAfter.price, 123456);
    expect(find.text('₩123,456'), findsOneWidget);
  });

  testWidgets('플래시 테두리는 숫자 위젯을 다시 만들지 않는다', (tester) async {
    await pumpRows(tester);

    store.ingest([tick('BTC', 123456, 1)]);
    await tester.pump();

    final numbers = numbersOf(tester, 'BTC');
    expect(store.flash('BTC')!.value, isNull);

    // 같은 가격에 재체결 — 숫자는 그대로이고 테두리만 깜빡인다.
    store.ingest([tick('BTC', 123456, 2)]);
    await tester.pump();

    expect(store.flash('BTC')!.value, FlashDir.same);
    expect(identical(numbers, numbersOf(tester, 'BTC')), isTrue);

    await tester.pump(const Duration(milliseconds: 150));
    expect(store.flash('BTC')!.value, isNull);
    expect(tester.takeException(), isNull);
  });
}
