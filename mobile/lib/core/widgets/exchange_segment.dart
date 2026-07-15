import 'package:flutter/material.dart';

import '../theme/theme.dart';

class SegmentItem<T> {
  const SegmentItem(this.value, this.label);

  final T value;
  final String label;
}

/// 웹 Tabs(`default` 변형, §8.6.4) 를 세그먼트로 옮긴 것.
/// 트랙 `#F0EEEB`/반경 10/내부 패딩 3/높이 36, 활성 탭은 `#F8F7F4` 배경 + `shadow-sm`.
///
/// 거래소 전환(마켓·복기)과 기간 전환(랭킹)이 같은 모양을 쓴다.
class ExchangeSegment<T> extends StatelessWidget {
  const ExchangeSegment({
    super.key,
    required this.items,
    required this.value,
    required this.onChanged,
  });

  final List<SegmentItem<T>> items;
  final T value;
  final ValueChanged<T> onChanged;

  @override
  Widget build(BuildContext context) {
    final labelStyle = Theme.of(context).textTheme.labelLarge;

    return Container(
      height: 36,
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(
        color: TryptoPalette.secondary,
        borderRadius: BorderRadius.circular(TryptoRadius.lg),
      ),
      child: Row(
        children: [
          for (final item in items)
            Expanded(
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () => onChanged(item.value),
                child: AnimatedContainer(
                  duration: TryptoMotion.colorTransition,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: item.value == value
                        ? TryptoPalette.background
                        : Colors.transparent,
                    borderRadius: BorderRadius.circular(TryptoRadius.md),
                    boxShadow: item.value == value ? TryptoShadows.sm : null,
                  ),
                  child: Text(
                    item.label,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: labelStyle?.copyWith(
                      color: item.value == value
                          ? TryptoPalette.foreground
                          : TryptoPalette.foreground.withValues(alpha: 0.6),
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
