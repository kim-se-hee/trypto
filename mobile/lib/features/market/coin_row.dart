import 'package:flutter/material.dart';

import '../../core/format/formatters.dart';
import '../../core/realtime/ticker_store.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/numeric_text.dart';
import '../../core/widgets/profit_badge.dart';

/// `ListView.builder(itemExtent:)` 의 고정 행 높이. 자식을 측정하지 않으므로 스크롤 중 위치
/// 계산이 O(1) 이다(계획서 §4.2.5).
const double kCoinRowHeight = 68;

/// 우측 숫자 묶음의 고정 폭. 가격 문자열의 폭이 바뀌어도 이 경계에서 리레이아웃이 멈춘다 —
/// tight 제약을 받은 `RenderBox` 가 relayout boundary 가 된다(계획서 §4.2.5-4).
const double _kNumericWidth = 160;

/// 행 3분할(계획서 §4.2.5-3).
///
/// ① 이름·심볼은 [ValueListenableBuilder] **바깥**이다 — 목록 수명 동안 한 번만 빌드된다.
/// ② 숫자 3개는 [RowNotifier] 를 구독한다.
/// ③ 플래시 테두리는 [FlashNotifier] 를 구독한다. 테두리 깜빡임이 숫자의 레이아웃을 다시
///    돌리지 않는다.
///
/// 행에 `key` 를 주지 않는다(호출부). sliver 자식은 index 슬롯으로 매칭되므로, 키가 있으면
/// 재정렬 시 `canUpdate == false` 가 되어 Element·State·RenderObject 가 전부 재생성된다.
class CoinRow extends StatelessWidget {
  const CoinRow({
    super.key,
    required this.symbol,
    required this.name,
    required this.row,
    required this.flash,
    required this.baseCurrency,
    this.onTap,
  });

  final String symbol;
  final String name;
  final RowNotifier row;
  final FlashNotifier flash;
  final String baseCurrency;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return InkWell(
      onTap: onTap,
      child: Container(
        height: kCoinRowHeight,
        padding: const EdgeInsets.symmetric(horizontal: TryptoSpacing.screen),
        decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: TryptoPalette.border)),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    symbol,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TryptoText.symbol,
                  ),
                  const SizedBox(height: 2),
                  Text(
                    name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.labelSmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: TryptoSpacing.sm),
            SizedBox(
              width: _kNumericWidth,
              child: RepaintBoundary(
                child: ValueListenableBuilder<CoinRowState>(
                  valueListenable: row,
                  builder: (context, state, child) => CoinNumbers(
                    price: state.price,
                    changeRate: state.changeRate,
                    volume: state.volume,
                    baseCurrency: baseCurrency,
                    flash: flash,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// 시세가 바뀐 **현재가 숫자에만** 잠깐 얇은 테두리를 두른다(업비트 방식). 테두리 두께·여백은
/// 플래시 유무와 무관하게 항상 차지하므로(색만 바뀐다) 깜빡여도 레이아웃이 흔들리지 않는다.
/// 페이드하지 않으므로 `AnimatedContainer` 를 쓰지 않는다(사양서 §3.3.3).
class _PriceFlash extends StatelessWidget {
  const _PriceFlash({required this.flash, required this.child});

  final FlashNotifier flash;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final colors = context.tryptoColors;

    return ValueListenableBuilder<FlashDir?>(
      valueListenable: flash,
      builder: (context, dir, child) {
        final color = switch (dir) {
          null => Colors.transparent,
          FlashDir.up => colors.positive,
          FlashDir.down => colors.negative,
          FlashDir.same => colors.flashNeutral,
        };
        return Container(
          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
          decoration: BoxDecoration(
            border: Border.all(color: color),
            borderRadius: BorderRadius.circular(TryptoRadius.sm),
          ),
          child: child,
        );
      },
      child: child,
    );
  }
}

/// 숫자 3개만 그린다. 티커가 바꾸는 것은 이 묶음뿐이다.
class CoinNumbers extends StatelessWidget {
  const CoinNumbers({
    super.key,
    required this.price,
    required this.changeRate,
    required this.volume,
    required this.baseCurrency,
    required this.flash,
  });

  final double price;
  final double changeRate;
  final double volume;
  final String baseCurrency;
  final FlashNotifier flash;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    // 시세를 아직 받지 못한 코인이 있다(사양서 §4.2.1).
    final unpriced = price <= 0;

    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        _PriceFlash(
          flash: flash,
          child: NumericText(
            unpriced
                ? '-'
                : '${getCurrencySymbol(baseCurrency)}'
                      '${formatPrice(price, baseCurrency)}',
            color: unpriced
                ? theme.colorScheme.onSurfaceVariant
                : context.profitColor(changeRate),
          ),
        ),
        const SizedBox(height: 4),
        Row(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            // 배지는 내용만큼 넓어진다. 세 자릿수 등락률(+300.00%)이 들어와도 열 밖으로
            // 밀지 않도록 남은 폭 안에 가둔다.
            Flexible(
              child: unpriced
                  ? NumericText(
                      '-',
                      size: 12,
                      weight: FontWeight.w500,
                      color: theme.colorScheme.onSurfaceVariant,
                    )
                  : ProfitBadge(
                      value: changeRate,
                      text: formatChangeRate(changeRate),
                    ),
            ),
            const SizedBox(width: TryptoSpacing.sm),
            SizedBox(
              width: 80,
              child: Align(
                alignment: Alignment.centerRight,
                child: NumericText(
                  // 거래대금은 모바일 폭에 맞춰 억·만으로 축약한다(KRW). USDT 는 그대로다.
                  volume <= 0
                      ? '-'
                      : formatCurrencyCompact(volume, baseCurrency),
                  size: 12,
                  weight: FontWeight.w500,
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }
}
