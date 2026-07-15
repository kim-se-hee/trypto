import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api/api_exception.dart';
import '../../core/format/formatters.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/empty_view.dart';
import '../../core/widgets/numeric_text.dart';
import '../../models/ranking.dart';

/// 서버가 101위 이하에 403 `PORTFOLIO_VIEW_NOT_ALLOWED` 를 낸다. 요청을 보내기 전에 순위로
/// 선제 차단한다(계획서 §4.1.4).
const int rankingPortfolioMaxRank = 100;

bool canViewRankerPortfolio(int rank) => rank <= rankingPortfolioMaxRank;

/// 서버가 비중을 `0.35` 로 주든 `35` 로 주든 35.0% 로 렌더한다(사양서 §6.2.7).
double asRatio(double value) => value > 1 ? value / 100 : value;

/// 행 확장(ExpansionTile)이 아니라 바텀시트다 — 좁은 화면에서 확장은 스크롤 위치를 흔든다.
/// [load] 는 호출부가 유저별 캐시를 물려 준다(사양서 §6.2.7 — 유저당 1회 조회).
Future<void> showRankerPortfolioSheet(
  BuildContext context, {
  required RankingItem item,
  required Future<RankerPortfolio> Function() load,
}) {
  return showModalBottomSheet<void>(
    context: context,
    useSafeArea: true,
    isScrollControlled: true,
    builder: (context) => _RankerPortfolioSheet(item: item, load: load),
  );
}

class _RankerPortfolioSheet extends StatefulWidget {
  const _RankerPortfolioSheet({required this.item, required this.load});

  final RankingItem item;
  final Future<RankerPortfolio> Function() load;

  @override
  State<_RankerPortfolioSheet> createState() => _RankerPortfolioSheetState();
}

class _RankerPortfolioSheetState extends State<_RankerPortfolioSheet> {
  late Future<RankerPortfolio> _future = widget.load();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final item = widget.item;

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(
          TryptoSpacing.screen,
          0,
          TryptoSpacing.screen,
          TryptoSpacing.screen,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                RankBadge(rank: item.rank),
                const SizedBox(width: TryptoSpacing.md),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.nickname,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: theme.textTheme.titleMedium,
                      ),
                      Text(
                        '${item.tradeCount}회 거래',
                        style: theme.textTheme.labelSmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                NumericText(
                  formatProfitPercent(item.profitRate),
                  size: 18,
                  weight: FontWeight.w700,
                  color: context.profitColor(item.profitRate),
                ),
              ],
            ),
            const SizedBox(height: TryptoSpacing.lg),
            Text('보유 자산 비중', style: theme.textTheme.titleMedium),
            const SizedBox(height: TryptoSpacing.sm),
            ConstrainedBox(
              constraints: BoxConstraints(
                maxHeight: MediaQuery.sizeOf(context).height * 0.5,
              ),
              child: FutureBuilder<RankerPortfolio>(
                future: _future,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Padding(
                      padding: EdgeInsets.all(TryptoSpacing.xxl),
                      child: Center(
                        child: Text('포트폴리오를 불러오는 중입니다...'),
                      ),
                    );
                  }
                  final error = snapshot.error;
                  if (error != null) {
                    return EmptyView(
                      icon: LucideIcons.circleAlert,
                      message: error.asApiException.isCode(
                        ErrorCodes.rankingNotFound,
                      )
                          ? '집계된 포트폴리오가 없습니다.'
                          : '포트폴리오를 불러오지 못했습니다.',
                      action: OutlinedButton(
                        onPressed: () =>
                            setState(() => _future = widget.load()),
                        child: const Text('다시 시도'),
                      ),
                    );
                  }
                  final holdings = snapshot.requireData.holdings;
                  if (holdings.isEmpty) {
                    return const EmptyView(
                      icon: LucideIcons.coins,
                      message: '보유 자산 정보가 없습니다.',
                    );
                  }
                  return ListView.separated(
                    shrinkWrap: true,
                    itemCount: holdings.length,
                    separatorBuilder: (context, index) =>
                        const SizedBox(height: TryptoSpacing.md),
                    itemBuilder: (context, index) =>
                        _HoldingRow(holding: holdings[index]),
                  );
                },
              ),
            ),
            const SizedBox(height: TryptoSpacing.md),
            Text(
              '보유 수량은 공개되지 않습니다.',
              style: theme.textTheme.labelSmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _HoldingRow extends StatelessWidget {
  const _HoldingRow({required this.holding});

  final RankerHolding holding;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final ratio = asRatio(holding.assetRatio);
    final percent = (ratio * 100).clamp(0.0, 100.0);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                holding.coinSymbol,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TryptoText.symbol,
              ),
            ),
            NumericText(
              formatProfitPercent(holding.profitRate),
              size: 12,
              weight: FontWeight.w500,
              color: context.profitColor(holding.profitRate),
            ),
            const SizedBox(width: TryptoSpacing.sm),
            NumericText('${(ratio * 100).toStringAsFixed(1)}%', size: 13),
          ],
        ),
        const SizedBox(height: TryptoSpacing.xs),
        ClipRRect(
          borderRadius: BorderRadius.circular(TryptoRadius.base),
          child: LinearProgressIndicator(
            value: percent / 100,
            minHeight: 6,
            backgroundColor: theme.colorScheme.surfaceContainer,
          ),
        ),
      ],
    );
  }
}

/// 순위 배지 32×32. 1~3위는 금·은·동으로 갈린다(사양서 §6.6.1-8).
class RankBadge extends StatelessWidget {
  const RankBadge({super.key, required this.rank, this.size = 32});

  final int rank;
  final double size;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.tryptoColors;

    final (Color background, Color foreground) = switch (rank) {
      1 => (colors.warning, Colors.white),
      2 => (const Color(0xFFB8B8C4), Colors.white),
      3 => (const Color(0xFFB08D57), Colors.white),
      _ => (theme.colorScheme.surfaceContainer, theme.colorScheme.onSurface),
    };

    return Container(
      width: size,
      height: size,
      alignment: Alignment.center,
      decoration: BoxDecoration(color: background, shape: BoxShape.circle),
      child: NumericText(
        '$rank',
        size: size <= 32 ? 13 : 16,
        weight: FontWeight.w700,
        color: foreground,
      ),
    );
  }
}
