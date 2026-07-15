import 'package:decimal/decimal.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/constants/exchanges.dart';
import 'package:trypto/features/market/market_controller.dart';
import 'package:trypto/features/market/order_form.dart';
import 'package:trypto/features/market/order_target.dart';
import 'package:trypto/models/enums.dart';
import 'package:trypto/models/exchange_coin.dart';

/// 계획서 §7-⑤ — `MARKET+BUY` 는 `volume` 금지·`price` = 총액, `MARKET+SELL` 은 `price` 금지.
/// 4조합 + 각 위반. 틀리면 서버가 400 이다.
void main() {
  final upbit = ExchangeIds.byId(ExchangeIds.upbit)!;
  final binance = ExchangeIds.byId(ExchangeIds.binance)!;

  Decimal dec(String value) => Decimal.parse(value);

  OrderContext ctx({
    Exchange? exchange,
    String price = '100000000',
    String buy = '10000000',
    String sell = '2',
  }) => OrderContext(
    exchange: exchange ?? upbit,
    currentPrice: dec(price),
    availableBuy: dec(buy),
    availableSell: dec(sell),
  );

  const target = OrderTarget(
    exchange: Exchange(
      id: 1,
      key: 'upbit',
      name: '업비트',
      baseCurrency: 'KRW',
      feeRate: 0.0005,
      candleCode: 'UPBIT',
    ),
    symbol: 'BTC',
    walletId: 100,
    exchangeCoinId: 7,
  );

  CoinEntry entry(int id, String symbol) => CoinEntry(
    ExchangeCoin(
      exchangeCoinId: id,
      coinId: id,
      coinSymbol: symbol,
      coinName: symbol,
      price: 1,
      changeRate: 0,
      volume: 0,
    ),
  );

  group('주문 대상 해석', () {
    test('라운드가 없으면 NO_ROUND 다', () {
      final result = resolveOrderTarget(
        exchange: upbit,
        symbol: 'BTC',
        walletId: null,
        coins: [entry(7, 'BTC')],
      );

      expect(result.target, isNull);
      expect(result.failure, OrderTargetFailure.noRound);
    });

    test('코인 목록 조회가 실패하면 LOOKUP_FAILED 다', () {
      final result = resolveOrderTarget(
        exchange: upbit,
        symbol: 'BTC',
        walletId: 100,
        coins: null,
      );

      expect(result.failure, OrderTargetFailure.lookupFailed);
    });

    test('상장 목록에 없으면 COIN_UNLISTED 다', () {
      final result = resolveOrderTarget(
        exchange: upbit,
        symbol: 'DOGE',
        walletId: 100,
        coins: [entry(7, 'BTC')],
      );

      expect(result.failure, OrderTargetFailure.coinUnlisted);
    });

    test('심볼은 대소문자를 무시하고 매칭한다', () {
      final result = resolveOrderTarget(
        exchange: upbit,
        symbol: 'btc',
        walletId: 100,
        coins: [entry(7, 'BTC')],
      );

      expect(result.target?.exchangeCoinId, 7);
      expect(result.target?.walletId, 100);
      // 어느 (거래소, 코인) 의 것인지 함께 들고 다닌다.
      expect(result.target?.key, 'upbit:BTC');
    });
  });

  group('바디 조립', () {
    test('시장가 매수 — volume 이 없고 price 에 총액이 실린다', () {
      final form = OrderForm.empty(
        side: Side.buy,
        orderType: OrderType.market,
      ).withTotal(dec('50000'), ctx());

      final json = form
          .toRequest(target: target, clientOrderId: 'key')
          .toJson();

      expect(json.containsKey('volume'), isFalse);
      expect(json['price'], 50000.0);
      expect(json['orderType'], 'MARKET');
      expect(json['side'], 'BUY');
      expect(json['walletId'], 100);
      expect(json['exchangeCoinId'], 7);
    });

    test('시장가 매도 — price 가 없고 volume 에 수량이 실린다', () {
      final form = OrderForm.empty(
        side: Side.sell,
        orderType: OrderType.market,
      ).withQuantity(dec('0.005'), ctx());

      final json = form
          .toRequest(target: target, clientOrderId: 'key')
          .toJson();

      expect(json.containsKey('price'), isFalse);
      expect(json['volume'], 0.005);
    });

    test('지정가 매수 — price 는 지정가고 volume 은 수량이다', () {
      final form = OrderForm.empty(side: Side.buy)
          .withPrice(dec('95000000'), ctx())
          .withQuantity(dec('0.001'), ctx());

      final json = form
          .toRequest(target: target, clientOrderId: 'key')
          .toJson();

      expect(json['price'], 95000000.0);
      expect(json['volume'], 0.001);
      expect(json['orderType'], 'LIMIT');
    });

    test('지정가 매도 — price·volume 을 모두 보낸다', () {
      final form = OrderForm.empty(side: Side.sell)
          .withPrice(dec('95000000'), ctx())
          .withQuantity(dec('0.5'), ctx());

      final json = form
          .toRequest(target: target, clientOrderId: 'key')
          .toJson();

      expect(json['price'], 95000000.0);
      expect(json['volume'], 0.5);
      expect(json['side'], 'SELL');
    });

    test('시장가 매수로 바꿔도 남아 있던 수량이 volume 으로 새지 않는다', () {
      final form = OrderForm.empty(side: Side.buy)
          .withPrice(dec('95000000'), ctx())
          .withQuantity(dec('0.001'), ctx())
          .withOrderType(OrderType.market, ctx());

      final json = form
          .toRequest(target: target, clientOrderId: 'key')
          .toJson();

      expect(json.containsKey('volume'), isFalse);
      // 지정가에서 연동된 총액(0.001 × 95,000,000)이 그대로 총 주문 금액이 된다.
      expect(json['price'], 95000.0);
    });

    test('소수 8자리 수량이 double 왕복에서 뭉개지지 않는다', () {
      final form = OrderForm.empty(
        side: Side.sell,
        orderType: OrderType.market,
      ).withQuantity(dec('0.00000001'), ctx());

      final json = form
          .toRequest(target: target, clientOrderId: 'key')
          .toJson();

      expect(json['volume'], 1e-8);
    });
  });

  group('수량↔총액 연동', () {
    test('지정가에서 수량을 넣으면 총액이 따라온다', () {
      final form = OrderForm.empty(side: Side.buy)
          .withPrice(dec('95000000'), ctx())
          .withQuantity(dec('0.001'), ctx());

      expect(form.total, dec('95000'));
    });

    test('지정가에서 총액을 넣으면 수량이 6자리로 따라온다', () {
      final form = OrderForm.empty(side: Side.buy)
          .withPrice(dec('95000000'), ctx())
          .withTotal(dec('100000'), ctx());

      // 100,000 / 95,000,000 = 0.00105263… → 6자리 반올림
      expect(form.quantity, dec('0.001053'));
    });

    test('가격이 바뀌면 마지막으로 손댄 쪽을 기준으로 반대편을 다시 계산한다', () {
      final form = OrderForm.empty(side: Side.buy)
          .withPrice(dec('95000000'), ctx())
          .withQuantity(dec('0.001'), ctx())
          .withPrice(dec('90000000'), ctx());

      expect(form.quantity, dec('0.001'));
      expect(form.total, dec('90000'));
    });

    test('시장가에서는 연동하지 않는다', () {
      final form = OrderForm.empty(
        side: Side.buy,
        orderType: OrderType.market,
      ).withTotal(dec('50000'), ctx());

      expect(form.quantity, Decimal.zero);
    });

    test('가격이 비어 있으면 스텝 버튼의 기준값은 현재가이고 0 아래로 내려가지 않는다', () {
      final context = ctx(price: '500');
      final up = OrderForm.empty(side: Side.buy).stepPrice(1, context);
      final down = OrderForm.empty(side: Side.buy).stepPrice(-1, context);

      expect(up.price, dec('1500'));
      expect(down.price, Decimal.zero);
    });
  });

  group('비율 버튼', () {
    test('매수 50% 는 주문 가능 금액의 절반이다', () {
      final form = OrderForm.empty(
        side: Side.buy,
        orderType: OrderType.market,
      ).applyRatio(50, ctx());

      expect(form.total, dec('5000000'));
    });

    test('매수 100% 는 수수료까지 내고 살 수 있는 최대치로 가둔다', () {
      final context = ctx();
      final form = OrderForm.empty(
        side: Side.buy,
        orderType: OrderType.market,
      ).applyRatio(100, context);

      // 10,000,000 을 그대로 채우면 수수료가 위에 더 붙어 서버가 INSUFFICIENT_BALANCE 를 낸다.
      expect(form.total < dec('10000000'), isTrue);
      expect(form.validate(context), isNull);
    });

    test('매도 100% 는 보유 수량 전량이다', () {
      final form = OrderForm.empty(
        side: Side.sell,
        orderType: OrderType.market,
      ).applyRatio(100, ctx(sell: '1.23456789'));

      // 수량은 6자리로 내림한다 — 올리면 보유 수량을 넘는다.
      expect(form.quantity, dec('1.234567'));
    });
  });

  group('검증', () {
    test('시장가 매수는 총액을, 나머지는 수량을 먼저 본다', () {
      final buy = OrderForm.empty(side: Side.buy, orderType: OrderType.market);
      final sell = OrderForm.empty(
        side: Side.sell,
        orderType: OrderType.market,
      );

      expect(buy.validate(ctx()), '주문 총액을 입력해 주세요.');
      expect(sell.validate(ctx()), '주문 수량을 입력해 주세요.');
    });

    test('지정가는 가격이 없으면 막는다', () {
      final form = OrderForm.empty(
        side: Side.buy,
      ).withQuantity(dec('0.001'), ctx());

      expect(form.validate(ctx()), '지정가를 입력해 주세요.');
    });

    test('최소 주문 금액을 밑돌면 막는다 — KRW 5,000', () {
      final form = OrderForm.empty(
        side: Side.buy,
        orderType: OrderType.market,
      ).withTotal(dec('4999'), ctx());

      expect(form.validate(ctx()), '최소 주문 금액은 5,000 KRW 입니다.');
    });

    test('최소 주문 금액은 거래소 기준통화를 따른다 — USDT 5', () {
      final context = ctx(exchange: binance, price: '60000', buy: '1000');
      final form = OrderForm.empty(
        side: Side.buy,
        orderType: OrderType.market,
      ).withTotal(dec('4'), context);

      // 웹은 하드코딩이라 바이낸스에서도 "최소 주문 5,000 USDT" 로 나온다.
      expect(form.validate(context), '최소 주문 금액은 5.00 USDT 입니다.');
    });

    test('KRW 상한 10억을 넘으면 막는다', () {
      final context = ctx(buy: '2000000000');
      final form = OrderForm.empty(
        side: Side.buy,
        orderType: OrderType.market,
      ).withTotal(dec('1000000001'), context);

      expect(form.validate(context), '최대 주문 금액은 1,000,000,000 KRW 입니다.');
    });

    test('매수는 수수료를 포함해 잔고를 검사한다', () {
      final context = ctx(buy: '100000');
      final form = OrderForm.empty(
        side: Side.buy,
        orderType: OrderType.market,
      ).withTotal(dec('100000'), context);

      // 차감액 = 100,000 × (1 + 0.0005) = 100,050 > 100,000
      expect(form.validate(context), startsWith('주문 가능 금액을 초과했습니다.'));
    });

    test('매도는 보유 수량을 넘으면 막는다', () {
      final context = ctx(sell: '0.5');
      final form = OrderForm.empty(
        side: Side.sell,
        orderType: OrderType.market,
      ).withQuantity(dec('0.6'), context);

      expect(form.validate(context), '보유 수량을 초과했습니다.');
    });

    test('예상 수수료는 주문 총액 × 거래소 수수료율이다', () {
      final context = ctx();
      final form = OrderForm.empty(
        side: Side.buy,
        orderType: OrderType.market,
      ).withTotal(dec('1000000'), context);

      // 업비트 0.05%
      expect(form.feeOf(context), dec('500'));
    });

    test('빗썸은 0.25% 다 — 수수료율은 거래소마다 다르다', () {
      final context = ctx(exchange: ExchangeIds.byId(ExchangeIds.bithumb)!);
      final form = OrderForm.empty(
        side: Side.buy,
        orderType: OrderType.market,
      ).withTotal(dec('1000000'), context);

      expect(form.feeOf(context), dec('2500'));
    });
  });

  group('입력 파싱', () {
    test('콤마를 제거하고, 숫자가 아니면 0 으로 본다', () {
      expect(parseAmountInput('1,234,567'), dec('1234567'));
      expect(parseAmountInput(''), Decimal.zero);
      expect(parseAmountInput('.'), Decimal.zero);
      expect(parseAmountInput('0.00000001'), dec('0.00000001'));
    });
  });
}
