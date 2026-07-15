import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/constants/exchanges.dart';
import '../../core/format/formatters.dart';
import '../../core/router/guard.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/app_snackbar.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/empty_view.dart';
import '../../core/widgets/exchange_segment.dart';
import '../../core/widgets/mypage_button.dart';
import '../../core/widgets/no_round_notice.dart';
import '../../core/widgets/numeric_text.dart';
import '../../models/round.dart';
import '../market/coin_search_field.dart';
import '../round/round_controller.dart';
import 'asset_detail_sheet.dart';
import 'transfer_history_page.dart';
import 'transfer_wizard/transfer_draft.dart';
import 'wallet_assets.dart';

const double _kAssetRowHeight = 72;

/// 지갑 자산 목록과 송금. 선택 거래소는 라우트 쿼리(`/wallet?exchange=upbit`)에 둔다.
class WalletPage extends ConsumerStatefulWidget {
  const WalletPage({super.key});

  @override
  ConsumerState<WalletPage> createState() => _WalletPageState();
}

class _WalletPageState extends ConsumerState<WalletPage> {
  final TextEditingController _search = TextEditingController();

  String _query = '';
  bool _hideSmall = false;
  bool _hidden = false;

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  void _switchExchange(Exchange exchange) {
    _search.clear();
    setState(() => _query = '');
    context.go('${Routes.wallet}?exchange=${exchange.key}');
  }

  /// 도착 후보 = 이 라운드의 지갑 중 현재 거래소를 뺀 전부(사양서 §5.2.6).
  List<TransferDestination> _destinations(ActiveRound round, Exchange current) {
    return [
      for (final wallet in round.wallets)
        if (wallet.exchangeId != current.id)
          if (ExchangeIds.byId(wallet.exchangeId) case final exchange?)
            TransferDestination(walletId: wallet.walletId, exchange: exchange),
    ];
  }

  Future<void> _openAsset(
    WalletAsset asset,
    WalletSnapshot snapshot,
    Exchange exchange,
    int walletId,
    ActiveRound round,
  ) async {
    final outcome = await showAssetDetailSheet(
      context,
      asset: asset,
      exchange: exchange,
      walletId: walletId,
      destinations: _destinations(round, exchange),
      transfers: snapshot.recentTransfers,
    );
    if (outcome == null || !mounted) return;

    // 출발·도착 두 지갑의 잔고가 모두 바뀐다. family 전체를 무효화해 다음 조회에서 새로 읽는다.
    ref.invalidate(walletSnapshotProvider);
    showAppSnackbar(
      context,
      '${formatQuantity(outcome.amount)} ${outcome.symbol} 출금 완료',
    );
  }

  void _openHistory(int walletId, Exchange exchange) {
    Navigator.of(context, rootNavigator: true).push(
      MaterialPageRoute<void>(
        builder: (context) => TransferHistoryPage(
          walletId: walletId,
          exchangeName: exchange.name,
        ),
      ),
    );
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
        title: const Text('입출금'),
        actions: [
          if (walletId != null)
            IconButton(
              icon: const Icon(LucideIcons.history),
              tooltip: '입출금 내역',
              onPressed: () => _openHistory(walletId, exchange),
            ),
          const MypageButton(),
        ],
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
                  onChanged: (id) =>
                      _switchExchange(ExchangeIds.byId(id) ?? exchange),
                ),
                const SizedBox(height: TryptoSpacing.sm),
                Align(
                  alignment: Alignment.centerLeft,
                  child: Text(
                    '자산 관리 · 입금/출금 내역 확인',
                    style: theme.textTheme.labelMedium?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ),
              ],
            ),
          ),
          Expanded(child: _body(exchange, round, walletId)),
        ],
      ),
    );
  }

  Widget _body(Exchange exchange, ActiveRound? round, int? walletId) {
    if (round == null) {
      return const NoRoundNotice(message: '진행 중인 라운드가 없어 지갑이 비어 있습니다.');
    }
    if (walletId == null) {
      return EmptyView(
        icon: LucideIcons.wallet,
        message: '${exchange.name} 지갑이 없습니다.',
        description: '이 라운드에서 자금을 배정하지 않은 거래소입니다.',
      );
    }

    final key = (exchangeId: exchange.id, walletId: walletId);
    return AsyncView<WalletSnapshot>(
      value: ref.watch(walletSnapshotProvider(key)),
      onRetry: () => ref.invalidate(walletSnapshotProvider(key)),
      builder: (snapshot) {
        final assets = applyWalletFilter(
          snapshot.assets,
          query: _query,
          hideSmall: _hideSmall,
          baseCurrency: snapshot.baseCurrency,
        );

        return Column(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: TryptoSpacing.screen,
              ),
              child: _SummaryCard(
                snapshot: snapshot,
                exchange: exchange,
                hidden: _hidden,
                onToggle: () => setState(() => _hidden = !_hidden),
              ),
            ),
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
                    onChanged: (query) => setState(() => _query = query),
                  ),
                  const SizedBox(height: TryptoSpacing.sm),
                  Row(
                    children: [
                      Text('보유 자산', style: Theme.of(context).textTheme.titleMedium),
                      const Spacer(),
                      FilterChip(
                        label: const Text('소액 제외'),
                        selected: _hideSmall,
                        onSelected: (selected) =>
                            setState(() => _hideSmall = selected),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            Expanded(
              child: RefreshIndicator(
                onRefresh: () => ref.refresh(walletSnapshotProvider(key).future),
                child: assets.isEmpty
                    ? ListView(
                        children: const [
                          SizedBox(height: 80),
                          EmptyView(
                            icon: LucideIcons.search,
                            message: '조건에 맞는 자산이 없습니다.',
                          ),
                        ],
                      )
                    : ListView.builder(
                        // 상장 코인 전량(600여 행)이 들어온다. 자식을 측정하지 않는다.
                        itemExtent: _kAssetRowHeight,
                        itemCount: assets.length,
                        itemBuilder: (context, index) => _AssetRow(
                          asset: assets[index],
                          baseCurrency: snapshot.baseCurrency,
                          onTap: () => _openAsset(
                            assets[index],
                            snapshot,
                            exchange,
                            walletId,
                            round,
                          ),
                        ),
                      ),
              ),
            ),
          ],
        );
      },
    );
  }
}

class _SummaryCard extends StatelessWidget {
  const _SummaryCard({
    required this.snapshot,
    required this.exchange,
    required this.hidden,
    required this.onToggle,
  });

  final WalletSnapshot snapshot;
  final Exchange exchange;
  final bool hidden;
  final VoidCallback onToggle;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final base = snapshot.baseCurrency;
    final baseAsset = snapshot.baseAsset;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(TryptoSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(
                  '${exchange.name} 총 자산',
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
                const Spacer(),
                IconButton(
                  icon: Icon(
                    hidden ? LucideIcons.eyeOff : LucideIcons.eye,
                    size: 16,
                  ),
                  tooltip: hidden ? '잔액 표시' : '잔액 숨기기',
                  onPressed: onToggle,
                ),
              ],
            ),
            const SizedBox(height: TryptoSpacing.xs),
            // 코인 평가액은 상장 목록의 `price` 로 계산한다. 웹은 0 고정이라 이 숫자가 사실상
            // 기준통화 잔고였다(사양서 R9-3).
            NumericText(
              hidden ? '••••••••' : formatCurrency(snapshot.totalValue, base),
              size: 26,
              weight: FontWeight.w700,
            ),
            const SizedBox(height: TryptoSpacing.sm),
            Text(
              hidden
                  ? '보유 $base ••••••••'
                  : '보유 $base ${formatCurrency(baseAsset.total, base)}',
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            if (!hidden && baseAsset.locked > 0) ...[
              const SizedBox(height: 2),
              Text(
                '(사용 가능 ${formatCurrency(baseAsset.available, base)} / '
                '잠금 ${formatCurrency(baseAsset.locked, base)})',
                style: theme.textTheme.labelSmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _AssetRow extends StatelessWidget {
  const _AssetRow({
    required this.asset,
    required this.baseCurrency,
    required this.onTap,
  });

  final WalletAsset asset;
  final String baseCurrency;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    // 잔고 0 인 코인은 흐리게 그린다. 알파는 색으로만 준다(계획서 §4.2.5-8).
    final alpha = asset.hasBalance || asset.isBase ? 1.0 : 0.4;
    final onSurface = theme.colorScheme.onSurface.withValues(alpha: alpha);
    final muted = theme.colorScheme.onSurfaceVariant.withValues(alpha: alpha);
    final quantity = asset.isBase
        ? formatGrouped(asset.total)
        : formatQuantity(asset.total);

    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: TryptoSpacing.screen),
        decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: TryptoPalette.border)),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    asset.symbol,
                    style: TryptoText.symbol.copyWith(color: onSurface),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    asset.name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.labelSmall?.copyWith(color: muted),
                  ),
                ],
              ),
            ),
            if (asset.locked > 0) ...[
              Icon(
                LucideIcons.lock,
                size: 12,
                color: context.tryptoColors.warning,
              ),
              const SizedBox(width: TryptoSpacing.xs),
            ],
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                NumericText(
                  asset.hasBalance ? quantity : '—',
                  size: 14,
                  weight: FontWeight.w600,
                  color: onSurface,
                ),
                const SizedBox(height: 2),
                NumericText(
                  asset.isBase || !asset.hasBalance
                      ? ''
                      : formatFiatEstimate(asset.totalValue, baseCurrency),
                  size: 11,
                  weight: FontWeight.w500,
                  color: muted,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
