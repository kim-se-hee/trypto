import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/constants/exchanges.dart';
import '../../core/theme/theme.dart';
import '../../models/enums.dart';
import 'candle_chart.dart';
import 'coin_detail_page.dart';
import 'live_candles.dart';

/// 가로 풀스크린. 같은 [CandleRequest] 를 쓰면 코인 상세와 **같은 `LiveCandleFolder`** 를
/// 공유하므로 진행 중인 봉이 끊기지 않는다.
class ChartFullscreenPage extends ConsumerStatefulWidget {
  const ChartFullscreenPage({
    super.key,
    required this.symbol,
    required this.interval,
  });

  final String symbol;
  final CandleInterval interval;

  @override
  ConsumerState<ChartFullscreenPage> createState() =>
      _ChartFullscreenPageState();
}

class _ChartFullscreenPageState extends ConsumerState<ChartFullscreenPage> {
  late CandleInterval _interval = widget.interval;

  @override
  void initState() {
    super.initState();
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);
  }

  @override
  void dispose() {
    SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final exchange = ExchangeIds.byKey(
      GoRouterState.of(context).uri.queryParameters['exchange'],
    );
    final request = CandleRequest(
      exchangeCode: exchange.candleCode,
      symbol: widget.symbol,
      interval: _interval,
    );

    return Scaffold(
      appBar: AppBar(
        title: Text('${widget.symbol} · ${exchange.name}'),
        titleTextStyle: Theme.of(context).textTheme.titleMedium,
      ),
      body: SafeArea(
        child: Column(
          children: [
            IntervalChips(
              interval: _interval,
              onChanged: (interval) => setState(() => _interval = interval),
            ),
            const SizedBox(height: TryptoSpacing.xs),
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
        ),
      ),
    );
  }
}
