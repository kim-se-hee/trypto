import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

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
/// 코인 상세와 가로 풀스크린이 같은 [CandleRequest] 를 쓰면 같은 폴더를 공유한다. 간격이
/// 다르면 폴더가 둘이 되고 **둘 다 관찰자로 산다** — 풀스크린을 닫아도 상세 차트의 실시간
/// 갱신이 멈추지 않는다. 폴더는 자기 심볼·자기 간격으로만 접으므로 서로 오염되지 않는다.
final liveCandleFolderProvider = Provider.autoDispose
    .family<LiveCandleFolder, CandleRequest>((ref, request) {
      final store = ref.watch(tickerStoreProvider);
      final folder = LiveCandleFolder(request);
      store.addRawObserver(folder);
      ref.onDispose(() {
        store.removeRawObserver(folder);
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

  /// 마지막으로 반영한 provider 응답의 **참조**. 같은 응답으로 다시 빌드될 때 배열을 새로
  /// 만들면 페인터의 `identical` 게이트가 깨져 틱마다 다시 칠한다.
  List<Candle>? _fetched;

  /// 더 당겨 올 과거 캔들이 남았는가. 조회 결과가 비면 끝에 다다른 것으로 보고 멈춘다.
  bool _hasMorePast = true;

  /// 같은 구간을 두 번 당겨 오지 않도록 조회가 도는 동안 잠근다.
  bool _loadingPast = false;

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
      _apply(next, total);
      return;
    }
    if (_pinching) return;

    _panDx += details.focalPointDelta.dx;
    final scale = _scaleOf(merged, total);
    _apply(
      _viewport.withEnd(_panAnchorEnd - scale.movedCandles(_panDx), total),
      total,
    );
  }

  void _apply(CandleViewport next, int total) {
    // 보이는 구간의 왼쪽 끝이 문턱 안으로 들어오면 벽에 닿기 전에 과거를 미리 당겨 온다.
    if (next.endIndexOf(total) - next.countOf(total) <= kPrefetchThreshold) {
      unawaited(_loadPast());
    }
    if (next.visibleCount == _viewport.visibleCount &&
        next.anchorEndIndex == _viewport.anchorEndIndex &&
        next.followingLatest == _viewport.followingLatest) {
      return;
    }
    setState(() => _viewport = next);
  }

  /// 가장 오래된 봉을 커서로 삼아 그 이전 구간을 서버에서 당겨 와 앞에 이어 붙인다.
  ///
  /// 커서 조회는 확정봉만 돌려주므로 진행봉이 과거 구간에 끼어들지 않는다. 앞에 끼워 넣은
  /// 만큼 기존 인덱스가 전부 밀리므로 뷰포트와 제스처 기준점을 같이 밀어 화면이 튀지 않게 한다.
  Future<void> _loadPast() async {
    if (_loadingPast || !_hasMorePast || _server.isEmpty) return;

    _loadingPast = true;
    final oldest = _server.first.time;
    try {
      final older = await findPastCandles(
        ref.read(candleRepositoryProvider),
        widget.request,
        oldest,
      );
      if (!mounted) return;

      final fresh = [
        for (final candle in older)
          if (candle.time.isBefore(oldest)) candle,
      ];
      if (fresh.isEmpty) {
        // 더 오래된 봉이 없다. 끝에 다다랐으니 더는 조회하지 않는다.
        _hasMorePast = false;
        return;
      }

      setState(() {
        _server = [...fresh, ..._server];
        _viewport = _viewport.prepended(fresh.length);
      });
      // 조회가 도는 사이 드래그가 이어지고 있으면 손끝과 화면이 어긋나지 않도록 같이 민다.
      _gestureBase = _gestureBase.prepended(fresh.length);
      _panAnchorEnd += fresh.length;
    } catch (_) {
      // 과거 캔들 조회 실패 시 다음 스와이프에서 다시 시도한다.
    } finally {
      _loadingPast = false;
    }
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
    // 같은 응답으로 다시 빌드될 때도 배열을 새로 만들지 않는다(페인터의 identical 게이트).
    if (data != null && data.isNotEmpty && !identical(data, _fetched)) {
      _fetched = data;
      // 통째로 갈아끼우면 앞에 쌓아 둔 과거 구간이 사라진다. 최신 창만 서버 값으로 덮는다.
      _server = mergeReconciled(_server, data);
    }

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
                          formatAxisLabel(value, widget.baseCurrency),
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

  /// 봉 시각은 UTC 로 들고 다닌다. 로컬 변환은 여기서만 한다.
  String _stamp(DateTime bucket) {
    final time = bucket.toLocal();
    String two(int value) => value.toString().padLeft(2, '0');
    return '${two(time.year % 100)}.${two(time.month)}.${two(time.day)} '
        '${two(time.hour)}:${two(time.minute)}';
  }
}
