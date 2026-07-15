import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import 'core/theme/theme.dart';
import 'core/theme/trypto_colors.dart';
import 'core/widgets/app_snackbar.dart';
import 'core/widgets/empty_view.dart';
import 'core/widgets/exchange_segment.dart';
import 'core/widgets/numeric_text.dart';
import 'core/widgets/profit_badge.dart';

/// 디자인 시스템 카탈로그. 라우터가 붙는 6단위까지 테마를 눈으로 검증하는 임시 화면이다.
class CatalogPage extends StatefulWidget {
  const CatalogPage({super.key});

  @override
  State<CatalogPage> createState() => _CatalogPageState();
}

class _CatalogPageState extends State<CatalogPage> {
  int _exchangeId = 1;
  double _ratio = 0.5;
  bool _switched = true;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.tryptoColors;

    return Scaffold(
      appBar: AppBar(
        title: const Text('디자인 시스템'),
        actions: [
          IconButton(
            onPressed: () => showAppSnackbar(context, '마이페이지로 이동합니다.'),
            icon: const Icon(LucideIcons.circleUser),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.symmetric(
          horizontal: TryptoSpacing.screen,
          vertical: TryptoSpacing.xxl,
        ),
        children: [
          Text('마켓', style: theme.textTheme.headlineMedium),
          const SizedBox(height: TryptoSpacing.xs),
          Text(
            '업비트 · 빗썸 · 바이낸스 시세로 모의 투자합니다.',
            style: theme.textTheme.bodyMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),

          _section('색 토큰'),
          Wrap(
            spacing: TryptoSpacing.sm,
            runSpacing: TryptoSpacing.sm,
            children: [
              _swatch('primary', TryptoPalette.primary),
              _swatch('positive', colors.positive),
              _swatch('negative', colors.negative),
              _swatch('warning', colors.warning),
              _swatch('chart2', colors.chart2),
              _swatch('muted-fg', TryptoPalette.mutedForeground),
              _swatch('secondary', TryptoPalette.secondary),
              _swatch('border', TryptoPalette.border),
            ],
          ),

          _section('타이포그래피'),
          Text('displaySmall 36', style: theme.textTheme.displaySmall),
          Text('headlineSmall 24', style: theme.textTheme.headlineSmall),
          Text('titleLarge 18', style: theme.textTheme.titleLarge),
          Text('bodyMedium 14 — 기본 본문', style: theme.textTheme.bodyMedium),
          Text('labelMedium 12 — 보조 라벨', style: theme.textTheme.labelMedium),
          Text('symbol 13 — BTC', style: TryptoText.symbol),
          const NumericText('163,254,000', size: 18, weight: FontWeight.w700),

          _section('세그먼트'),
          ExchangeSegment<int>(
            items: const [
              SegmentItem(1, '업비트'),
              SegmentItem(2, '빗썸'),
              SegmentItem(3, '바이낸스'),
            ],
            value: _exchangeId,
            onChanged: (v) => setState(() => _exchangeId = v),
          ),

          _section('버튼'),
          Wrap(
            spacing: TryptoSpacing.sm,
            runSpacing: TryptoSpacing.sm,
            children: [
              FilledButton(onPressed: () {}, child: const Text('매수하기')),
              FilledButton(
                style: TryptoButtons.secondary,
                onPressed: () {},
                child: const Text('보조'),
              ),
              FilledButton(
                style: TryptoButtons.destructive,
                onPressed: () {},
                child: const Text('라운드 종료'),
              ),
              OutlinedButton(onPressed: () {}, child: const Text('외곽선')),
              TextButton(onPressed: () {}, child: const Text('고스트')),
              TextButton(
                style: TryptoButtons.link,
                onPressed: () {},
                child: const Text('링크'),
              ),
              const FilledButton(onPressed: null, child: Text('비활성')),
              FilledButton.icon(
                onPressed: () =>
                    showAppSnackbar(context, '주문에 실패했습니다.', isError: true),
                icon: const Icon(LucideIcons.circleAlert),
                label: const Text('오류 스낵바'),
              ),
            ],
          ),

          _section('배지'),
          Wrap(
            spacing: TryptoSpacing.sm,
            runSpacing: TryptoSpacing.sm,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: const [
              ProfitBadge(value: 2.41, text: '+2.41%'),
              ProfitBadge(value: -1.08, text: '-1.08%'),
              ProfitBadge(value: 0, text: '0.00%'),
              WarningBadge(label: '대기', icon: LucideIcons.clock),
            ],
          ),

          _section('카드'),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(TryptoSpacing.lg),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          '비트코인',
                          style: theme.textTheme.bodyMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        Text(
                          'BTC/KRW',
                          style: theme.textTheme.labelSmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      NumericText(
                        '163,254,000',
                        weight: FontWeight.w700,
                        color: context.profitColor(2.41),
                      ),
                      const SizedBox(height: 2),
                      const ProfitBadge(value: 2.41, text: '+2.41%'),
                    ],
                  ),
                ],
              ),
            ),
          ),

          _section('입력'),
          const TextField(
            decoration: InputDecoration(
              hintText: '코인명 또는 심볼 검색',
              prefixIcon: Icon(LucideIcons.search),
            ),
          ),

          _section('슬라이더 · 스위치 · 탭'),
          Slider(
            value: _ratio,
            onChanged: (v) => setState(() => _ratio = v),
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: Text('손절 규칙 적용', style: theme.textTheme.bodyMedium),
            value: _switched,
            onChanged: (v) => setState(() => _switched = v),
          ),
          const SizedBox(
            height: 48,
            child: DefaultTabController(
              length: 2,
              child: TabBar(
                tabs: [Tab(text: '주문'), Tab(text: '거래내역')],
              ),
            ),
          ),

          _section('빈 상태'),
          SizedBox(
            height: 220,
            child: Card(
              child: EmptyView(
                message: '보유 중인 코인이 없습니다.',
                description: '마켓에서 첫 주문을 넣어 보세요.',
                action: FilledButton(
                  onPressed: () {},
                  child: const Text('마켓으로'),
                ),
              ),
            ),
          ),
          const SizedBox(height: TryptoSpacing.xxl),
        ],
      ),
    );
  }

  Widget _section(String title) {
    return Padding(
      padding: const EdgeInsets.only(
        top: TryptoSpacing.xxl,
        bottom: TryptoSpacing.md,
      ),
      child: Text(title, style: Theme.of(context).textTheme.titleLarge),
    );
  }

  Widget _swatch(String name, Color color) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 76,
          height: 40,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(TryptoRadius.md),
            border: Border.all(color: TryptoPalette.border),
          ),
        ),
        const SizedBox(height: TryptoSpacing.xs),
        Text(name, style: TryptoText.micro),
      ],
    );
  }
}
