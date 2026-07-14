import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/api/api_exception.dart';
import '../../core/format/formatters.dart';
import '../../core/router/guard.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/app_snackbar.dart';
import '../../core/widgets/empty_view.dart';
import '../../core/widgets/exchange_segment.dart';
import '../../core/widgets/mypage_button.dart';
import '../../core/widgets/numeric_text.dart';
import '../../models/enums.dart';
import '../../models/ranking.dart';
import '../auth/auth_controller.dart';
import 'ranker_portfolio_sheet.dart';
import 'ranking_repository.dart';

const int _kPageSize = 20;

const Map<RankingPeriod, String> _periodLabels = {
  RankingPeriod.daily: '일간',
  RankingPeriod.weekly: '주간',
  RankingPeriod.monthly: '월간',
};

/// 라우트 쿼리 값이라 무엇이든 들어올 수 있다. 웹과 같이 알 수 없는 값은 `daily` 로 강제한다
/// (사양서 §6.2.1).
RankingPeriod _periodOf(String? key) => switch (key) {
  'weekly' => RankingPeriod.weekly,
  'monthly' => RankingPeriod.monthly,
  _ => RankingPeriod.daily,
};

/// 기간은 라우트 쿼리(`/ranking?period=weekly`)에 유지한다 — 딥링크·뒤로가기가 공짜로 된다.
///
/// 목록·통계·내 랭킹은 **개별 실패를 허용**한다(사양서 §6.2.3). 목록만 전체 에러 화면으로
/// 전환하고, 통계와 내 랭킹은 각각 null 로 떨어뜨린다.
class RankingPage extends ConsumerStatefulWidget {
  const RankingPage({super.key});

  @override
  ConsumerState<RankingPage> createState() => _RankingPageState();
}

class _RankingPageState extends ConsumerState<RankingPage> {
  final ScrollController _scroll = ScrollController();
  final GlobalKey _myCardKey = GlobalKey();
  final ValueNotifier<bool> _stickyVisible = ValueNotifier(false);

  /// 유저별 1회 조회. 시트를 닫았다 다시 열어도 재요청하지 않는다(사양서 §6.2.7).
  final Map<int, RankerPortfolio> _portfolioCache = {};

  RankingPeriod? _period;
  List<RankingItem> _entries = [];
  MyRanking? _myRanking;
  RankingStats? _stats;
  int? _cursor;
  bool _hasNext = false;
  bool _loading = true;
  bool _loadingMore = false;
  String? _error;
  String? _moreError;
  int _generation = 0;
  double _myCardExtent = 0;

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_onScroll);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final period = _periodOf(
      GoRouterState.of(context).uri.queryParameters['period'],
    );
    if (period == _period) return;
    _period = period;
    _reload();
  }

  @override
  void dispose() {
    _scroll.dispose();
    _stickyVisible.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_scroll.hasClients) return;
    _stickyVisible.value = _myRanking != null && _scroll.offset > _myCardExtent;

    if (!_hasNext || _loadingMore || _loading) return;
    if (_scroll.position.pixels < _scroll.position.maxScrollExtent - 200) return;
    _loadMore();
  }

  /// 실패를 삼킨다. 통계·내 랭킹의 개별 실패가 화면 전체를 무너뜨리면 안 된다. 여기서 잡지
  /// 않으면 목록이 먼저 실패했을 때 남은 future 가 처리되지 않은 예외로 터진다.
  Future<T?> _optional<T>(Future<T> request) async {
    try {
      return await request;
    } on ApiException {
      return null;
    }
  }

  Future<void> _reload() async {
    final period = _period!;
    final generation = ++_generation;
    final repository = ref.read(rankingRepositoryProvider);
    final authed = ref.read(authControllerProvider).isAuthenticated;

    setState(() {
      _loading = true;
      _error = null;
      _moreError = null;
      _portfolioCache.clear();
    });

    final entries = repository.getRankings(period: period, size: _kPageSize);
    final stats = _optional(repository.getStats(period));
    final myRanking = authed
        ? _optional(repository.getMyRanking(period))
        : Future<MyRanking?>.value();

    try {
      final page = await entries;
      if (!mounted || generation != _generation) return;
      setState(() {
        _entries = page.content;
        _cursor = page.nextCursor;
        _hasNext = page.hasNext;
        _stats = null;
        _myRanking = null;
        _loading = false;
      });
    } on ApiException {
      if (!mounted || generation != _generation) return;
      setState(() {
        _entries = [];
        _hasNext = false;
        _stats = null;
        _myRanking = null;
        _error = '랭킹을 불러오지 못했습니다.';
        _loading = false;
      });
      return;
    }

    final loaded = await (stats, myRanking).wait;
    if (!mounted || generation != _generation) return;
    setState(() {
      _stats = loaded.$1;
      _myRanking = loaded.$2;
    });
    _measureMyCard();
  }

  /// 스티키 바는 내 랭킹 카드가 화면 밖으로 나갔을 때만 뜬다. 카드 높이는 폰트 배율에 따라
  /// 달라지므로 실제 레이아웃에서 잰다.
  void _measureMyCard() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      _myCardExtent = _myCardKey.currentContext?.size?.height ?? 0;
    });
  }

  Future<void> _loadMore() async {
    final period = _period!;
    final generation = _generation;
    setState(() {
      _loadingMore = true;
      _moreError = null;
    });
    try {
      final page = await ref
          .read(rankingRepositoryProvider)
          .getRankings(period: period, cursorRank: _cursor, size: _kPageSize);
      if (!mounted || generation != _generation) return;
      setState(() {
        _entries = [..._entries, ...page.content];
        _cursor = page.nextCursor;
        _hasNext = page.hasNext;
        _loadingMore = false;
      });
    } on ApiException {
      if (!mounted || generation != _generation) return;
      // 더보기 실패는 화면 전체를 에러로 바꾸지 않는다(사양서 §6.2.6).
      setState(() {
        _loadingMore = false;
        _moreError = '추가 랭킹을 불러오지 못했습니다.';
      });
    }
  }

  void _setPeriod(RankingPeriod period) {
    if (period == _period) return;
    context.go('${Routes.ranking}?period=${period.wire.toLowerCase()}');
  }

  Future<void> _openPortfolio(RankingItem item) async {
    if (!canViewRankerPortfolio(item.rank)) {
      // 403 이 뻔한 요청은 보내지 않는다(계획서 §4.1.4).
      showAppSnackbar(context, '포트폴리오는 $rankingPortfolioMaxRank위까지 공개됩니다.');
      return;
    }
    await showRankerPortfolioSheet(
      context,
      item: item,
      load: () => _loadPortfolio(item.userId),
    );
  }

  Future<RankerPortfolio> _loadPortfolio(int userId) async {
    final cached = _portfolioCache[userId];
    if (cached != null) return cached;
    final portfolio = await ref
        .read(rankingRepositoryProvider)
        .getRankerPortfolio(userId: userId, period: _period!);
    _portfolioCache[userId] = portfolio;
    return portfolio;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final period = _period ?? RankingPeriod.daily;

    return Scaffold(
      appBar: AppBar(title: const Text('랭킹'), actions: const [MypageButton()]),
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
                ExchangeSegment<RankingPeriod>(
                  items: [
                    for (final entry in _periodLabels.entries)
                      SegmentItem(entry.key, entry.value),
                  ],
                  value: period,
                  onChanged: _setPeriod,
                ),
                const SizedBox(height: TryptoSpacing.sm),
                Align(
                  alignment: Alignment.centerLeft,
                  child: Text(
                    '${_periodLabels[period]} 수익률 기준 순위',
                    style: theme.textTheme.labelMedium?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: Stack(
              children: [
                _body(period),
                Positioned(
                  left: 0,
                  right: 0,
                  bottom: 0,
                  child: ValueListenableBuilder<bool>(
                    valueListenable: _stickyVisible,
                    builder: (context, visible, child) {
                      final myRanking = _myRanking;
                      if (!visible || myRanking == null) {
                        return const SizedBox.shrink();
                      }
                      return _StickyMyRankBar(
                        myRanking: myRanking,
                        onTap: () => _scroll.animateTo(
                          0,
                          duration: TryptoMotion.sheet,
                          curve: TryptoMotion.enterCurve,
                        ),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _body(RankingPeriod period) {
    if (_loading) {
      return const Center(child: Text('랭킹 데이터를 불러오는 중입니다...'));
    }

    final error = _error;
    if (error != null) {
      return EmptyView(
        icon: LucideIcons.circleAlert,
        message: error,
        description: '일시적인 문제일 수 있습니다. 잠시 후 다시 시도해 주세요.',
        action: OutlinedButton(onPressed: _reload, child: const Text('다시 시도')),
      );
    }

    // 집계 전에는 서버가 오류가 아니라 빈 결과를 준다(사양서 §6.2.8). 통계 응답이 성공이어도
    // 자리표시가 우선한다.
    final aggregated = _entries.isNotEmpty;
    final top3 = _entries.take(3).toList();
    final rest = _entries.skip(3).toList();

    return RefreshIndicator(
      onRefresh: _reload,
      child: CustomScrollView(
        controller: _scroll,
        physics: const AlwaysScrollableScrollPhysics(),
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(
              TryptoSpacing.screen,
              TryptoSpacing.sm,
              TryptoSpacing.screen,
              TryptoSpacing.md,
            ),
            sliver: SliverToBoxAdapter(
              child: Column(
                children: [
                  _MyRankingCard(key: _myCardKey, myRanking: _myRanking),
                  const SizedBox(height: TryptoSpacing.md),
                  _StatsCard(
                    stats: aggregated ? _stats : null,
                    period: period,
                    // 목록이 비면 통계도 자리표시다. 목록이 있는데 통계만 없으면 조회 실패다.
                    failed: aggregated && _stats == null,
                  ),
                ],
              ),
            ),
          ),
          if (!aggregated) ...[
            const SliverPadding(
              padding: EdgeInsets.symmetric(horizontal: TryptoSpacing.screen),
              sliver: SliverToBoxAdapter(child: _PodiumPlaceholder()),
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: TryptoSpacing.xxl),
                child: EmptyView(
                  icon: LucideIcons.trophy,
                  message: '아직 집계된 랭킹이 없습니다',
                  description:
                      '랭킹은 매일 밤 자정에 집계됩니다.\n지금 거래를 시작하면 내일 첫 순위에 오릅니다.',
                  action: FilledButton(
                    onPressed: () => context.go(Routes.market),
                    child: const Text('거래하러 가기'),
                  ),
                ),
              ),
            ),
          ] else ...[
            SliverPadding(
              padding: const EdgeInsets.symmetric(
                horizontal: TryptoSpacing.screen,
              ),
              sliver: SliverToBoxAdapter(
                child: _Podium(
                  entries: top3,
                  myRank: _myRanking?.rank,
                  onTap: _openPortfolio,
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(
                TryptoSpacing.screen,
                TryptoSpacing.lg,
                TryptoSpacing.screen,
                0,
              ),
              sliver: SliverList.separated(
                itemCount: rest.length,
                separatorBuilder: (context, index) =>
                    const SizedBox(height: TryptoSpacing.sm),
                itemBuilder: (context, index) => _RankerTile(
                  item: rest[index],
                  isMe: rest[index].rank == _myRanking?.rank,
                  onTap: _openPortfolio,
                ),
              ),
            ),
            SliverToBoxAdapter(
              child: _ListFooter(
                loading: _loadingMore,
                error: _moreError,
                onRetry: _loadMore,
                // 스티키 바가 마지막 행을 가리지 않게 여백을 남긴다.
                bottomInset: _myRanking != null ? 72 : TryptoSpacing.xxl,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _MyRankingCard extends StatelessWidget {
  const _MyRankingCard({super.key, required this.myRanking});

  final MyRanking? myRanking;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final myRanking = this.myRanking;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(TryptoSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('내 랭킹', style: theme.textTheme.titleMedium),
            const SizedBox(height: TryptoSpacing.md),
            if (myRanking == null)
              // `data: null` 은 오류가 아니다 — 아직 집계에 포함되지 않은 사용자다(§6.2.5).
              Text(
                '아직 순위가 없습니다.\n거래를 시작하면 다음 집계에 반영됩니다.',
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              )
            else
              Row(
                children: [
                  RankBadge(rank: myRanking.rank),
                  const SizedBox(width: TryptoSpacing.md),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          myRanking.nickname,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.titleMedium,
                        ),
                        Text(
                          '${myRanking.tradeCount}회 거래',
                          style: theme.textTheme.labelSmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: TryptoSpacing.md,
                      vertical: TryptoSpacing.sm,
                    ),
                    decoration: BoxDecoration(
                      color: theme.colorScheme.surfaceContainer,
                      borderRadius: BorderRadius.circular(TryptoRadius.md),
                    ),
                    child: NumericText(
                      formatProfitPercent(myRanking.profitRate),
                      size: 20,
                      weight: FontWeight.w700,
                      color: context.profitColor(myRanking.profitRate),
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

class _StatsCard extends StatelessWidget {
  const _StatsCard({
    required this.stats,
    required this.period,
    required this.failed,
  });

  final RankingStats? stats;
  final RankingPeriod period;
  final bool failed;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.tryptoColors;
    final stats = this.stats;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(TryptoSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '${_periodLabels[period]} 통계',
              style: theme.textTheme.titleMedium,
            ),
            const SizedBox(height: TryptoSpacing.md),
            if (failed)
              Text(
                '통계를 불러오지 못했습니다.',
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: colors.negative,
                ),
              )
            else
              Row(
                children: [
                  Expanded(
                    child: _StatTile(
                      label: '참여자',
                      value: stats == null
                          ? '0명'
                          : '${formatGrouped(stats.totalParticipants)}명',
                      muted: stats == null,
                    ),
                  ),
                  Expanded(
                    child: _StatTile(
                      label: '최고 수익률',
                      value: stats == null
                          ? '--'
                          : formatProfitPercent(stats.maxProfitRate),
                      color: stats == null ? null : colors.positive,
                      muted: stats == null,
                    ),
                  ),
                  Expanded(
                    child: _StatTile(
                      label: '평균 수익률',
                      value: stats == null
                          ? '--'
                          : formatProfitPercent(stats.avgProfitRate),
                      color: stats == null
                          ? null
                          : context.profitColor(stats.avgProfitRate),
                      muted: stats == null,
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

class _StatTile extends StatelessWidget {
  const _StatTile({
    required this.label,
    required this.value,
    this.color,
    this.muted = false,
  });

  final String label;
  final String value;
  final Color? color;

  /// 집계 전 자리표시. 실데이터와 구분되게 흐리다.
  final bool muted;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final onSurface = theme.colorScheme.onSurface;

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
        NumericText(
          value,
          size: 14,
          weight: FontWeight.w700,
          // 알파는 색으로만 준다 — Opacity 위젯은 saveLayer 를 만든다(계획서 §4.2.5-8).
          color: muted
              ? onSurface.withValues(alpha: 0.4)
              : (color ?? onSurface),
        ),
      ],
    );
  }
}

/// 1위를 크게, 2·3위를 아래 2열로 둔다. 3열 그리드는 좁은 화면에서 글자가 뭉개진다(§6.6.1-8).
class _Podium extends StatelessWidget {
  const _Podium({
    required this.entries,
    required this.myRank,
    required this.onTap,
  });

  final List<RankingItem> entries;
  final int? myRank;
  final void Function(RankingItem item) onTap;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _PodiumCard(
          item: entries.first,
          isMe: entries.first.rank == myRank,
          large: true,
          onTap: onTap,
        ),
        if (entries.length > 1) ...[
          const SizedBox(height: TryptoSpacing.sm),
          IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(
                  child: _PodiumCard(
                    item: entries[1],
                    isMe: entries[1].rank == myRank,
                    large: false,
                    onTap: onTap,
                  ),
                ),
                const SizedBox(width: TryptoSpacing.sm),
                Expanded(
                  child: entries.length > 2
                      ? _PodiumCard(
                          item: entries[2],
                          isMe: entries[2].rank == myRank,
                          large: false,
                          onTap: onTap,
                        )
                      : const SizedBox.shrink(),
                ),
              ],
            ),
          ),
        ],
      ],
    );
  }
}

class _PodiumCard extends StatelessWidget {
  const _PodiumCard({
    required this.item,
    required this.isMe,
    required this.large,
    required this.onTap,
  });

  final RankingItem item;
  final bool isMe;
  final bool large;
  final void Function(RankingItem item) onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      shape: _cardShape(context, isMe),
      child: InkWell(
        onTap: () => onTap(item),
        child: Padding(
          padding: const EdgeInsets.all(TryptoSpacing.lg),
          child: Column(
            children: [
              RankBadge(rank: item.rank, size: large ? 44 : 32),
              const SizedBox(height: TryptoSpacing.sm),
              Text(
                item.nickname,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.titleMedium,
              ),
              const SizedBox(height: 2),
              NumericText(
                formatProfitPercent(item.profitRate),
                size: large ? 20 : 16,
                weight: FontWeight.w700,
                color: context.profitColor(item.profitRate),
              ),
              const SizedBox(height: 2),
              Text(
                '${item.tradeCount}회 거래',
                style: theme.textTheme.labelSmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _RankerTile extends StatelessWidget {
  const _RankerTile({
    required this.item,
    required this.isMe,
    required this.onTap,
  });

  final RankingItem item;
  final bool isMe;
  final void Function(RankingItem item) onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      shape: _cardShape(context, isMe),
      child: InkWell(
        onTap: () => onTap(item),
        child: Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: TryptoSpacing.lg,
            vertical: TryptoSpacing.md,
          ),
          child: Row(
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
                      style: theme.textTheme.labelLarge,
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
              // 색만으로 방향을 알리지 않는다 — 부호와 아이콘을 함께 쓴다(§6.6.1-9).
              Icon(
                item.profitRate > 0
                    ? LucideIcons.trendingUp
                    : item.profitRate < 0
                    ? LucideIcons.trendingDown
                    : LucideIcons.minus,
                size: 14,
                color: context.profitColor(item.profitRate),
              ),
              const SizedBox(width: TryptoSpacing.xs),
              NumericText(
                formatProfitPercent(item.profitRate),
                color: context.profitColor(item.profitRate),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _StickyMyRankBar extends StatelessWidget {
  const _StickyMyRankBar({required this.myRanking, required this.onTap});

  final MyRanking myRanking;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Material(
      color: theme.colorScheme.surface,
      child: InkWell(
        onTap: onTap,
        child: Container(
          decoration: const BoxDecoration(
            border: Border(top: BorderSide(color: TryptoPalette.border)),
          ),
          padding: const EdgeInsets.symmetric(
            horizontal: TryptoSpacing.screen,
            vertical: TryptoSpacing.md,
          ),
          child: Row(
            children: [
              RankBadge(rank: myRanking.rank, size: 28),
              const SizedBox(width: TryptoSpacing.md),
              Expanded(
                child: Text(
                  '내 순위 · ${myRanking.nickname}',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.labelLarge,
                ),
              ),
              NumericText(
                formatProfitPercent(myRanking.profitRate),
                color: context.profitColor(myRanking.profitRate),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 집계 전 1·2·3위 자리표시(파선 테두리 · `--%` · "집계 대기").
class _PodiumPlaceholder extends StatelessWidget {
  const _PodiumPlaceholder();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Row(
      children: [
        for (final rank in [1, 2, 3]) ...[
          if (rank > 1) const SizedBox(width: TryptoSpacing.sm),
          Expanded(
            child: _DashedBox(
              child: Column(
                children: [
                  Container(
                    width: 28,
                    height: 28,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: theme.colorScheme.surfaceContainer,
                      shape: BoxShape.circle,
                    ),
                    child: NumericText(
                      '$rank',
                      size: 12,
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: TryptoSpacing.sm),
                  NumericText(
                    '--%',
                    size: 14,
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                  const SizedBox(height: 2),
                  Text(
                    '집계 대기',
                    style: theme.textTheme.labelSmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }
}

/// 파선 테두리 상자. 자리표시가 실데이터 카드와 혼동되지 않게 한다.
class _DashedBox extends StatelessWidget {
  const _DashedBox({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: const _DashedBorderPainter(color: TryptoPalette.border),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: TryptoSpacing.sm,
          vertical: TryptoSpacing.lg,
        ),
        child: child,
      ),
    );
  }
}

class _DashedBorderPainter extends CustomPainter {
  const _DashedBorderPainter({required this.color});

  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1;
    final border = Path()
      ..addRRect(
        RRect.fromRectAndRadius(
          Offset.zero & size,
          const Radius.circular(TryptoRadius.xl),
        ),
      );

    for (final metric in border.computeMetrics()) {
      var distance = 0.0;
      while (distance < metric.length) {
        final next = (distance + 4).clamp(0.0, metric.length);
        canvas.drawPath(metric.extractPath(distance, next), paint);
        distance = next + 4;
      }
    }
  }

  @override
  bool shouldRepaint(_DashedBorderPainter oldDelegate) =>
      oldDelegate.color != color;
}

class _ListFooter extends StatelessWidget {
  const _ListFooter({
    required this.loading,
    required this.error,
    required this.onRetry,
    required this.bottomInset,
  });

  final bool loading;
  final String? error;
  final VoidCallback onRetry;
  final double bottomInset;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final error = this.error;

    return Padding(
      padding: EdgeInsets.fromLTRB(
        TryptoSpacing.screen,
        TryptoSpacing.lg,
        TryptoSpacing.screen,
        bottomInset,
      ),
      child: Center(
        child: loading
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : error == null
            ? const SizedBox.shrink()
            : Column(
                children: [
                  Text(
                    error,
                    style: theme.textTheme.labelMedium?.copyWith(
                      color: context.tryptoColors.negative,
                    ),
                  ),
                  const SizedBox(height: TryptoSpacing.sm),
                  OutlinedButton(
                    onPressed: onRetry,
                    child: const Text('다시 시도'),
                  ),
                ],
              ),
      ),
    );
  }
}

/// 내 행은 목록 안에서도 테두리로 강조한다(§6.6.1-3).
ShapeBorder _cardShape(BuildContext context, bool isMe) {
  return RoundedRectangleBorder(
    borderRadius: BorderRadius.circular(TryptoRadius.xl),
    side: BorderSide(
      color: isMe ? Theme.of(context).colorScheme.primary : TryptoPalette.border,
      width: isMe ? 1.5 : 1,
    ),
  );
}
