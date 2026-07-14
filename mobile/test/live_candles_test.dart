import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/realtime/ticker_store.dart';
import 'package:trypto/features/market/live_candles.dart';
import 'package:trypto/models/candle.dart';
import 'package:trypto/models/enums.dart';
import 'package:trypto/models/exchange_coin.dart';
import 'package:trypto/models/ticker.dart';

/// 10단위 완료 조건 ①②(계획서 §6). 봉 접기와 합성은 위젯 없이 고정한다.
void main() {
  const request = CandleRequest(
    exchangeCode: 'UPBIT',
    symbol: 'BTC',
    interval: CandleInterval.minute1,
  );

  DateTime at(int hour, int minute, [int second = 0]) =>
      DateTime(2026, 7, 15, hour, minute, second);

  Ticker tick(double price, DateTime time, {String symbol = 'BTC'}) => Ticker(
    coinId: 1,
    symbol: symbol,
    price: price,
    changeRate: 0,
    quoteTurnover: 0,
    timestamp: time.millisecondsSinceEpoch,
  );

  Candle candle(
    DateTime time,
    double open,
    double high,
    double low,
    double close,
  ) => Candle(time: time, open: open, high: high, low: low, close: close);

  group('normalizeCandleTime', () {
    final sample = DateTime(2026, 7, 15, 13, 47, 31, 456);

    test('간격별로 봉 시각을 절삭한다', () {
      expect(
        normalizeCandleTime(sample, CandleInterval.minute1),
        DateTime(2026, 7, 15, 13, 47),
      );
      expect(
        normalizeCandleTime(sample, CandleInterval.hour1),
        DateTime(2026, 7, 15, 13),
      );
      // 0, 4, 8, 12, 16, 20 시로 내림.
      expect(
        normalizeCandleTime(sample, CandleInterval.hour4),
        DateTime(2026, 7, 15, 12),
      );
      expect(
        normalizeCandleTime(sample, CandleInterval.day1),
        DateTime(2026, 7, 15),
      );
      expect(
        normalizeCandleTime(sample, CandleInterval.month1),
        DateTime(2026, 7),
      );
    });

    test('주봉은 그 주 월요일 자정이다', () {
      // 2026-07-15 는 수요일이다.
      expect(
        normalizeCandleTime(sample, CandleInterval.week1),
        DateTime(2026, 7, 13),
      );
      // 일요일은 6일을 되돌려 같은 주 월요일로 간다.
      expect(
        normalizeCandleTime(DateTime(2026, 7, 19, 23), CandleInterval.week1),
        DateTime(2026, 7, 13),
      );
      // 월을 넘겨도 성립한다(2026-08-02 는 일요일).
      expect(
        normalizeCandleTime(DateTime(2026, 8, 2, 5), CandleInterval.week1),
        DateTime(2026, 7, 27),
      );
    });

    test('서버 캔들의 UTC time 과 티커의 epoch millis 가 같은 봉으로 떨어진다', () {
      final server = DateTime(2026, 7, 15, 13, 47).toUtc();
      final ticked = DateTime(2026, 7, 15, 13, 47, 59).millisecondsSinceEpoch;

      expect(
        normalizeCandleTime(server, CandleInterval.minute1),
        normalizeTickTime(ticked, CandleInterval.minute1),
      );
    });
  });

  group('LiveCandleFolder.fold', () {
    test('한 봉 안의 체결 [100, 130, 90, 110] 이 high=130 low=90 close=110 이 된다', () {
      final folder = LiveCandleFolder(request);
      final bucket = at(10, 0);

      for (final price in [100.0, 130.0, 90.0, 110.0]) {
        folder.onTick(tick(price, bucket.add(const Duration(seconds: 5))));
      }

      expect(folder.live, hasLength(1));
      final live = folder.live.single;
      expect(live.open, 100);
      expect(live.high, 130);
      expect(live.low, 90);
      expect(live.close, 110);
    });

    test('이미 닫힌 봉에 뒤늦게 도착한 체결은 버린다', () {
      final folder = LiveCandleFolder(request);
      folder.onTick(tick(100, at(10, 0)));
      folder.onTick(tick(200, at(10, 1)));

      folder.onTick(tick(999, at(10, 0, 30)));

      expect(folder.live, hasLength(2));
      expect(folder.live.first.close, 100);
      expect(folder.live.last.close, 200);
    });

    test('새 봉은 네 값이 모두 그 체결가이고 최근 4개만 남는다', () {
      final folder = LiveCandleFolder(request);
      for (var minute = 0; minute < 6; minute++) {
        folder.onTick(tick(100.0 + minute, at(10, minute)));
      }

      expect(folder.live, hasLength(kLiveCandleLimit));
      expect(folder.live.first.bucket, at(10, 2));
      final last = folder.live.last;
      expect([last.open, last.high, last.low, last.close], [105, 105, 105, 105]);
    });

    test('다른 심볼과 유효하지 않은 가격은 접지 않는다', () {
      final folder = LiveCandleFolder(request);
      folder.onTick(tick(100, at(10, 0), symbol: 'ETH'));
      folder.onTick(tick(0, at(10, 0)));
      folder.onTick(tick(double.nan, at(10, 0)));

      expect(folder.live, isEmpty);
    });

    test('openedBucket 은 봉이 바뀔 때만 발화한다 — 재조정 타이머가 틱마다 재무장하지 않는다', () {
      final folder = LiveCandleFolder(request);
      var fired = 0;
      folder.openedBucket.addListener(() => fired++);

      for (var i = 0; i < 50; i++) {
        folder.onTick(tick(100.0 + i, at(10, 0, i)));
      }
      expect(fired, 1);

      folder.onTick(tick(300, at(10, 1)));
      expect(fired, 2);
    });
  });

  group('MergedCandles', () {
    test('서버 캔들이 기준이고 같은 봉은 서버 open + 실시간 high/low/close 다', () {
      final server = [
        candle(at(10, 0), 100, 110, 90, 105),
        candle(at(10, 1), 105, 115, 100, 108),
      ];
      final folder = LiveCandleFolder(request);
      folder.onTick(tick(120, at(10, 1, 10)));
      folder.onTick(tick(95, at(10, 1, 20)));
      folder.onTick(tick(112, at(10, 1, 30)));

      final merged = MergedCandles(server, folder.live);
      expect(merged.length, 2);

      final last = merged[1];
      expect(last.open, 105, reason: '시가는 서버 값이다');
      expect(last.high, 120);
      expect(last.low, 95);
      expect(last.close, 112, reason: '종가는 최신 체결가다');

      // 과거 봉은 서버 값 그대로다.
      expect(merged[0].close, 105);
    });

    test('서버보다 미래의 봉은 뒤에 붙이고 이전 봉은 무시한다', () {
      final server = [candle(at(10, 1), 105, 115, 100, 108)];
      final folder = LiveCandleFolder(request);
      // 서버가 이미 확정한 과거 구간 — 재조정 뒤 남아 있는 낡은 실시간 봉이다.
      folder.onTick(tick(999, at(10, 0)));
      folder.onTick(tick(130, at(10, 2)));
      folder.onTick(tick(140, at(10, 3)));

      final merged = MergedCandles(server, folder.live);

      expect(merged.length, 3);
      expect(merged[0].close, 108);
      expect(merged[1].time, at(10, 2));
      expect(merged[2].close, 140);
      expect(merged.liveFromIndex, 1);
    });

    test('서버 캔들이 0개여도 실시간 봉만으로 그린다', () {
      final folder = LiveCandleFolder(request);
      folder.onTick(tick(100, at(10, 0)));
      folder.onTick(tick(130, at(10, 1)));

      final merged = MergedCandles(const [], folder.live);

      expect(merged.length, 2);
      expect(merged[0].open, 100);
      expect(merged[1].close, 130);
      expect(merged.liveFromIndex, 0);
    });

    test('실시간 봉이 없으면 서버 배열 그대로다', () {
      final server = [candle(at(10, 0), 100, 110, 90, 105)];
      final merged = MergedCandles(server, const []);

      expect(merged.length, 1);
      expect(identical(merged[0], server[0]), isTrue);
      expect(merged.liveFromIndex, 1, reason: '반영되는 실시간 봉이 없다');
    });

    test('실시간 봉이 전부 과거면 반영 지점이 없다', () {
      final server = [
        candle(at(10, 5), 100, 110, 90, 105),
        candle(at(10, 6), 105, 115, 100, 108),
      ];
      final folder = LiveCandleFolder(request);
      folder.onTick(tick(999, at(10, 1)));

      final merged = MergedCandles(server, folder.live);

      expect(merged.length, 2);
      expect(merged.liveFromIndex, merged.length);
    });
  });

  group('normalizeCandles', () {
    test('유한하지 않은 캔들을 버리고 봉 시각을 절삭해 오름차순 정렬한다', () {
      final result = normalizeCandles([
        candle(DateTime(2026, 7, 15, 10, 1, 40), 105, 115, 100, 108),
        candle(DateTime(2026, 7, 15, 10, 0, 20), 100, 110, 90, 105),
        candle(DateTime(2026, 7, 15, 10, 2), 108, double.infinity, 100, 110),
      ], CandleInterval.minute1);

      expect(result, hasLength(2));
      expect(result.first.time, at(10, 0));
      expect(result.last.time, at(10, 1));
    });
  });

  /// **프레임 버퍼로 접으면 실패하는 테스트다.** 한 프레임 안에 들어온 네 체결을 마지막 값만
  /// 보고 접으면 high=low=close=110 이 된다.
  testWidgets('TickerStore 를 통과한 한 프레임의 체결이 봉의 고가·저가를 지킨다', (tester) async {
    final store = TickerStore()
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
    addTearDown(store.dispose);

    final folder = LiveCandleFolder(request);
    store.setRawObserver(folder);

    final bucket = at(10, 0);
    for (final price in [100.0, 130.0, 90.0, 110.0]) {
      store.ingest([tick(price, bucket.add(const Duration(seconds: 1)))]);
    }
    // 아직 프레임을 밀지 않았다. 접기는 이미 끝나 있어야 한다.
    expect(folder.live.single.high, 130);
    expect(folder.live.single.low, 90);
    expect(folder.live.single.close, 110);
    expect(folder.revision.value, 0);

    await tester.pump();
    expect(folder.revision.value, 1, reason: '그리기 알림은 프레임당 1회다');

    // 같은 프레임에 다시 100틱이 들어와도 알림은 한 번뿐이다.
    for (var i = 0; i < 100; i++) {
      store.ingest([
        tick(120.0 + i, bucket.add(Duration(milliseconds: 2000 + i * 10))),
      ]);
    }
    await tester.pump();
    expect(folder.revision.value, 2);
    expect(folder.live.single.high, 219);
  });
}
