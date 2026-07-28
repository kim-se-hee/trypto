import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/format/formatters.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/empty_view.dart';
import '../../core/widgets/mypage_button.dart';
import '../../core/widgets/no_round_notice.dart';
import '../../core/widgets/numeric_text.dart';
import '../../models/enums.dart';
import '../../models/regret.dart';
import '../round/round_controller.dart';
import 'regret_chart.dart';
import 'regret_repository.dart';
import 'regret_simulation.dart';
import 'rule_chips.dart';
import 'violation_list.dart';

const String _kDisclaimer = '* 모의투자 데이터입니다. 규칙 준수 시 수익률은 시뮬레이션 결과입니다.';

const List<String> _kEstimateNotices = [
  '규칙 준수 시 자산은 추정값으로 참고용입니다.',
  '바이낸스 지갑의 금액은 1 USDT = 1,400원 고정 환율로 환산됩니다.',
];

final DateFormat _analysisDate = DateFormat('M/d', 'en_US');

/// 복기는 라운드 단위다. 거래소를 골라 보지 않고 라운드에 속한 모든 거래소를 원화로 합쳐 본다 —
/// 거래소 간 송금은 라운드 안에서의 자금 이동이라 합계에 영향을 주지 않기 때문이다.
///
/// 거래소는 위반 거래 목록에서만 구분한다. 거기서는 발생 거래소의 기축통화로 금액을 보여준다.
class RegretPage extends ConsumerStatefulWidget {
  const RegretPage({super.key});

  @override
  ConsumerState<RegretPage> createState() => _RegretPageState();
}

class _RegretPageState extends ConsumerState<RegretPage> {
  int? _roundId;

  /// null 이면 아직 리포트가 도착하지 않았다는 뜻이다. 사용자가 규칙을 전부 끈 **빈 집합**과
  /// 구분해야 한다 — 빈 집합은 시뮬레이션 라인을 숨기라는 의사 표시다.
  Set<RuleType>? _enabledRules;
  bool _btcEnabled = true;
  ViolationFilter _filter = ViolationFilter.all;

  /// 위반 거래 목록의 거래소 축. null 이면 전 거래소다.
  int? _exchangeFilter;

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

    final roundId = round.roundId;
    if (roundId != _roundId) {
      // 라운드가 바뀌면 이전 리포트의 토글·필터 상태를 버린다.
      _roundId = roundId;
      _enabledRules = null;
      _btcEnabled = true;
      _filter = ViolationFilter.all;
      _exchangeFilter = null;
    }

    return Scaffold(
      appBar: const _RegretAppBar(),
      body: Column(
        children: [
          Expanded(
            child: AsyncView<RegretBundle>(
              value: ref.watch(regretProvider(roundId)),
              onRetry: () => ref.invalidate(regretProvider(roundId)),
              builder: (bundle) => RefreshIndicator(
                onRefresh: () => ref.refresh(regretProvider(roundId).future),
                child: _Content(
                  bundle: bundle,
                  // 초기값은 리포트에 담긴 규칙 전체 활성이다(사양서 §6.3.4).
                  enabledRules: _enabledRules ??= _initialRules(bundle.report),
                  btcEnabled: _btcEnabled,
                  filter: _filter,
                  exchangeFilter: _exchangeFilter,
                  onToggleRule: _toggleRule,
                  onToggleBtc: () => setState(() => _btcEnabled = !_btcEnabled),
                  onFilter: (filter) => setState(() => _filter = filter),
                  onExchangeFilter: (id) =>
                      setState(() => _exchangeFilter = id),
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
    required this.exchangeFilter,
    required this.onToggleRule,
    required this.onToggleBtc,
    required this.onFilter,
    required this.onExchangeFilter,
  });

  final RegretBundle bundle;
  final Set<RuleType> enabledRules;
  final bool btcEnabled;
  final ViolationFilter filter;
  final int? exchangeFilter;
  final void Function(RuleType rule) onToggleRule;
  final VoidCallback onToggleBtc;
  final ValueChanged<ViolationFilter> onFilter;
  final ValueChanged<int?> onExchangeFilter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final report = bundle.report;
    final chart = bundle.chart;
    final violations = filterViolations(
      report.violationDetails,
      filter,
      exchangeId: exchangeFilter,
    );
    final exchanges = violatedExchanges(report.violationDetails);
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
                        const SizedBox(width: TryptoSpacing.xs),
                        const _EstimateNoticeButton(),
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
                      violations: report.violationDetails,
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
                    exchangeId: exchangeFilter,
                    onChanged: onFilter,
                  ),
                  // 거래소가 하나뿐이면 고를 것이 없어 축을 세우지 않는다.
                  if (exchanges.length > 1) ...[
                    const SizedBox(height: TryptoSpacing.sm),
                    ExchangeFilterBar(
                      exchanges: exchanges,
                      selected: exchangeFilter,
                      onChanged: onExchangeFilter,
                    ),
                  ],
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
                itemBuilder: (context, index) =>
                    ViolationTile(violation: violations[index]),
              ),
            ),
        ],
        const SliverToBoxAdapter(child: SizedBox(height: TryptoSpacing.lg)),
      ],
    );
  }
}

/// 복기 그래프가 추정값이라는 안내. 문구는 웹과 같아야 한다.
class _EstimateNoticeButton extends StatelessWidget {
  const _EstimateNoticeButton();

  @override
  Widget build(BuildContext context) {
    return InkResponse(
      onTap: () => _showEstimateNotice(context),
      radius: 16,
      child: Icon(
        LucideIcons.circleHelp,
        size: 14,
        color: Theme.of(context).colorScheme.onSurfaceVariant,
      ),
    );
  }
}

Future<void> _showEstimateNotice(BuildContext context) => showDialog<void>(
  context: context,
  builder: (context) {
    final theme = Theme.of(context);
    return AlertDialog(
      title: const Text('유의 사항'),
      content: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          for (final notice in _kEstimateNotices)
            Padding(
              padding: const EdgeInsets.only(bottom: TryptoSpacing.sm),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('· ', style: theme.textTheme.bodySmall),
                  Expanded(
                    child: Text(notice, style: theme.textTheme.bodySmall),
                  ),
                ],
              ),
            ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('확인'),
        ),
      ],
    );
  },
);

/// "위반 손실" 히어로 + 3-stat 타일. 금액은 라운드 전체를 원화로 합친 값이다.
class _Hero extends StatelessWidget {
  const _Hero({required this.report});

  final RegretReport report;

  /// 위반 손실은 양수가 손해다. 부호에 따라 같은 숫자라도 정반대의 이야기가 된다.
  String _describe() {
    if (report.isEmpty) return '복기 리포트는 매일 밤 집계됩니다. 내일 다시 확인해 주세요.';
    if (report.totalViolations == 0) return '원칙을 어긴 거래가 없습니다.';
    final amount = formatCurrency(
      report.totalViolationLoss.abs(),
      roundCurrency,
    );
    if (report.totalViolationLoss > 0) return '원칙만 지켰다면 $amount 더 벌었습니다.';
    return '원칙을 어긴 게 오히려 $amount 이득이었습니다.';
  }

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
              '위반 손실',
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: TryptoSpacing.xs),
            NumericText(
              formatCurrency(report.totalViolationLoss, roundCurrency),
              size: 30,
              weight: FontWeight.w700,
              color: report.totalViolationLoss > 0
                  ? colors.negative
                  : theme.colorScheme.onSurface,
            ),
            const SizedBox(height: TryptoSpacing.xs),
            Text(
              _describe(),
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: TryptoSpacing.lg),
            Row(
              children: [
                Expanded(
                  child: _StatTile(
                    label: '실제 자산',
                    value: formatCurrencyCompact(
                      report.actualAsset,
                      roundCurrency,
                    ),
                  ),
                ),
                Expanded(
                  child: _StatTile(
                    label: '규칙 준수 시',
                    value: formatCurrencyCompact(
                      report.ruleFollowedAsset,
                      roundCurrency,
                    ),
                    color: report.ruleFollowedAsset > report.actualAsset
                        ? colors.positive
                        : null,
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
