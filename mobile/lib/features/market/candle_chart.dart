import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/format/formatters.dart';
import '../../core/realtime/ticker_store.dart';
import '../../core/theme/theme.dart';
import '../../core/widgets/numeric_text.dart';
import '../../models/candle.dart';
import 'candle_painter.dart';
import 'candle_repository.dart';
import 'candle_scale.dart';
import 'live_candles.dart';

/// 실시간 봉의 소유자. **Riverpod 은 이 객체를 들고만 있고 틱은 통과하지 않는다** — 접기는
/// `TickerStore` 가 매 틱 동기로 호출한다(계획서 §5.4 ②).
///
/// 관찰자는 하나뿐이다. 코인 상세와 가로 풀스크린이 같은 [CandleRequest] 를 쓰면 같은 폴더를
/// 공유하고, 간격이 바뀌면 새 폴더가 관찰자 자리를 넘겨받는다.
final liveCandleFolderProvider = Provider.autoDispose
    .family<LiveCandleFolder, CandleRequest>((ref, request) {
      final store = ref.watch(tickerStoreProvider);
      final folder = LiveCandleFolder(request);
      store.setRawObserver(folder);
      ref.onDispose(() {
        store.clearRawObserver(folder);
        folder.dispose();
      });
      return folder;
    });

/// REST 캔들과 STOMP 티커를 함께 소비한다(사양서 §4.3.1). 정적 차트가 아니다.
class CandleChart extends ConsumerStatefulWidget {
  const CandleChart({
    super.key,
    required this.request,
    required this.baseCurrency,
  });

  final CandleRequest request;
  final String baseCurrency;

  @override
  ConsumerState<CandleChart> createState() => _CandleChartState();
}

class _CandleChartState extends ConsumerState<CandleChart> {
  final ValueNotifier<int?> _crosshair = ValueNotifier(null);

  late CandleViewport _viewport = CandleViewport(
    visibleCount: widget.request.visibleCount,
  );
  late CandleViewport _gestureBase = _viewport;

  List<Candle> _server = const [];
  LiveCandleFolder? _folder;
  Timer? _reconcile;
  Size _size = Size.zero;
  int _panAnchorEnd = 0;
  double _panDx = 0;
  bool _pinching = false;

  @override
  void dispose() {
    _reconcile?.cancel();
    _folder?.openedBucket.removeListener(_onBucketOpened);
    _crosshair.dispose();
    super.dispose();
  }

  MergedCandles get _merged =>
      MergedCandles(_server, _folder?.live ?? const []);

  void _bind(LiveCandleFolder folder) {
    if (identical(_folder, folder)) return;
    _folder?.openedBucket.removeListener(_onBucketOpened);
    _folder = folder;
    folder.openedBucket.addListener(_onBucketOpened);
  }

  /// 봉이 새로 열렸다 = 직전 봉이 닫혔다. 같은 봉 안에서 체결이 아무리 많이 들어와도
  /// 재조회는 예약되지 않는다 — 타이머는 틱이 아니라 봉을 따라간다(사양서 §4.3.6).
  void _onBucketOpened() {
    _reconcile?.cancel();
    if (_folder?.openedBucket.value == null) return;
    _reconcile = Timer(kReconcileDelay, () {
      if (mounted) ref.invalidate(candlesProvider(widget.request));
    });
  }

  void _onScaleStart(ScaleStartDetails details) {
    _gestureBase = _viewport;
    _panAnchorEnd = _viewport.endIndexOf(_merged.length);
    _panDx = 0;
    _pinching = false;
  }

  void _onScaleUpdate(ScaleUpdateDetails details) {
    final merged = _merged;
    final total = merged.length;
    if (total == 0) return;

    if (details.pointerCount >= 2) {
      _pinching = true;
      final plotWidth = math.max(1.0, _size.width - kChartPadding.horizontal);
      final ratio = ((details.localFocalPoint.dx - kChartPadding.left) /
              plotWidth)
          .clamp(0.0, 1.0);
      final next = _gestureBase.zoom(
        total: total,
        scale: details.scale,
        focusRatio: ratio,
      );
      _apply(next);
      return;
    }
    if (_pinching) return;

    _panDx += details.focalPointDelta.dx;
    final scale = _scaleOf(merged, total);
    _apply(
      _viewport.withEnd(_panAnchorEnd - scale.movedCandles(_panDx), total),
    );
  }

  void _apply(CandleViewport next) {
    if (next.visibleCount == _viewport.visibleCount &&
        next.anchorEndIndex == _viewport.anchorEndIndex &&
        next.followingLatest == _viewport.followingLatest) {
      return;
    }
    setState(() => _viewport = next);
  }

  CandleScale _scaleOf(MergedCandles merged, int total) => CandleScale.of(
    _size,
    kChartPadding,
    merged.slice(_viewport.startIndexOf(total), _viewport.endIndexOf(total)),
  );

  void _moveCrosshair(Offset local) {
    final merged = _merged;
    final total = merged.length;
    if (total == 0) return;

    if (local.dy < kChartPadding.top - 8 ||
        local.dy > _size.height - kChartPadding.bottom + 8) {
      _crosshair.value = null;
      return;
    }

    final start = _viewport.startIndexOf(total);
    final hit = _scaleOf(merged, total).hitTest(local.dx);
    _crosshair.value = hit == null ? null : start + hit;
  }

  @override
  Widget build(BuildContext context) {
    final candles = ref.watch(candlesProvider(widget.request));
    final data = candles.valueOrNull;
    // 재조회가 실패하거나 빈 배열이면 아무것도 하지 않는다 — 실시간 봉이 자리를 지킨다.
    if (data != null && data.isNotEmpty) _server = data;

    _bind(ref.watch(liveCandleFolderProvider(widget.request)));
    final theme = CandleChartTheme.of(context, widget.baseCurrency);
    final empty = _server.isEmpty && _folder!.live.isEmpty;

    return Column(
      children: [
        SizedBox(height: 44, child: _tooltip(context)),
        Expanded(
          child: empty
              ? Center(
                  child: candles.isLoading
                      ? const CircularProgressIndicator()
                      : Text(
                          '캔들 데이터가 부족합니다',
                          style: Theme.of(context).textTheme.labelMedium
                              ?.copyWith(
                                color: Theme.of(
                                  context,
                                ).colorScheme.onSurfaceVariant,
                              ),
                        ),
                )
              : _chart(theme),
        ),
      ],
    );
  }

  Widget _chart(CandleChartTheme theme) {
    return LayoutBuilder(
      builder: (context, constraints) {
        _size = Size(constraints.maxWidth, constraints.maxHeight);

        return GestureDetector(
          behavior: HitTestBehavior.opaque,
          onScaleStart: _onScaleStart,
          onScaleUpdate: _onScaleUpdate,
          onLongPressStart: (details) => _moveCrosshair(details.localPosition),
          onLongPressMoveUpdate: (details) =>
              _moveCrosshair(details.localPosition),
          onLongPressEnd: (_) => _crosshair.value = null,
          onLongPressCancel: () => _crosshair.value = null,
          child: Stack(
            children: [
              RepaintBoundary(
                child: ValueListenableBuilder<int>(
                  valueListenable: _folder!.revision,
                  builder: (context, revision, child) {
                    final merged = _merged;
                    final total = merged.length;
                    final start = _viewport.startIndexOf(total);
                    final end = _viewport.endIndexOf(total);
                    return CustomPaint(
                      size: _size,
                      painter: CandlePainter(
                        server: _server,
                        visible: merged.slice(start, end),
                        startIndex: start,
                        endIndex: end,
                        liveVisible: end > merged.liveFromIndex,
                        revision: revision,
                        interval: widget.request.interval,
                        theme: theme,
                      ),
                    );
                  },
                ),
              ),
              // 크로스헤어가 움직여도 캔들 레이어는 다시 칠하지 않는다(그 반대도 마찬가지다).
              RepaintBoundary(
                child: ListenableBuilder(
                  listenable: Listenable.merge([_folder!.revision, _crosshair]),
                  builder: (context, child) {
                    final index = _crosshair.value;
                    final merged = _merged;
                    final total = merged.length;
                    final start = _viewport.startIndexOf(total);
                    final end = _viewport.endIndexOf(total);
                    return CustomPaint(
                      size: _size,
                      painter: CrosshairPainter(
                        visible: index == null
                            ? const []
                            : merged.slice(start, end),
                        index: index == null ? null : index - start,
                        revision: _folder!.revision.value,
                        theme: theme,
                      ),
                    );
                  },
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  /// 툴팁은 차트 위 **고정 패널**이다. 손가락이 가리는 자리에 띄우지 않는다.
  Widget _tooltip(BuildContext context) {
    final theme = Theme.of(context);

    return ValueListenableBuilder<int?>(
      valueListenable: _crosshair,
      builder: (context, index, child) {
        final merged = _merged;
        if (index == null || index < 0 || index >= merged.length) {
          return Align(
            alignment: Alignment.centerLeft,
            child: Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: TryptoSpacing.screen,
              ),
              child: Text(
                '길게 눌러 시세를 확인하세요',
                style: theme.textTheme.labelSmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ),
          );
        }

        final candle = merged[index];
        return Container(
          margin: const EdgeInsets.symmetric(horizontal: TryptoSpacing.screen),
          padding: const EdgeInsets.symmetric(horizontal: TryptoSpacing.sm),
          decoration: BoxDecoration(
            color: TryptoPalette.secondary,
            borderRadius: BorderRadius.circular(TryptoRadius.md),
          ),
          child: Row(
            children: [
              Expanded(
                flex: 3,
                child: Text(
                  _stamp(candle.time),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.labelSmall,
                ),
              ),
              for (final (label, value) in [
                ('시', candle.open),
                ('고', candle.high),
                ('저', candle.low),
                ('종', candle.close),
              ])
                Expanded(
                  flex: 4,
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      Text(
                        label,
                        style: theme.textTheme.labelSmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(width: 2),
                      Flexible(
                        child: NumericText(
                          formatCurrencyCompact(value, widget.baseCurrency),
                          size: 11,
                          weight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        );
      },
    );
  }

  String _stamp(DateTime time) {
    String two(int value) => value.toString().padLeft(2, '0');
    return '${two(time.year % 100)}.${two(time.month)}.${two(time.day)} '
        '${two(time.hour)}:${two(time.minute)}';
  }
}
