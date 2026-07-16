import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
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
import '../../models/enums.dart';
import '../../models/regret.dart';
import '../../models/round.dart';
import '../round/round_controller.dart';
import 'regret_chart.dart';
import 'regret_repository.dart';
import 'regret_simulation.dart';
import 'rule_chips.dart';
import 'violation_list.dart';

const String _kDisclaimer = '* 모의투자 데이터입니다. 규칙 준수 시 수익률은 시뮬레이션 결과입니다.';

final DateFormat _analysisDate = DateFormat('M/d', 'en_US');

/// 웹은 `wallets[0]` 에 고정되어 다른 거래소의 복기를 볼 수 없다(사양서 §6.3.2). 거래소 선택기를
/// 추가하고 응답의 `exchangeName`·`currency` 를 표기해 통화(KRW/USDT) 혼동을 막는다.
///
/// 선택 거래소는 라우트 쿼리(`/regret?exchange=upbit`)에 둔다 — 다른 탭과 같은 규칙이다.
class RegretPage extends ConsumerStatefulWidget {
  const RegretPage({super.key});

  @override
  ConsumerState<RegretPage> createState() => _RegretPageState();
}

class _RegretPageState extends ConsumerState<RegretPage> {
  RegretRequest? _request;

  /// null 이면 아직 리포트가 도착하지 않았다는 뜻이다. 사용자가 규칙을 전부 끈 **빈 집합**과
  /// 구분해야 한다 — 빈 집합은 시뮬레이션 라인을 숨기라는 의사 표시다.
  Set<RuleType>? _enabledRules;
  bool _btcEnabled = true;
  ViolationFilter _filter = ViolationFilter.all;

  @override
  Widget build(BuildContext context) {
    final round = ref.watch(
      roundControllerProvider.select((state) => state.activeRound),
    );

    if (round == null) {
      return const Scaffold(
        appBar: _RegretAppBar(),
        body: Column(
          children: [
            Expanded(
              child: NoRoundNotice(message: '진행 중인 라운드가 없어 복기할 내역이 없습니다.'),
            ),
            _Disclaimer(),
          ],
        ),
      );
    }

    final exchange = _selectedExchange(round);
    final request = (roundId: round.roundId, exchangeId: exchange.id);
    if (request != _request) {
      // 거래소·라운드가 바뀌면 이전 리포트의 토글 상태를 버린다.
      _request = request;
      _enabledRules = null;
      _btcEnabled = true;
      _filter = ViolationFilter.all;
    }

    return Scaffold(
      appBar: const _RegretAppBar(),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(
              TryptoSpacing.screen,
              TryptoSpacing.md,
              TryptoSpacing.screen,
              TryptoSpacing.sm,
            ),
            child: ExchangeSegment<int>(
              items: [
                for (final wallet in round.wallets)
                  SegmentItem(
                    wallet.exchangeId,
                    ExchangeIds.byId(wallet.exchangeId)?.name ??
                        '거래소 ${wallet.exchangeId}',
                  ),
              ],
              value: exchange.id,
              onChanged: (id) => context.go(
                '${Routes.regret}?exchange='
                '${(ExchangeIds.byId(id) ?? exchange).key}',
              ),
            ),
          ),
          Expanded(
            child: AsyncView<RegretBundle>(
              value: ref.watch(regretProvider(request)),
              onRetry: () => ref.invalidate(regretProvider(request)),
              builder: (bundle) => RefreshIndicator(
                onRefresh: () => ref.refresh(regretProvider(request).future),
                child: _Content(
                  bundle: bundle,
                  // 초기값은 리포트에 담긴 규칙 전체 활성이다(사양서 §6.3.4).
                  enabledRules: _enabledRules ??= _initialRules(bundle.report),
                  btcEnabled: _btcEnabled,
                  filter: _filter,
                  onToggleRule: _toggleRule,
                  onToggleBtc: () => setState(() => _btcEnabled = !_btcEnabled),
                  onFilter: (filter) => setState(() => _filter = filter),
                ),
              ),
            ),
          ),
          const _Disclaimer(),
        ],
      ),
    );
  }

  Set<RuleType> _initialRules(RegretReport report) => {
    for (final impact in report.ruleImpacts)
      if (impact.ruleType != null) impact.ruleType!,
  };

  void _toggleRule(RuleType rule) {
    setState(() {
      final rules = {..._enabledRules ?? <RuleType>{}};
      if (!rules.remove(rule)) rules.add(rule);
      _enabledRules = rules;
    });
  }

  /// 쿼리가 가리키는 거래소에 이 라운드의 지갑이 없으면 첫 지갑으로 떨어진다.
  Exchange _selectedExchange(ActiveRound round) {
    final requested = ExchangeIds.byKey(
      GoRouterState.of(context).uri.queryParameters['exchange'],
    );
    if (round.walletIdOf(requested.id) != null) return requested;
    for (final wallet in round.wallets) {
      final exchange = ExchangeIds.byId(wallet.exchangeId);
      if (exchange != null) return exchange;
    }
    return requested;
  }
}

class _RegretAppBar extends StatelessWidget implements PreferredSizeWidget {
  const _RegretAppBar();

  @override
  Size get preferredSize => const Size.fromHeight(56);

  @override
  Widget build(BuildContext context) => AppBar(
    title: const Text('투자 복기'),
    actions: const [MypageButton()],
  );
}

class _Content extends StatelessWidget {
  const _Content({
    required this.bundle,
    required this.enabledRules,
    required this.btcEnabled,
    required this.filter,
    required this.onToggleRule,
    required this.onToggleBtc,
    required this.onFilter,
  });

  final RegretBundle bundle;
  final Set<RuleType> enabledRules;
  final bool btcEnabled;
  final ViolationFilter filter;
  final void Function(RuleType rule) onToggleRule;
  final VoidCallback onToggleBtc;
  final ValueChanged<ViolationFilter> onFilter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final report = bundle.report;
    final chart = bundle.chart;
    final violations = filterViolations(report.violationDetails, filter);
    final start = report.analysisStart;
    final end = report.analysisEnd;

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
          sliver: SliverToBoxAdapter(child: _Hero(report: report)),
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
                    Row(
                      children: [
                        Text('자산 추이', style: theme.textTheme.titleMedium),
                        const Spacer(),
                        if (start != null && end != null)
                          Text(
                            '분석 구간 ${_analysisDate.format(start)}'
                            ' ~ ${_analysisDate.format(end)}',
                            style: theme.textTheme.labelSmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: TryptoSpacing.md),
                    RegretAssetChart(
                      chart: chart,
                      enabledRules: enabledRules,
                      btcEnabled: btcEnabled,
                    ),
                    // 배치 전에는 규칙 임팩트도 비어 있다. 토글 행을 그리지 않는다.
                    if (!report.isEmpty) ...[
                      const SizedBox(height: TryptoSpacing.lg),
                      Text('만약 규칙을 지켰다면', style: theme.textTheme.titleMedium),
                      const SizedBox(height: TryptoSpacing.sm),
                      RuleChips(
                        impacts: report.ruleImpacts,
                        enabled: enabledRules,
                        btcEnabled: btcEnabled,
                        btcProfitRate: btcHoldProfitRate(chart.assetHistory),
                        onToggleRule: onToggleRule,
                        onToggleBtc: onToggleBtc,
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ),
        if (!report.isEmpty) ...[
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(
              TryptoSpacing.screen,
              TryptoSpacing.lg,
              TryptoSpacing.screen,
              TryptoSpacing.sm,
            ),
            sliver: SliverToBoxAdapter(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('규칙 위반 거래', style: theme.textTheme.titleMedium),
                  const SizedBox(height: TryptoSpacing.sm),
                  ViolationFilterBar(
                    violations: report.violationDetails,
                    filter: filter,
                    onChanged: onFilter,
                  ),
                ],
              ),
            ),
          ),
          if (violations.isEmpty)
            const SliverToBoxAdapter(
              child: Padding(
                padding: EdgeInsets.symmetric(vertical: TryptoSpacing.xl),
                child: EmptyView(
                  icon: LucideIcons.circleCheck,
                  message: '해당 조건의 위반 거래가 없습니다.',
                ),
              ),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.symmetric(
                horizontal: TryptoSpacing.screen,
              ),
              sliver: SliverList.separated(
                itemCount: violations.length,
                separatorBuilder: (context, index) =>
                    const SizedBox(height: TryptoSpacing.sm),
                itemBuilder: (context, index) => ViolationTile(
                  violation: violations[index],
                  currency: report.currency,
                ),
              ),
            ),
        ],
        const SliverToBoxAdapter(child: SizedBox(height: TryptoSpacing.lg)),
      ],
    );
  }
}

/// "놓친 수익" 히어로 + 3-stat 타일. 비율은 `toStringAsFixed(2)` 로 다듬는다 — 서버 `BigDecimal`
/// 의 scale 이 그대로 노출되면 `12.340000%` 가 된다(사양서 §6.4.4).
class _Hero extends StatelessWidget {
  const _Hero({required this.report});

  final RegretReport report;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.tryptoColors;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(TryptoSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '${report.exchangeName} · ${report.currency}',
              style: theme.textTheme.labelSmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: TryptoSpacing.sm),
            Text(
              '놓친 수익',
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: TryptoSpacing.xs),
            NumericText(
              formatCurrency(report.missedProfit, report.currency),
              size: 30,
              weight: FontWeight.w700,
              color: report.missedProfit > 0
                  ? colors.negative
                  : theme.colorScheme.onSurface,
            ),
            const SizedBox(height: TryptoSpacing.xs),
            Text(
              report.isEmpty
                  ? '복기 리포트는 매일 밤 집계됩니다. 내일 다시 확인해 주세요.'
                  : report.missedProfit > 0
                  ? '규칙을 지켰다면 이만큼 더 벌었습니다.'
                  : '규칙을 잘 지켰습니다. 놓친 수익이 없습니다.',
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: TryptoSpacing.lg),
            Row(
              children: [
                Expanded(
                  child: _StatTile(
                    label: '실제',
                    value: formatProfitPercent(report.actualProfitRate),
                    color: context.profitColor(report.actualProfitRate),
                  ),
                ),
                Expanded(
                  child: _StatTile(
                    label: '규칙 준수 시',
                    value: formatProfitPercent(report.ruleFollowedProfitRate),
                    color: context.profitColor(report.ruleFollowedProfitRate),
                  ),
                ),
                Expanded(
                  child: _StatTile(
                    label: '위반',
                    value: '${report.totalViolations}건',
                    color: report.totalViolations > 0 ? colors.negative : null,
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
  const _StatTile({required this.label, required this.value, this.color});

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
        NumericText(value, size: 16, weight: FontWeight.w700, color: color),
      ],
    );
  }
}

/// 시뮬레이션이 가중치 보간 근사임을 알리는 유일한 장치다. 상태와 무관하게 항상 붙는다.
class _Disclaimer extends StatelessWidget {
  const _Disclaimer();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(
          TryptoSpacing.screen,
          0,
          TryptoSpacing.screen,
          TryptoSpacing.sm,
        ),
        child: Text(
          _kDisclaimer,
          style: theme.textTheme.labelSmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
      ),
    );
  }
}
