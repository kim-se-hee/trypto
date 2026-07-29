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

  /// 봉 시각은 전부 UTC 다. Dart 의 `DateTime.==` 는 `isUtc` 까지 비교하므로 로컬 시각을
  /// 섞으면 서버 봉과 실시간 봉이 같은 구간인데도 다른 봉으로 갈라진다.
  DateTime at(int hour, int minute, [int second = 0]) =>
      DateTime.utc(2026, 7, 15, hour, minute, second);

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
    /// 업비트·바이낸스는 UTC(0), 빗썸만 KST(+540)다.
    const utc = 0;
    const kst = 9 * 60;

    final sample = DateTime.utc(2026, 7, 15, 13, 47, 31, 456);

    test('간격별로 봉 시각을 절삭한다', () {
      expect(
        normalizeCandleTime(sample, CandleInterval.minute1, utc),
        DateTime.utc(2026, 7, 15, 13, 47),
      );
      expect(
        normalizeCandleTime(sample, CandleInterval.hour1, utc),
        DateTime.utc(2026, 7, 15, 13),
      );
      // 0, 4, 8, 12, 16, 20 시로 내림.
      expect(
        normalizeCandleTime(sample, CandleInterval.hour4, utc),
        DateTime.utc(2026, 7, 15, 12),
      );
      expect(
        normalizeCandleTime(sample, CandleInterval.day1, utc),
        DateTime.utc(2026, 7, 15),
      );
      expect(
        normalizeCandleTime(sample, CandleInterval.month1, utc),
        DateTime.utc(2026, 7),
      );
    });

    test('주봉은 그 주 월요일 자정이다', () {
      // 2026-07-15 는 수요일이다.
      expect(
        normalizeCandleTime(sample, CandleInterval.week1, utc),
        DateTime.utc(2026, 7, 13),
      );
      // 일요일은 6일을 되돌려 같은 주 월요일로 간다.
      expect(
        normalizeCandleTime(
          DateTime.utc(2026, 7, 19, 23),
          CandleInterval.week1,
          utc,
        ),
        DateTime.utc(2026, 7, 13),
      );
      // 월을 넘겨도 성립한다(2026-08-02 는 일요일).
      expect(
        normalizeCandleTime(
          DateTime.utc(2026, 8, 2, 5),
          CandleInterval.week1,
          utc,
        ),
        DateTime.utc(2026, 7, 27),
      );
    });

    test('서버 캔들의 UTC time 과 티커의 epoch millis 가 같은 봉으로 떨어진다', () {
      final server = DateTime.utc(2026, 7, 15, 13, 47);
      final ticked = DateTime.utc(2026, 7, 15, 13, 47, 59)
          .millisecondsSinceEpoch;

      expect(
        normalizeCandleTime(server, CandleInterval.minute1, utc),
        normalizeTickTime(ticked, CandleInterval.minute1, utc),
      );
    });

    test('업비트 일봉은 UTC 자정으로 자른다 — 09:00 KST 틱이 서버 진행봉과 같은 봉이다', () {
      // 서버가 내려주는 2026-07-29 일봉의 time.
      final bucket = DateTime.utc(2026, 7, 29);
      // 09:00:01 KST = 00:00:01 UTC. 단말 로컬로 자르면 07-29 로 떨어져 서버와 어긋난다.
      final ticked = DateTime.utc(2026, 7, 29, 0, 0, 1).millisecondsSinceEpoch;

      expect(normalizeCandleTime(bucket, CandleInterval.day1, utc), bucket);
      expect(normalizeTickTime(ticked, CandleInterval.day1, utc), bucket);

      // 08:59 KST(= 전날 23:59 UTC)는 아직 전날 봉이다.
      expect(
        normalizeTickTime(
          DateTime.utc(2026, 7, 28, 23, 59).millisecondsSinceEpoch,
          CandleInterval.day1,
          utc,
        ),
        DateTime.utc(2026, 7, 28),
      );
    });

    test('빗썸 일봉은 KST 자정으로 자른다', () {
      // 2026-07-29 00:00 KST = 2026-07-28T15:00Z.
      final bucket = DateTime.utc(2026, 7, 28, 15);

      expect(normalizeCandleTime(bucket, CandleInterval.day1, kst), bucket);
      // 같은 날 정오 KST(03:00Z)도 같은 봉이다.
      expect(
        normalizeCandleTime(
          DateTime.utc(2026, 7, 29, 3),
          CandleInterval.day1,
          kst,
        ),
        bucket,
      );
      // 1분 전(23:59 KST)은 전날 봉이다.
      expect(
        normalizeCandleTime(
          DateTime.utc(2026, 7, 28, 14, 59),
          CandleInterval.day1,
          kst,
        ),
        DateTime.utc(2026, 7, 27, 15),
      );
    });

    test('4시간봉도 거래소 기준 시간대에서 0·4·8·12·16·20 시로 내림한다', () {
      // 업비트: 05:30Z → 04:00Z.
      expect(
        normalizeCandleTime(
          DateTime.utc(2026, 7, 29, 5, 30),
          CandleInterval.hour4,
          utc,
        ),
        DateTime.utc(2026, 7, 29, 4),
      );
      // 빗썸: 14:30Z = 23:30 KST → 20:00 KST = 11:00Z.
      expect(
        normalizeCandleTime(
          DateTime.utc(2026, 7, 29, 14, 30),
          CandleInterval.hour4,
          kst,
        ),
        DateTime.utc(2026, 7, 29, 11),
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
        candle(DateTime.utc(2026, 7, 15, 10, 1, 40), 105, 115, 100, 108),
        candle(DateTime.utc(2026, 7, 15, 10, 0, 20), 100, 110, 90, 105),
        candle(
          DateTime.utc(2026, 7, 15, 10, 2),
          108,
          double.infinity,
          100,
          110,
        ),
      ], CandleInterval.minute1, 0);

      expect(result, hasLength(2));
      expect(result.first.time, at(10, 0));
      expect(result.last.time, at(10, 1));
    });
  });

  group('mergeReconciled', () {
    test('최신 창만 서버 값으로 덮고 앞에 쌓아 둔 과거 구간은 남긴다', () {
      final existing = [
        candle(at(9, 58), 1, 1, 1, 1),
        candle(at(9, 59), 2, 2, 2, 2),
        candle(at(10, 0), 3, 3, 3, 3),
      ];
      final fresh = [
        candle(at(10, 0), 30, 30, 30, 30),
        candle(at(10, 1), 40, 40, 40, 40),
      ];

      final merged = mergeReconciled(existing, fresh);

      expect(merged.map((c) => c.time).toList(), [
        at(9, 58),
        at(9, 59),
        at(10, 0),
        at(10, 1),
      ]);
      // 겹치는 구간은 서버 값이 이긴다.
      expect(merged[2].close, 30);
    });

    test('빈 응답은 기존 배열을 그대로 둔다', () {
      final existing = [candle(at(10, 0), 3, 3, 3, 3)];
      expect(identical(mergeReconciled(existing, const []), existing), isTrue);
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
    store.addRawObserver(folder);

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
