import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/scheduler.dart' show SchedulerBinding;
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../models/exchange_coin.dart';
import '../../models/ticker.dart';

/// 티커 배칭의 단일 지점(계획서 §4.2.4). 초당 수백 건 × 최대 600행이 들어온다.
///
/// **티커는 Riverpod 그래프를 통과하지 않는다.** 심볼별 [ValueNotifier] 로 해당 행만 갱신한다.
/// 경로가 둘이다.
///
/// - **그리기**: 프레임당 1회 flush. 같은 프레임의 같은 심볼은 마지막 값만 남는다.
/// - **접기**: [TickObserver.onTick] 이 **매 틱 동기 호출**된다. 프레임 버퍼로 캔들을 접으면
///   한 프레임 안의 체결이 사라져 봉의 고가·저가가 실제보다 얕아진다(사양서 §4.3.5.2).
enum FlashDir { up, down, same }

/// 100ms 후 즉시 해제한다. 페이드하지 않는다(사양서 §3.3.3).
const Duration kFlashDuration = Duration(milliseconds: 100);

/// 숫자 3개만 담는다. `tickedAt` 을 넣지 않는다 — 넣으면 매 틱 `==` 가 거짓이 되어 동일가
/// 재체결에서 숫자 위젯이 무의미하게 리빌드된다.
///
/// `==`/`hashCode` 는 성능 계약이다.
@immutable
class CoinRowState {
  const CoinRowState(this.price, this.changeRate, this.volume);

  final double price;
  final double changeRate;
  final double volume;

  @override
  bool operator ==(Object other) =>
      other is CoinRowState &&
      other.price == price &&
      other.changeRate == changeRate &&
      other.volume == volume;

  @override
  int get hashCode => Object.hash(price, changeRate, volume);
}

/// `hasListeners` 는 `ChangeNotifier` 의 protected 멤버다. 화면 밖 행을 건너뛰려면 공개 게터가
/// 필요하다.
class RowNotifier extends ValueNotifier<CoinRowState> {
  RowNotifier(super.value);

  int? lastTickedAt;

  bool get isWatched => hasListeners;
}

class FlashNotifier extends ValueNotifier<FlashDir?> {
  FlashNotifier() : super(null);

  bool get isWatched => hasListeners;
}

/// 티커의 두 번째 소비자(캔들 차트)가 구현한다. 관찰자는 하나뿐이다 — 차트는 한 번에 하나만
/// 열린다.
abstract class TickObserver {
  /// [TickerStore.ingest] 안에서 **매 틱** 동기 호출된다. 접기 전용이며 그리기를 하지 않는다.
  void onTick(Ticker tick);

  /// flush 안에서 **프레임당 1회** 호출된다. 그리기 알림은 여기서만 낸다.
  void onFrame();
}

class TickerStore {
  final Map<String, RowNotifier> _rows = {};
  final Map<String, FlashNotifier> _flash = {};

  /// 프레임 버퍼. 같은 심볼은 마지막 값이 이긴다.
  final Map<String, Ticker> _pending = {};
  final Map<String, int> _flashUntilMs = {};

  TickObserver? _observer;
  bool _scheduled = false;
  int _frameId = 0;
  bool _active = true;
  bool _orderDirty = false;
  bool _disposed = false;

  RowNotifier? row(String symbol) => _rows[symbol];

  FlashNotifier? flash(String symbol) => _flash[symbol];

  /// 정렬·필터가 읽는 최신값. 목록 밖 심볼이면 null.
  CoinRowState? quote(String symbol) => _rows[symbol]?.value;

  /// 시세 변동에 의한 재정렬은 티커와 캐던스를 분리한다(1초 스로틀 + 스크롤 중 동결).
  bool get orderDirty => _orderDirty;

  void clearOrderDirty() => _orderDirty = false;

  void setRawObserver(TickObserver observer) => _observer = observer;

  /// 관찰자 교체 중에 옛 관찰자의 해제가 새 관찰자를 지우지 않게 한다.
  void clearRawObserver(TickObserver observer) {
    if (identical(_observer, observer)) _observer = null;
  }

  /// 마켓 탭이 비활성이면 flush 를 멈춘다. `indexedStack` 은 숨은 탭의 build 비용을 그대로
  /// 청구하므로 구독 해제만으로는 부족하다(계획서 §4.2.3).
  void setActive(bool active) {
    if (_active == active || _disposed) return;
    _active = active;
    if (active) return;
    _pending.clear();
    // 잔여 플래시는 다음 프레임의 만료 스윕이 끈다. notifier 쓰기는 flush 안에서만 한다.
    if (_flashUntilMs.isNotEmpty) _schedule(rescheduling: false);
  }

  /// 거래소 전환·스냅샷 재조회. 이전 거래소의 잔여 틱이 새 목록을 오염시키지 않게 한다
  /// (사양서 §3.3.2-5).
  ///
  /// 옛 notifier 를 dispose 하지 않는다 — 아직 마운트된 행이 리스너를 떼는 중이다. 참조만
  /// 끊으면 GC 가 가져간다.
  void switchExchange(Iterable<ExchangeCoin> coins) {
    _pending.clear();
    _flashUntilMs.clear();
    _rows.clear();
    _flash.clear();
    for (final coin in coins) {
      _rows[coin.coinSymbol] = RowNotifier(
        CoinRowState(coin.price, coin.changeRate, coin.volume),
      );
      _flash[coin.coinSymbol] = FlashNotifier();
    }
    _orderDirty = false;
  }

  /// STOMP 프레임마다 호출된다. 초당 수백 회. `setState` 를 절대 부르지 않는다.
  void ingest(List<Ticker> ticks) {
    if (!_active || _disposed) return;
    var accepted = false;
    for (final tick in ticks) {
      // 상장 목록 밖 심볼은 버린다(사양서 §3.3.2-2).
      if (!_rows.containsKey(tick.symbol)) continue;
      _observer?.onTick(tick);
      _pending[tick.symbol] = tick;
      accepted = true;
    }
    if (accepted) _schedule(rescheduling: false);
  }

  void _schedule({required bool rescheduling}) {
    if (_scheduled || _disposed) return;
    _scheduled = true;
    // Timer(16ms) 가 아니다. transient callback 은 같은 프레임의 build 보다 앞서 실행되므로
    // flush 가 만든 dirty 마크가 그 프레임 안에서 처리된다.
    _frameId = SchedulerBinding.instance.scheduleFrameCallback(
      _flush,
      rescheduling: rescheduling,
    );
  }

  /// [timeStamp] 는 프레임의 시각이다. `DateTime.now()` 를 쓰지 않는다 — 프레임마다 시계를
  /// 읽을 이유가 없고, 플래시 만료는 프레임 시간축에서 판정해야 일관된다.
  void _flush(Duration timeStamp) {
    _scheduled = false;
    _frameId = 0;
    if (_disposed) return;

    final nowMs = timeStamp.inMilliseconds;

    if (_pending.isNotEmpty) {
      for (final tick in _pending.values) {
        final row = _rows[tick.symbol];
        if (row == null) continue;
        final flash = _flash[tick.symbol]!;

        // 화면 밖 행은 리스너가 0개다. 플래시를 계산하지 않는다.
        if (flash.isWatched &&
            row.lastTickedAt != null &&
            tick.timestamp != row.lastTickedAt) {
          flash.value = tick.price > row.value.price
              ? FlashDir.up
              : tick.price < row.value.price
              ? FlashDir.down
              : FlashDir.same;
          _flashUntilMs[tick.symbol] = nowMs + kFlashDuration.inMilliseconds;
        }
        row.lastTickedAt = tick.timestamp;
        // 값이 같으면 알림이 나가지 않는다(CoinRowState.== 계약).
        row.value = CoinRowState(tick.price, tick.changeRate, tick.quoteTurnover);
      }
      _pending.clear();
      _orderDirty = true;
    }

    // 심볼별 Timer 대신 flush 루프에서 만료를 쓸어 담는다. 타이머 수: 수백 → 0.
    _flashUntilMs.removeWhere((symbol, until) {
      if (nowMs < until) return false;
      _flash[symbol]?.value = null;
      return true;
    });

    _observer?.onFrame();

    if (_flashUntilMs.isNotEmpty) _schedule(rescheduling: true);
  }

  void dispose() {
    if (_disposed) return;
    _disposed = true;
    if (_scheduled) {
      SchedulerBinding.instance.cancelFrameCallbackWithId(_frameId);
      _scheduled = false;
    }
    _pending.clear();
    _flashUntilMs.clear();
    _rows.clear();
    _flash.clear();
    _observer = null;
  }
}

/// 페이로드는 JSON **배열**이다(사양서 §3.2.1). 파싱 실패·빈 배열은 조용히 무시한다.
List<Ticker> decodeTickers(String body) {
  try {
    final decoded = jsonDecode(body);
    if (decoded is! List || decoded.isEmpty) return const [];
    return [
      for (final item in decoded)
        if (item is Map<String, dynamic>) Ticker.fromJson(item),
    ];
  } catch (_) {
    return const [];
  }
}

final tickerStoreProvider = Provider<TickerStore>((ref) {
  final store = TickerStore();
  ref.onDispose(store.dispose);
  return store;
});
