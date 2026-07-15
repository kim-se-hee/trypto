import 'dart:async';

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
import '../../core/widgets/app_snackbar.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/empty_view.dart';
import '../../core/widgets/exchange_segment.dart';
import '../../core/widgets/mypage_button.dart';
import '../../models/enums.dart';
import '../../models/round.dart';
import '../round/emergency_funding_sheet.dart';
import '../round/round_controller.dart';
import 'coin_row.dart';
import 'coin_search_field.dart';
import 'market_controller.dart';
import 'overview_cards.dart';

/// 시세 변동에 따른 재정렬 캐던스. 사용자 조작(정렬키·필터·검색어·거래소)은 즉시 반영한다.
/// 이 스로틀이 없으면 인덱스→심볼 매핑이 매 프레임 뒤집혀 심볼별 notifier 의 이득이 통째로
/// 사라지고, 초당 60번 자리를 바꾸는 목록은 읽을 수도 없다(계획서 §4.2.5-5).
const Duration _kResortInterval = Duration(seconds: 1);

/// 선택 거래소는 라우트 쿼리(`/market?exchange=upbit`)에 둔다 — 딥링크와 복원이 공짜로 된다.
///
/// 티커 토픽 구독은 **거래소당 하나**다. `TickerStore` 가 목록과 캔들 차트에 나눠 준다 —
/// 웹은 목록과 차트가 같은 토픽을 각각 구독해 페이로드를 두 번 받는다(사양서 §4.3.9).
class MarketPage extends ConsumerStatefulWidget {
  const MarketPage({super.key});

  @override
  ConsumerState<MarketPage> createState() => _MarketPageState();
}

class _MarketPageState extends ConsumerState<MarketPage> {
  final TextEditingController _search = TextEditingController();
  MarketView _view = const MarketView();

  late final AppLifecycleListener _lifecycle;
  VoidCallback? _cancelTickers;
  int? _exchangeId;
  bool _visible = false;
  bool _primed = false;

  @override
  void initState() {
    super.initState();
    _lifecycle = AppLifecycleListener(onResume: _onResume);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final exchange = ExchangeIds.byKey(
      GoRouterState.of(context).uri.queryParameters['exchange'],
    );
    // go_router 의 `indexedStack` 셸은 선택되지 않은 브랜치를 `TickerMode(enabled: false)` 로
    // 감싼다. 숨은 탭에서 티커를 받으면 보이지도 않는 행을 계속 리빌드한다.
    _sync(exchange.id, TickerMode.valuesOf(context).enabled);
  }

  @override
  void dispose() {
    _lifecycle.dispose();
    _cancelTickers?.call();
    _search.dispose();
    super.dispose();
  }

  void _sync(int exchangeId, bool visible) {
    if (_exchangeId == exchangeId && _visible == visible) return;
    final store = ref.read(tickerStoreProvider);

    _cancelTickers?.call();
    _cancelTickers = null;

    if (!visible) {
      store.setActive(false);
      _exchangeId = exchangeId;
      _visible = false;
      return;
    }

    store.setActive(true);
    _cancelTickers = ref
        .read(stompServiceProvider)
        .subscribe(
          '/topic/tickers.$exchangeId',
          (body) => store.ingest(decodeTickers(body)),
        );

    // 탭 재활성·거래소 전환 시 REST 스냅샷을 한 번 다시 받는다. 구독이 끊긴 동안의 틱을
    // 보정한다(계획서 §4.2.3). 첫 진입은 provider 가 알아서 조회하므로 건드리지 않는다.
    if (_primed) _refetch(exchangeId);

    _exchangeId = exchangeId;
    _visible = visible;
    _primed = true;
  }

  /// 포그라운드 복귀 — 누락 틱 보정(계획서 §4.2.2). 소켓 재연결은 `StompService` 가 한다.
  void _onResume() {
    final exchangeId = _exchangeId;
    if (!_visible || exchangeId == null) return;
    _refetch(exchangeId);
  }

  void _refetch(int exchangeId) {
    // provider 무효화는 프레임 밖에서 한다.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) ref.invalidate(marketCoinsProvider(exchangeId));
    });
  }

  void _switchExchange(Exchange exchange) {
    _search.clear();
    setState(() => _view = _view.forExchangeSwitch());
    context.go('${Routes.market}?exchange=${exchange.key}');
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final exchange = ExchangeIds.byKey(
      GoRouterState.of(context).uri.queryParameters['exchange'],
    );
    final coins = ref.watch(marketCoinsProvider(exchange.id));
    final round = ref.watch(
      roundControllerProvider.select((state) => state.activeRound),
    );

    return Scaffold(
      appBar: AppBar(title: const Text('코인 시세'), actions: const [MypageButton()]),
      body: Column(
        children: [
          if (round == null)
            const _RoundStartBanner()
          else if (round.emergencyFundingLimit > 0)
            _EmergencyBanner(round: round, exchange: exchange),
          Padding(
            padding: const EdgeInsets.fromLTRB(
              TryptoSpacing.screen,
              TryptoSpacing.md,
              TryptoSpacing.screen,
              TryptoSpacing.sm,
            ),
            child: Column(
              children: [
                CoinSearchField(
                  controller: _search,
                  onChanged: (query) =>
                      setState(() => _view = _view.withQuery(query)),
                ),
                const SizedBox(height: TryptoSpacing.sm),
                ExchangeSegment<int>(
                  items: [
                    for (final item in ExchangeIds.all)
                      SegmentItem(item.id, item.name),
                  ],
                  value: exchange.id,
                  onChanged: (id) =>
                      _switchExchange(ExchangeIds.byId(id) ?? exchange),
                ),
                const SizedBox(height: TryptoSpacing.sm),
                Align(
                  alignment: Alignment.centerLeft,
                  child: Text(
                    '${exchange.name} 기준 · ${exchange.baseCurrency} 마켓',
                    style: theme.textTheme.labelMedium?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: AsyncView<List<CoinEntry>>(
              value: coins,
              onRetry: () => ref.invalidate(marketCoinsProvider(exchange.id)),
              builder: (data) => _CoinList(
                coins: data,
                view: _view,
                exchange: exchange,
                onFilter: (filter) =>
                    setState(() => _view = _view.withFilter(filter)),
                onSort: (key) => setState(() => _view = _view.sortBy(key)),
                onRefresh: () =>
                    ref.refresh(marketCoinsProvider(exchange.id).future),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _CoinList extends ConsumerStatefulWidget {
  const _CoinList({
    required this.coins,
    required this.view,
    required this.exchange,
    required this.onFilter,
    required this.onSort,
    required this.onRefresh,
  });

  final List<CoinEntry> coins;
  final MarketView view;
  final Exchange exchange;
  final ValueChanged<MarketFilter> onFilter;
  final ValueChanged<MarketSortKey> onSort;
  final Future<void> Function() onRefresh;

  @override
  ConsumerState<_CoinList> createState() => _CoinListState();
}

class _CoinListState extends ConsumerState<_CoinList> {
  final ScrollController _scroll = ScrollController();
  late TickerStore _store;
  late List<CoinEntry> _rows;
  Timer? _resort;

  @override
  void initState() {
    super.initState();
    _store = ref.read(tickerStoreProvider);
    _seed();
    _resort = Timer.periodic(_kResortInterval, (_) => _reorder());
  }

  @override
  void didUpdateWidget(_CoinList oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!identical(widget.coins, oldWidget.coins)) {
      _seed();
    } else if (!identical(widget.view, oldWidget.view)) {
      _rows = _apply();
    }
  }

  @override
  void dispose() {
    _resort?.cancel();
    _scroll.dispose();
    super.dispose();
  }

  /// REST 스냅샷이 기준이다. 티커 맵을 비우고 이 목록으로 다시 채운다.
  void _seed() {
    _store.switchExchange([for (final entry in widget.coins) entry.coin]);
    _rows = _apply();
  }

  List<CoinEntry> _apply() =>
      applyMarketView(widget.coins, widget.view, quote: _store.quote);

  void _reorder() {
    if (!_store.orderDirty) return;
    // 손가락 밑에서 행이 튀면 오탭이 난다. dirty 를 남겨 두고 스크롤이 멈춘 뒤에 반영한다.
    if (_scroll.hasClients && _scroll.position.isScrollingNotifier.value) return;
    _store.clearOrderDirty();
    setState(() => _rows = _apply());
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        OverviewCards(
          coins: widget.coins,
          store: _store,
          baseCurrency: widget.exchange.baseCurrency,
        ),
        _ListControls(
          view: widget.view,
          onFilter: widget.onFilter,
          onSort: widget.onSort,
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: widget.onRefresh,
            child: _rows.isEmpty
                ? ListView(
                    children: const [
                      SizedBox(height: 120),
                      EmptyView(
                        icon: LucideIcons.search,
                        message: '검색 결과가 없습니다.',
                      ),
                    ],
                  )
                : ListView.builder(
                    controller: _scroll,
                    // 자식을 측정하지 않는다. 600행에서 스크롤 위치 계산이 O(1) 이 된다.
                    itemExtent: kCoinRowHeight,
                    itemCount: _rows.length,
                    itemBuilder: (context, index) {
                      final entry = _rows[index];
                      return CoinRow(
                        symbol: entry.symbol,
                        name: entry.name,
                        row: _store.row(entry.symbol)!,
                        flash: _store.flash(entry.symbol)!,
                        baseCurrency: widget.exchange.baseCurrency,
                        onTap: () => context.push(
                          Routes.coinDetail(entry.symbol, widget.exchange.key),
                        ),
                      );
                    },
                  ),
          ),
        ),
      ],
    );
  }
}

class _ListControls extends StatelessWidget {
  const _ListControls({
    required this.view,
    required this.onFilter,
    required this.onSort,
  });

  final MarketView view;
  final ValueChanged<MarketFilter> onFilter;
  final ValueChanged<MarketSortKey> onSort;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Padding(
      padding: const EdgeInsets.fromLTRB(
        TryptoSpacing.screen,
        TryptoSpacing.sm,
        TryptoSpacing.sm,
        TryptoSpacing.sm,
      ),
      child: Row(
        children: [
          for (final filter in MarketFilter.values) ...[
            ChoiceChip(
              label: Text(filter.label),
              selected: view.filter == filter,
              labelStyle: TextStyle(
                fontSize: 12,
                fontWeight:
                    view.filter == filter ? FontWeight.w700 : FontWeight.w600,
                color: view.filter == filter
                    ? theme.colorScheme.primary
                    : theme.colorScheme.onSurface,
              ),
              onSelected: (_) => onFilter(filter),
            ),
            const SizedBox(width: TryptoSpacing.xs),
          ],
          const Spacer(),
          PopupMenuButton<MarketSortKey>(
            tooltip: '정렬',
            position: PopupMenuPosition.under,
            onSelected: onSort,
            itemBuilder: (context) => [
              for (final key in MarketSortKey.values)
                PopupMenuItem(
                  value: key,
                  child: Row(
                    children: [
                      Text(key.label),
                      const Spacer(),
                      if (view.sortKey == key)
                        Icon(
                          view.descending
                              ? LucideIcons.arrowDown
                              : LucideIcons.arrowUp,
                          size: 14,
                          color: theme.colorScheme.primary,
                        ),
                    ],
                  ),
                ),
            ],
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(view.sortKey.label, style: theme.textTheme.labelMedium),
                Icon(
                  view.descending ? LucideIcons.arrowDown : LucideIcons.arrowUp,
                  size: 14,
                ),
                const SizedBox(width: TryptoSpacing.xs),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// 사용 빈도(라운드당 최대 3회)에 비해 웹 카드는 자리를 너무 차지한다. 남은 횟수만 보여주는
/// 접힌 배너로 줄이고 나머지는 바텀시트로 옮긴다(계획서 §4.6.1).
class _EmergencyBanner extends StatelessWidget {
  const _EmergencyBanner({required this.round, required this.exchange});

  final ActiveRound round;
  final Exchange exchange;

  /// 웹 `canCharge` 와 같은 판별식이다(사양서 §4.5).
  bool get _canCharge =>
      round.status == RoundStatus.active && round.emergencyChargeCount > 0;

  Future<void> _open(BuildContext context) async {
    final charged = await showEmergencyFundingSheet(context, exchange: exchange);
    if (charged == null || !context.mounted) return;
    showAppSnackbar(context, '${formatKRW(charged.toDouble())}을 투입했습니다.');
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final warning = context.tryptoColors.warning;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: TryptoSpacing.screen,
        vertical: TryptoSpacing.sm,
      ),
      color: warning.withValues(alpha: 0.08),
      child: Row(
        children: [
          Icon(LucideIcons.shieldPlus, size: 16, color: warning),
          const SizedBox(width: TryptoSpacing.sm),
          Expanded(
            child: Text(
              _canCharge
                  ? '긴급 자금 · 남은 횟수 ${round.emergencyChargeCount}회'
                  : '긴급 자금 · 3회를 모두 사용했습니다',
              style: theme.textTheme.labelLarge,
            ),
          ),
          FilledButton(
            style: TryptoButtons.secondary,
            onPressed: _canCharge ? () => _open(context) : null,
            child: const Text('투입'),
          ),
        ],
      ),
    );
  }
}

/// 라운드가 없어도 시세 조회는 계속된다 — 주문만 막힌다(사양서 §7.4.2). 하단 탭 바에 자리가
/// 없으므로 웹 헤더의 "라운드 시작" 버튼을 이 배너가 대신한다.
class _RoundStartBanner extends StatelessWidget {
  const _RoundStartBanner();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: TryptoSpacing.screen,
        vertical: TryptoSpacing.md,
      ),
      color: theme.colorScheme.primary.withValues(alpha: 0.08),
      child: Row(
        children: [
          Icon(LucideIcons.flag, size: 16, color: theme.colorScheme.primary),
          const SizedBox(width: TryptoSpacing.sm),
          Expanded(
            child: Text(
              '진행 중인 라운드가 없습니다.',
              style: theme.textTheme.labelLarge,
            ),
          ),
          FilledButton(
            onPressed: () => context.go(Routes.roundNew),
            child: const Text('라운드 시작'),
          ),
        ],
      ),
    );
  }
}
