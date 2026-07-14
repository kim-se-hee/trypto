import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/constants/exchanges.dart';
import '../../core/format/formatters.dart';
import '../../core/realtime/ticker_store.dart';
import '../../core/router/guard.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/async_view.dart';
import '../../core/widgets/empty_view.dart';
import '../../core/widgets/numeric_text.dart';
import '../../core/widgets/profit_badge.dart';
import '../../models/enums.dart';
import 'candle_chart.dart';
import 'live_candles.dart';
import 'market_controller.dart';

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

  @override
  Widget build(BuildContext context) {
    final exchange = ExchangeIds.byKey(
      GoRouterState.of(context).uri.queryParameters['exchange'],
    );
    final coins = ref.watch(marketCoinsProvider(exchange.id));

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

          final request = CandleRequest(
            exchangeCode: exchange.candleCode,
            symbol: entry.symbol,
            interval: _interval,
          );

          return Column(
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
              IntervalChips(
                interval: _interval,
                onChanged: (interval) => setState(() => _interval = interval),
              ),
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
        },
      ),
    );
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
              onSelected: (_) => onChanged(entry.key),
            ),
            const SizedBox(width: TryptoSpacing.xs),
          ],
        ],
      ),
    );
  }
}
