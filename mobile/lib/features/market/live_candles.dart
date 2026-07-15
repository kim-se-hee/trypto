import 'dart:math' as math;

import 'package:flutter/foundation.dart';

import '../../core/realtime/ticker_store.dart';
import '../../models/candle.dart';
import '../../models/enums.dart';
import '../../models/ticker.dart';

/// 클라이언트가 들고 있는 실시간 봉의 최대 개수. 봉이 닫혀도 서버 캔들은 InfluxDB 집계 주기
/// (1분 + 오프셋 10초)만큼 늦게 확정된다. 그동안 실시간 봉이 자리를 지킨다(사양서 §4.3.2).
const int kLiveCandleLimit = 4;

/// 새 봉이 열린 뒤 서버 캔들을 다시 조회하기까지의 대기 시간.
const Duration kReconcileDelay = Duration(milliseconds: 15000);

/// 줌인 하한. 표시 개수는 `[12, 전체]` 로 가둔다.
const int kMinVisibleCount = 12;

class CandleIntervalSpec {
  const CandleIntervalSpec({
    required this.label,
    required this.limit,
    required this.visibleCount,
  });

  final String label;

  /// 조회 개수(`limit`). 서버는 1~200 으로 제한한다.
  final int limit;

  /// 최초 표시 개수.
  final int visibleCount;
}

const Map<CandleInterval, CandleIntervalSpec> candleIntervalSpecs = {
  CandleInterval.minute1: CandleIntervalSpec(
    label: '1분',
    limit: 120,
    visibleCount: 40,
  ),
  CandleInterval.hour1: CandleIntervalSpec(
    label: '1시간',
    limit: 96,
    visibleCount: 32,
  ),
  CandleInterval.hour4: CandleIntervalSpec(
    label: '4시간',
    limit: 84,
    visibleCount: 28,
  ),
  CandleInterval.day1: CandleIntervalSpec(
    label: '일',
    limit: 90,
    visibleCount: 32,
  ),
  CandleInterval.week1: CandleIntervalSpec(
    label: '주',
    limit: 72,
    visibleCount: 24,
  ),
  CandleInterval.month1: CandleIntervalSpec(
    label: '월',
    limit: 48,
    visibleCount: 18,
  ),
};

/// `(거래소, 코인, 간격)`. 웹의 `requestKey` 를 대신한다(사양서 §4.3.7).
///
/// 셋 중 하나가 바뀌면 서버 캔들 provider 의 키가 바뀌고 [LiveCandleFolder] 도 빈 배열에서
/// 다시 시작한다 — 이전 코인의 봉에 새 코인의 체결이 접히지 않는다.
@immutable
class CandleRequest {
  const CandleRequest({
    required this.exchangeCode,
    required this.symbol,
    required this.interval,
  });

  /// 캔들 API 만 id 가 아니라 대문자 코드를 받는다(`UPBIT`).
  final String exchangeCode;
  final String symbol;
  final CandleInterval interval;

  CandleIntervalSpec get spec => candleIntervalSpecs[interval]!;

  int get limit => spec.limit;

  int get visibleCount => spec.visibleCount;

  @override
  bool operator ==(Object other) =>
      other is CandleRequest &&
      other.exchangeCode == exchangeCode &&
      other.symbol == symbol &&
      other.interval == interval;

  @override
  int get hashCode => Object.hash(exchangeCode, symbol, interval);
}

/// 체결 시각을 어느 봉에 떨어뜨릴지 정한다(사양서 §4.3.5.1).
///
/// **서버 캔들의 `time` 과 티커의 `timestamp` 양쪽에 같은 함수를 적용한다.** 두 출처의 봉
/// 시각이 정확히 일치해야 합성이 성립하므로 이 함수가 유일한 공통 기준이다. 절삭은 **단말
/// 로컬 시간** 기준이다.
DateTime normalizeCandleTime(DateTime time, CandleInterval interval) {
  final t = time.toLocal();
  return switch (interval) {
    CandleInterval.minute1 => DateTime(t.year, t.month, t.day, t.hour, t.minute),
    CandleInterval.hour1 => DateTime(t.year, t.month, t.day, t.hour),
    // 0, 4, 8, 12, 16, 20 시로 내림.
    CandleInterval.hour4 => DateTime(t.year, t.month, t.day, (t.hour ~/ 4) * 4),
    CandleInterval.day1 => DateTime(t.year, t.month, t.day),
    // 그 주 월요일 자정. Dart 의 weekday 는 월=1 … 일=7 이다.
    CandleInterval.week1 => DateTime(t.year, t.month, t.day - (t.weekday - 1)),
    CandleInterval.month1 => DateTime(t.year, t.month),
  };
}

DateTime normalizeTickTime(int epochMs, CandleInterval interval) =>
    normalizeCandleTime(
      DateTime.fromMillisecondsSinceEpoch(epochMs),
      interval,
    );

/// 서버 캔들 후처리(사양서 §4.3.1 ①). 유한하지 않은 값이 하나라도 있으면 버리고, 봉 시각을
/// 간격 단위로 절삭한 뒤 시간 오름차순으로 정렬한다.
List<Candle> normalizeCandles(List<Candle> candles, CandleInterval interval) {
  final result = [
    for (final candle in candles)
      if (candle.open.isFinite &&
          candle.high.isFinite &&
          candle.low.isFinite &&
          candle.close.isFinite)
        Candle(
          time: normalizeCandleTime(candle.time, interval),
          open: candle.open,
          high: candle.high,
          low: candle.low,
          close: candle.close,
        ),
  ];
  result.sort((a, b) => a.time.compareTo(b.time));
  return result;
}

/// 가변 객체다. 같은 봉 안에서는 필드만 고쳐 쓴다 — 틱마다 배열을 새로 만들지 않는다.
class LiveCandle {
  LiveCandle(this.bucket, double price)
    : open = price,
      high = price,
      low = price,
      close = price;

  final DateTime bucket;

  /// 그 봉에서 클라이언트가 **처음 받은** 체결가다. 서버가 집계한 진짜 시가가 아니다(구독
  /// 시작 전 체결을 못 봤을 수 있다). 서버 봉이 도착하면 [MergedCandles] 가 서버 값으로
  /// 되돌린다.
  final double open;

  double high;
  double low;
  double close;
}

/// 진행 중인 봉을 만든다(사양서 §4.3.5.2).
///
/// [onTick] 은 `TickerStore.ingest` 안에서 **매 틱** 불린다. 프레임 버퍼로 솎아내면 한 프레임
/// 안의 체결이 사라져 봉의 고가·저가가 실제보다 얕아진다. 그리기 알림은 [onFrame] 에서만
/// 낸다 — 접기 빈도와 그리기 빈도를 분리한다.
class LiveCandleFolder implements TickObserver {
  LiveCandleFolder(this.request);

  final CandleRequest request;

  /// 최대 [kLiveCandleLimit] 개. 시간 오름차순.
  final List<LiveCandle> live = [];

  /// 봉이 **바뀔 때만** 발화한다. 재조정 타이머는 이것만 본다 — 틱마다 재무장하지 않는다.
  final ValueNotifier<DateTime?> openedBucket = ValueNotifier(null);

  /// 프레임당 최대 1회 증가한다. 차트는 이것만 구독한다.
  final ValueNotifier<int> revision = ValueNotifier(0);

  bool _dirty = false;

  @override
  void onTick(Ticker tick) {
    // 배열에서 자기 심볼만 고른다.
    if (tick.symbol != request.symbol) return;
    if (!tick.price.isFinite || tick.price <= 0) return;

    final bucket = normalizeTickTime(tick.timestamp, request.interval);
    final opened = live.isEmpty ? null : live.last;

    // ① 같은 봉 → 제자리 갱신. 할당 0. open 은 건드리지 않는다.
    if (opened != null && opened.bucket == bucket) {
      if (tick.price > opened.high) opened.high = tick.price;
      if (tick.price < opened.low) opened.low = tick.price;
      opened.close = tick.price;
      _dirty = true;
      return;
    }

    // ② 이미 닫힌 봉에 뒤늦게 도착한 체결 → 버린다. 다시 열면 서버 집계와 어긋난다.
    if (opened != null && bucket.isBefore(opened.bucket)) return;

    // ③ 새 봉 → 네 값이 모두 이 체결가다. 간격당 1회이며 매 틱이 아니다.
    live.add(LiveCandle(bucket, tick.price));
    if (live.length > kLiveCandleLimit) live.removeAt(0);
    _dirty = true;
    openedBucket.value = bucket;
  }

  @override
  void onFrame() {
    if (!_dirty) return;
    _dirty = false;
    revision.value++;
  }

  void dispose() {
    openedBucket.dispose();
    revision.dispose();
  }
}

/// 서버 캔들 위에 실시간 봉을 얹는다(사양서 §4.3.5.3).
///
/// 배열을 materialize 하지 않는다 — 웹은 매번 새 배열을 만들지만(React 는 그래야 리렌더된다)
/// 여기서는 **인덱스로 읽는 뷰**다. 틱당 배열 복사가 0이다.
///
/// 구조([_tailFrom]·[_overlapsLast])는 서버 캔들이 교체되거나 새 봉이 열릴 때만 달라진다.
/// 같은 봉 안의 체결은 값만 바꾼다.
class MergedCandles {
  MergedCandles(this.server, this.live) {
    var tail = 0;
    var overlaps = false;
    if (live.isNotEmpty && server.isNotEmpty) {
      final lastTime = server.last.time;
      for (var i = 0; i < live.length; i++) {
        final bucket = live[i].bucket;
        // ① 서버가 이미 확정한 과거 봉 → 무시한다.
        if (bucket.isBefore(lastTime)) {
          tail = i + 1;
          continue;
        }
        // ② 서버의 마지막 봉과 같은 구간 → 서버가 기준이고 실시간을 덧댄다.
        if (bucket == lastTime) {
          overlaps = true;
          tail = i + 1;
          continue;
        }
        // ③ 이후는 전부 서버보다 미래다.
        break;
      }
    }
    _tailFrom = tail;
    _overlapsLast = overlaps;
  }

  final List<Candle> server;
  final List<LiveCandle> live;

  late final int _tailFrom;
  late final bool _overlapsLast;

  int get length => server.length + (live.length - _tailFrom);

  /// 실시간 값이 반영되는 첫 인덱스. 없으면 [length] 다. 페인터의 repaint 게이트가 이것을
  /// 본다 — 실시간 봉이 표시 구간 밖이면 revision 이 올라도 그림이 같다.
  int get liveFromIndex {
    if (_overlapsLast) return server.length - 1;
    if (_tailFrom < live.length) return server.length;
    return length;
  }

  Candle operator [](int index) {
    final lastIdx = server.length - 1;
    if (index < lastIdx) return server[index];

    if (index == lastIdx) {
      if (!_overlapsLast) return server[index];
      final s = server[index];
      final l = live[_tailFrom - 1];
      return Candle(
        // 시가는 서버 값이고 종가는 최신 체결가다.
        time: s.time,
        open: s.open,
        high: math.max(s.high, l.high),
        low: math.min(s.low, l.low),
        close: l.close,
      );
    }

    final l = live[_tailFrom + (index - server.length)];
    return Candle(
      time: l.bucket,
      open: l.open,
      high: l.high,
      low: l.low,
      close: l.close,
    );
  }

  /// 보이는 구간만 꺼낸다(≤120개). 과거 봉은 서버 객체를 그대로 돌려주므로 새로 만들어지는
  /// 것은 실시간이 얹힌 최대 [kLiveCandleLimit]+1 개뿐이다.
  List<Candle> slice(int start, int end) => [
    for (var i = start; i < end; i++) this[i],
  ];
}
