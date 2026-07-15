import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/constants/exchanges.dart';
import '../../core/format/formatters.dart';
import '../../core/realtime/stomp_service.dart';
import '../../core/realtime/ticker_store.dart';
import '../../core/router/guard.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/empty_view.dart';
import '../../core/widgets/numeric_text.dart';
import '../../core/widgets/profit_badge.dart';
import '../../models/enums.dart';
import '../round/round_controller.dart';
import 'candle_chart.dart';
import 'live_candles.dart';
import 'market_controller.dart';
import 'order_history_tab.dart';
import 'order_sheet.dart';
import 'order_target.dart';

/// 마켓 셸 브랜치 **위로** push 된다. 마켓 탭은 계속 선택 상태이므로 `TickerStore` 가 살아
/// 있고 차트는 별도로 토픽을 구독하지 않는다(계획서 §5.4).
class CoinDetailPage extends ConsumerStatefulWidget {
  const CoinDetailPage({super.key, required this.symbol});

  final String symbol;

  @override
  ConsumerState<CoinDetailPage> createState() => _CoinDetailPageState();
}

class _CoinDetailPageState extends ConsumerState<CoinDetailPage> {
  CandleInterval _interval = CandleInterval.day1;

  /// 주문이 나갈 때마다 올린다. 거래내역 탭이 이 값을 보고 다시 읽는다 — 체결 이벤트 큐는
  /// 서버에서 동작하지 않으므로(사양서 R3) 반영은 전부 REST 재조회다.
  int _revision = 0;

  /// 이 화면은 마켓을 루트 네비게이터 위로 덮는다. 가려진 마켓 페이지는 자기 티커 구독을
  /// 끊고 스토어를 멈추려 하므로, 상세가 직접 스토어를 잡고(hold) 같은 토픽을 구독해 현재가·
  /// 캔들 실시간 갱신을 잇는다. 거래소는 진입 시점에 고정된다(쿼리 파라미터).
  void Function()? _cancelTickers;
  bool _bound = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_bound) return;
    _bound = true;
    final store = ref.read(tickerStoreProvider);
    store.hold();
    final exchange = ExchangeIds.byKey(
      GoRouterState.of(context).uri.queryParameters['exchange'],
    );
    _cancelTickers = ref
        .read(stompServiceProvider)
        .subscribe(
          '/topic/tickers.${exchange.id}',
          (body) => store.ingest(decodeTickers(body)),
        );
  }

  @override
  void dispose() {
    _cancelTickers?.call();
    ref.read(tickerStoreProvider).release();
    super.dispose();
  }

  void _refresh() => setState(() => _revision++);

  @override
  Widget build(BuildContext context) {
    final exchange = ExchangeIds.byKey(
      GoRouterState.of(context).uri.queryParameters['exchange'],
    );
    final coins = ref.watch(marketCoinsProvider(exchange.id));
    final walletId = ref.watch(
      roundControllerProvider.select((round) => round.walletIdOf(exchange.id)),
    );

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.symbol),
        actions: [
          IconButton(
            tooltip: '전체 화면',
            icon: const Icon(LucideIcons.maximize),
            onPressed: () => context.push(
              Routes.coinChart(widget.symbol, exchange.key, _interval.wire),
            ),
          ),
        ],
      ),
      body: AsyncView<List<CoinEntry>>(
        value: coins,
        onRetry: () => ref.invalidate(marketCoinsProvider(exchange.id)),
        builder: (data) {
          final entry = data
              .where((coin) => coin.symbol == widget.symbol)
              .firstOrNull;
          if (entry == null) {
            return const EmptyView(
              icon: LucideIcons.circleAlert,
              message: '이 거래소에 상장되지 않은 코인입니다.',
            );
          }

          final resolution = resolveOrderTarget(
            exchange: exchange,
            symbol: entry.symbol,
            walletId: walletId,
            coins: data,
          );

          return DefaultTabController(
            length: 2,
            child: Column(
              children: [
                // 헤더는 티커가 올 때마다 갱신되고 캔들 레이어는 실시간 봉이 보일 때만 다시
                // 칠해진다. 경계가 없으면 헤더 한 줄이 200개 캔들을 다시 칠한다.
                RepaintBoundary(
                  child: CoinDetailHeader(
                    entry: entry,
                    row: ref.watch(tickerStoreProvider).row(entry.symbol),
                    baseCurrency: exchange.baseCurrency,
                  ),
                ),
                const TabBar(tabs: [Tab(text: '차트'), Tab(text: '거래내역')]),
                Expanded(
                  child: TabBarView(
                    children: [
                      _ChartTab(
                        entry: entry,
                        exchange: exchange,
                        interval: _interval,
                        onInterval: (interval) =>
                            setState(() => _interval = interval),
                      ),
                      if (resolution.target case final target?)
                        OrderHistoryTab(
                          target: target,
                          symbol: entry.symbol,
                          revision: _revision,
                        )
                      else
                        _TargetFailureView(failure: resolution.failure!),
                    ],
                  ),
                ),
                _OrderCta(
                  target: resolution.target,
                  failure: resolution.failure,
                  entry: entry,
                  onPlaced: _refresh,
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _ChartTab extends StatelessWidget {
  const _ChartTab({
    required this.entry,
    required this.exchange,
    required this.interval,
    required this.onInterval,
  });

  final CoinEntry entry;
  final Exchange exchange;
  final CandleInterval interval;
  final ValueChanged<CandleInterval> onInterval;

  @override
  Widget build(BuildContext context) {
    final request = CandleRequest(
      exchangeCode: exchange.candleCode,
      symbol: entry.symbol,
      interval: interval,
    );

    return Column(
      children: [
        IntervalChips(interval: interval, onChanged: onInterval),
        Expanded(
          child: RepaintBoundary(
            child: CandleChart(
              key: ValueKey(request),
              request: request,
              baseCurrency: exchange.baseCurrency,
            ),
          ),
        ),
      ],
    );
  }
}

/// 하단 고정 CTA 2개(계획서 §4.6.1). 주문 대상이 해석되지 않으면 사유를 알리고 비활성한다 —
/// 라운드가 없어도 시세 조회는 계속되고 **주문만** 막힌다.
class _OrderCta extends StatelessWidget {
  const _OrderCta({
    required this.target,
    required this.failure,
    required this.entry,
    required this.onPlaced,
  });

  final OrderTarget? target;
  final OrderTargetFailure? failure;
  final CoinEntry entry;
  final VoidCallback onPlaced;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.tryptoColors;
    final resolved = target;

    Widget button(Side side) {
      final buy = side == Side.buy;
      final color = buy ? colors.positive : colors.negative;

      return Expanded(
        child: SizedBox(
          height: 48,
          child: FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: color,
              foregroundColor: Colors.white,
              disabledBackgroundColor: color.withValues(alpha: 0.4),
              disabledForegroundColor: Colors.white,
            ),
            onPressed: resolved == null
                ? null
                : () => showOrderSheet(
                    context,
                    target: resolved,
                    entry: entry,
                    side: side,
                    onPlaced: onPlaced,
                  ),
            child: Text(buy ? '매수' : '매도'),
          ),
        ),
      );
    }

    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.all(TryptoSpacing.screen),
        decoration: const BoxDecoration(
          color: TryptoPalette.card,
          border: Border(top: BorderSide(color: TryptoPalette.border)),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (failure != null) ...[
              Row(
                children: [
                  Expanded(
                    child: Text(
                      failure!.message,
                      style: theme.textTheme.labelMedium?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ),
                  if (failure == OrderTargetFailure.noRound)
                    TextButton(
                      style: TryptoButtons.link,
                      onPressed: () => context.go(Routes.roundNew),
                      child: const Text('라운드 시작하기'),
                    ),
                ],
              ),
              const SizedBox(height: TryptoSpacing.sm),
            ],
            Row(
              children: [
                button(Side.buy),
                const SizedBox(width: TryptoSpacing.sm),
                button(Side.sell),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _TargetFailureView extends StatelessWidget {
  const _TargetFailureView({required this.failure});

  final OrderTargetFailure failure;

  @override
  Widget build(BuildContext context) {
    return EmptyView(icon: LucideIcons.receipt, message: failure.message);
  }
}

/// 현재가·등락률은 캔들이 아니라 **실시간 티커 값**이다(사양서 §4.3.1).
class CoinDetailHeader extends StatelessWidget {
  const CoinDetailHeader({
    super.key,
    required this.entry,
    required this.row,
    required this.baseCurrency,
  });

  final CoinEntry entry;
  final RowNotifier? row;
  final String baseCurrency;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final notifier = row;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(
        TryptoSpacing.screen,
        TryptoSpacing.sm,
        TryptoSpacing.screen,
        TryptoSpacing.md,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            entry.name,
            style: theme.textTheme.labelMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 2),
          if (notifier == null)
            _Quote(
              price: entry.price,
              changeRate: entry.changeRate,
              baseCurrency: baseCurrency,
            )
          else
            ValueListenableBuilder<CoinRowState>(
              valueListenable: notifier,
              builder: (context, state, child) => _Quote(
                price: state.price,
                changeRate: state.changeRate,
                baseCurrency: baseCurrency,
              ),
            ),
        ],
      ),
    );
  }
}

class _Quote extends StatelessWidget {
  const _Quote({
    required this.price,
    required this.changeRate,
    required this.baseCurrency,
  });

  final double price;
  final double changeRate;
  final String baseCurrency;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Flexible(
          child: NumericText(
            price <= 0
                ? '-'
                : '${getCurrencySymbol(baseCurrency)}'
                      '${formatPrice(price, baseCurrency)}',
            size: 24,
            weight: FontWeight.w700,
            color: context.profitColor(changeRate),
          ),
        ),
        const SizedBox(width: TryptoSpacing.sm),
        ProfitBadge(
          value: changeRate,
          text: formatChangeRate(changeRate),
          showTrendIcon: true,
        ),
      ],
    );
  }
}

class IntervalChips extends StatelessWidget {
  const IntervalChips({
    super.key,
    required this.interval,
    required this.onChanged,
  });

  final CandleInterval interval;
  final ValueChanged<CandleInterval> onChanged;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 40,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: TryptoSpacing.screen),
        children: [
          for (final entry in candleIntervalSpecs.entries) ...[
            ChoiceChip(
              label: Text(entry.value.label),
              selected: entry.key == interval,
              labelStyle: TextStyle(
                fontSize: 12,
                fontWeight:
                    entry.key == interval ? FontWeight.w700 : FontWeight.w600,
                color: entry.key == interval
                    ? Theme.of(context).colorScheme.primary
                    : Theme.of(context).colorScheme.onSurface,
              ),
              onSelected: (_) => onChanged(entry.key),
            ),
            const SizedBox(width: TryptoSpacing.xs),
          ],
        ],
      ),
    );
  }
}
