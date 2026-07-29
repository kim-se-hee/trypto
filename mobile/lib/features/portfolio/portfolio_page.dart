import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/constants/exchanges.dart';
import '../../core/format/formatters.dart';
import '../../core/router/guard.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/empty_view.dart';
import '../../core/widgets/exchange_segment.dart';
import '../../core/widgets/mypage_button.dart';
import '../../core/widgets/no_round_notice.dart';
import '../../core/widgets/numeric_text.dart';
import '../../models/round.dart';
import '../round/round_controller.dart';
import 'donut_painter.dart';
import 'holding_card.dart';
import 'portfolio_repository.dart';
import 'portfolio_summary.dart';

/// 선택 거래소는 라우트 쿼리(`/portfolio?exchange=upbit`)에 둔다 — 마켓과 같은 규칙이다.
/// 탭 목록은 이 라운드가 자금을 배정한 지갑이 정한다.
///
/// 이 화면은 스스로 갱신되지 않는다(사양서 §5.1.2 — 폴링·WS 없음). 탭 재활성·거래소 전환·
/// 포그라운드 복귀에서 보유 스냅샷을 다시 받고, 당김 새로고침이 마지막 수동 경로다.
class PortfolioPage extends ConsumerStatefulWidget {
  const PortfolioPage({super.key});

  @override
  ConsumerState<PortfolioPage> createState() => _PortfolioPageState();
}

class _PortfolioPageState extends ConsumerState<PortfolioPage> {
  HoldingSortKey _sortKey = HoldingSortKey.evalAmount;
  bool _descending = true;

  late final AppLifecycleListener _lifecycle;
  int? _walletId;
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
    // go_router 의 `indexedStack` 셸은 선택되지 않은 브랜치를 `TickerMode(enabled: false)` 로
    // 감싼다. 탭이 숨어 있는 동안 체결된 주문은 이 화면 캐시에 들어오지 않는다.
    _sync(_currentWalletId(), TickerMode.valuesOf(context).enabled);
  }

  @override
  void dispose() {
    _lifecycle.dispose();
    super.dispose();
  }

  /// `portfolioProvider` 는 autoDispose 가 아니고 셸이 이 화면을 살려 둔다. 탭 재활성·거래소
  /// 전환에서 무효화하지 않으면 매수 체결 이전 스냅샷이 그대로 남는다.
  void _sync(int? walletId, bool visible) {
    if (_walletId == walletId && _visible == visible) return;
    // 첫 진입은 provider 가 알아서 조회한다. 그 뒤의 전환만 다시 받는다.
    final refetch = _primed && visible;
    _walletId = walletId;
    _visible = visible;
    _primed = true;
    if (refetch && walletId != null) _refetch(walletId);
  }

  /// 포그라운드 복귀 — 백그라운드 동안 지연 체결된 지정가 주문을 들인다(계획서 §4.2.2).
  void _onResume() {
    if (!_visible) return;
    final walletId = _currentWalletId();
    if (walletId == null) return;
    _refetch(walletId);
  }

  void _refetch(int walletId) {
    // provider 무효화는 프레임 밖에서 한다.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) ref.invalidate(portfolioProvider(walletId));
    });
  }

  /// 지금 보고 있는 지갑. 라운드와 라우트 쿼리에서 매번 다시 구한다.
  int? _currentWalletId() {
    final round = ref.read(roundControllerProvider).activeRound;
    final exchange = _selected(
      _tabs(round),
      GoRouterState.of(context).uri.queryParameters['exchange'],
    );
    return round?.walletIdOf(exchange.id);
  }

  /// 거래소 탭은 이 라운드의 지갑 목록이 정한다 — 자금을 배정하지 않은 거래소는 탭도 없다.
  /// 상수 표에 없는 `exchangeId` 는 이름·키를 id 문자열로 떨어뜨려서라도 탭을 낸다.
  List<Exchange> _tabs(ActiveRound? round) {
    if (round == null) return const [];
    return [
      for (final wallet in round.wallets)
        ExchangeIds.byId(wallet.exchangeId) ??
            Exchange(
              id: wallet.exchangeId,
              key: '${wallet.exchangeId}',
              name: '${wallet.exchangeId}',
              baseCurrency: '',
              feeRate: 0,
              candleCode: '',
            ),
    ];
  }

  /// 쿼리에 실린 거래소를 고른다. 쿼리가 없으면 지갑 목록의 첫 거래소가 기본값이다 —
  /// 업비트 지갑이 없는 라운드에서 빈 화면으로 시작하지 않는다. 지갑이 없는 거래소를
  /// 직접 지정한 경우는 그대로 두고 본문의 '지갑이 없습니다' 안내로 넘긴다.
  Exchange _selected(List<Exchange> tabs, String? key) {
    for (final tab in tabs) {
      if (tab.key == key) return tab;
    }
    if (key == null && tabs.isNotEmpty) return tabs.first;
    return ExchangeIds.byKey(key);
  }

  Future<void> _openSortSheet() async {
    final selected = await showModalBottomSheet<HoldingSortKey>(
      context: context,
      useSafeArea: true,
      builder: (context) => _SortSheet(sortKey: _sortKey, descending: _descending),
    );
    if (selected == null) return;
    setState(() {
      // 같은 키를 다시 고르면 방향을 뒤집고, 다른 키면 내림차순으로 되돌린다(§5.1.5).
      _descending = selected == _sortKey ? !_descending : true;
      _sortKey = selected;
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final round = ref.watch(
      roundControllerProvider.select((state) => state.activeRound),
    );
    final tabs = _tabs(round);
    final exchange = _selected(
      tabs,
      GoRouterState.of(context).uri.queryParameters['exchange'],
    );
    final walletId = round?.walletIdOf(exchange.id);

    return Scaffold(
      appBar: AppBar(
        title: const Text('투자내역'),
        actions: const [MypageButton()],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(
              TryptoSpacing.screen,
              TryptoSpacing.md,
              TryptoSpacing.screen,
              TryptoSpacing.sm,
            ),
            child: Column(
              children: [
                ExchangeSegment<String>(
                  items: [
                    for (final tab in tabs) SegmentItem(tab.key, tab.name),
                  ],
                  value: exchange.key,
                  onChanged: (key) =>
                      context.go('${Routes.portfolio}?exchange=$key'),
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
          Expanded(child: _body(exchange, round == null, walletId)),
        ],
      ),
    );
  }

  Widget _body(Exchange exchange, bool noRound, int? walletId) {
    if (noRound) {
      return const NoRoundNotice(
        message: '진행 중인 라운드가 없어 포트폴리오가 비어 있습니다.',
      );
    }
    if (walletId == null) {
      return EmptyView(
        icon: LucideIcons.wallet,
        message: '${exchange.name} 지갑이 없습니다.',
        description: '이 라운드에서 자금을 배정하지 않은 거래소입니다.',
      );
    }

    return AsyncView<PortfolioSummary>(
      value: ref.watch(portfolioProvider(walletId)),
      onRetry: () => ref.invalidate(portfolioProvider(walletId)),
      builder: (summary) => RefreshIndicator(
        onRefresh: () => ref.refresh(portfolioProvider(walletId).future),
        child: _Content(
          summary: summary,
          exchange: exchange,
          sortKey: _sortKey,
          descending: _descending,
          onSort: _openSortSheet,
        ),
      ),
    );
  }
}

class _Content extends StatelessWidget {
  const _Content({
    required this.summary,
    required this.exchange,
    required this.sortKey,
    required this.descending,
    required this.onSort,
  });

  final PortfolioSummary summary;
  final Exchange exchange;
  final HoldingSortKey sortKey;
  final bool descending;
  final VoidCallback onSort;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final holdings = sortHoldings(
      summary.holdings,
      sortKey,
      descending: descending,
    );

    return CustomScrollView(
      physics: const AlwaysScrollableScrollPhysics(),
      slivers: [
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(
            TryptoSpacing.screen,
            TryptoSpacing.sm,
            TryptoSpacing.screen,
            TryptoSpacing.md,
          ),
          sliver: SliverToBoxAdapter(child: _SummaryCard(summary: summary)),
        ),
        SliverPadding(
          padding: const EdgeInsets.symmetric(horizontal: TryptoSpacing.screen),
          sliver: SliverToBoxAdapter(
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(TryptoSpacing.lg),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('자산 구성', style: theme.textTheme.titleMedium),
                    const SizedBox(height: TryptoSpacing.lg),
                    // 보유 코인이 0개여도 현금 한 조각으로 정상 렌더된다.
                    DonutChart(
                      segments: buildDonutSegments(summary),
                      totalAsset: summary.totalAsset.toDouble(),
                      baseCurrency: summary.baseCurrency,
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
        if (holdings.isEmpty)
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: TryptoSpacing.xxl),
              child: EmptyView(
                icon: LucideIcons.coins,
                message: '보유 중인 코인이 없습니다.',
                action: FilledButton(
                  onPressed: () =>
                      context.go('${Routes.market}?exchange=${exchange.key}'),
                  child: const Text('코인 사러 가기'),
                ),
              ),
            ),
          )
        else ...[
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(
              TryptoSpacing.screen,
              TryptoSpacing.lg,
              TryptoSpacing.screen,
              TryptoSpacing.sm,
            ),
            sliver: SliverToBoxAdapter(
              child: Row(
                children: [
                  Text(
                    '보유 종목 ${holdings.length}',
                    style: theme.textTheme.titleMedium,
                  ),
                  const Spacer(),
                  ActionChip(
                    avatar: Icon(
                      descending ? LucideIcons.arrowDown : LucideIcons.arrowUp,
                      size: 12,
                    ),
                    label: Text('정렬: ${sortKey.label}'),
                    onPressed: onSort,
                  ),
                ],
              ),
            ),
          ),
          SliverPadding(
            padding: const EdgeInsets.symmetric(
              horizontal: TryptoSpacing.screen,
            ),
            sliver: SliverList.separated(
              itemCount: holdings.length,
              separatorBuilder: (context, index) =>
                  const SizedBox(height: TryptoSpacing.sm),
              itemBuilder: (context, index) => HoldingCard(
                holding: holdings[index],
                baseCurrency: summary.baseCurrency,
                onTap: () => context.push(
                  Routes.coinDetail(holdings[index].symbol, exchange.key),
                ),
              ),
            ),
          ),
        ],
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.all(TryptoSpacing.screen),
            child: Text(
              '* 모의투자 데이터입니다.',
              style: theme.textTheme.labelSmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _SummaryCard extends StatelessWidget {
  const _SummaryCard({required this.summary});

  final PortfolioSummary summary;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final base = summary.baseCurrency;
    final profitLoss = summary.profitLoss.toDouble();
    final rate = summary.profitRate;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(TryptoSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '보유 $base ${formatCurrency(summary.cash.toDouble(), base)}',
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: TryptoSpacing.sm),
            Text(
              '총 보유자산',
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: TryptoSpacing.xs),
            NumericText(
              formatCurrency(summary.totalAsset.toDouble(), base),
              size: 24,
              weight: FontWeight.w700,
            ),
            const SizedBox(height: TryptoSpacing.xs),
            Text(
              '보유 $base + 코인 평가 합계',
              style: theme.textTheme.labelSmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: TryptoSpacing.lg),
            Row(
              children: [
                Expanded(
                  child: _SummaryCell(
                    label: '총매수',
                    value: formatCurrency(summary.totalBuy.toDouble(), base),
                  ),
                ),
                Expanded(
                  child: _SummaryCell(
                    label: '총평가',
                    value: formatCurrency(summary.totalEval.toDouble(), base),
                  ),
                ),
              ],
            ),
            const SizedBox(height: TryptoSpacing.md),
            Row(
              children: [
                Expanded(
                  child: _SummaryCell(
                    label: '평가손익',
                    value:
                        '${profitLoss > 0 ? '+' : ''}'
                        '${formatCurrency(profitLoss, base)}',
                    color: context.profitColor(profitLoss),
                  ),
                ),
                Expanded(
                  child: _SummaryCell(
                    label: '수익률',
                    value:
                        '${rate > 0 ? '+' : ''}${rate.toStringAsFixed(2)}%',
                    color: context.profitColor(rate),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _SummaryCell extends StatelessWidget {
  const _SummaryCell({required this.label, required this.value, this.color});

  final String label;
  final String value;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: theme.textTheme.labelSmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 2),
        NumericText(value, size: 14, weight: FontWeight.w600, color: color),
      ],
    );
  }
}

class _SortSheet extends StatelessWidget {
  const _SortSheet({required this.sortKey, required this.descending});

  final HoldingSortKey sortKey;
  final bool descending;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return SafeArea(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(
              TryptoSpacing.screen,
              0,
              TryptoSpacing.screen,
              TryptoSpacing.sm,
            ),
            child: Text('정렬 기준', style: theme.textTheme.titleLarge),
          ),
          for (final key in HoldingSortKey.values)
            ListTile(
              title: Text(key.label),
              trailing: key == sortKey
                  ? Icon(
                      descending ? LucideIcons.arrowDown : LucideIcons.arrowUp,
                      size: 16,
                      color: theme.colorScheme.primary,
                    )
                  : null,
              onTap: () => Navigator.of(context).pop(key),
            ),
          const SizedBox(height: TryptoSpacing.sm),
        ],
      ),
    );
  }
}
