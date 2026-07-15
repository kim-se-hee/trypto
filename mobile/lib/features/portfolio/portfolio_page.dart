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
import '../round/round_controller.dart';
import 'donut_painter.dart';
import 'holding_card.dart';
import 'portfolio_repository.dart';
import 'portfolio_summary.dart';

/// 선택 거래소는 라우트 쿼리(`/portfolio?exchange=upbit`)에 둔다 — 마켓과 같은 규칙이다.
///
/// 이 화면은 스스로 갱신되지 않는다(사양서 §5.1.2 — 폴링·WS 없음). 당김 새로고침이 유일한
/// 수동 갱신 경로이므로 반드시 있어야 한다.
class PortfolioPage extends ConsumerStatefulWidget {
  const PortfolioPage({super.key});

  @override
  ConsumerState<PortfolioPage> createState() => _PortfolioPageState();
}

class _PortfolioPageState extends ConsumerState<PortfolioPage> {
  HoldingSortKey _sortKey = HoldingSortKey.evalAmount;
  bool _descending = true;

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
    final exchange = ExchangeIds.byKey(
      GoRouterState.of(context).uri.queryParameters['exchange'],
    );
    final round = ref.watch(
      roundControllerProvider.select((state) => state.activeRound),
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
                ExchangeSegment<int>(
                  items: [
                    for (final item in ExchangeIds.all)
                      SegmentItem(item.id, item.name),
                  ],
                  value: exchange.id,
                  onChanged: (id) => context.go(
                    '${Routes.portfolio}?exchange='
                    '${(ExchangeIds.byId(id) ?? exchange).key}',
                  ),
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
                    value: formatCurrencyCompact(
                      summary.totalBuy.toDouble(),
                      base,
                    ),
                  ),
                ),
                Expanded(
                  child: _SummaryCell(
                    label: '총평가',
                    value: formatCurrencyCompact(
                      summary.totalEval.toDouble(),
                      base,
                    ),
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
                        '${formatCurrencyCompact(profitLoss, base)}',
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
