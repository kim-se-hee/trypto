import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/constants/exchanges.dart';
import '../../core/router/guard.dart';
import '../../core/theme/theme.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/empty_view.dart';
import '../../core/widgets/exchange_segment.dart';
import '../../core/widgets/mypage_button.dart';
import '../round/round_controller.dart';
import 'coin_row.dart';
import 'coin_search_field.dart';
import 'market_controller.dart';
import 'overview_cards.dart';

/// 선택 거래소는 라우트 쿼리(`/market?exchange=upbit`)에 둔다 — 딥링크와 복원이 공짜로 된다.
///
/// 이 단위의 시세는 REST 스냅샷 하나뿐이다. STOMP 구독과 `TickerStore` 는 9단위에서
/// [CoinNumbers] 만 갈아 끼우는 방식으로 얹힌다.
class MarketPage extends ConsumerStatefulWidget {
  const MarketPage({super.key});

  @override
  ConsumerState<MarketPage> createState() => _MarketPageState();
}

class _MarketPageState extends ConsumerState<MarketPage> {
  final TextEditingController _search = TextEditingController();
  MarketView _view = const MarketView();

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
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
    final hasRound = ref.watch(
      roundControllerProvider.select((round) => round.hasActive),
    );

    return Scaffold(
      appBar: AppBar(title: const Text('코인 시세'), actions: const [MypageButton()]),
      body: Column(
        children: [
          if (!hasRound) const _RoundStartBanner(),
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

class _CoinList extends StatelessWidget {
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
  Widget build(BuildContext context) {
    final rows = applyMarketView(coins, view);

    return Column(
      children: [
        OverviewCards(coins: coins, baseCurrency: exchange.baseCurrency),
        _ListControls(view: view, onFilter: onFilter, onSort: onSort),
        Expanded(
          child: RefreshIndicator(
            onRefresh: onRefresh,
            child: rows.isEmpty
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
                    // 자식을 측정하지 않는다. 600행에서 스크롤 위치 계산이 O(1) 이 된다.
                    itemExtent: kCoinRowHeight,
                    itemCount: rows.length,
                    itemBuilder: (context, index) {
                      final entry = rows[index];
                      return CoinRow(
                        symbol: entry.symbol,
                        name: entry.name,
                        price: entry.price,
                        changeRate: entry.changeRate,
                        volume: entry.volume,
                        baseCurrency: exchange.baseCurrency,
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
