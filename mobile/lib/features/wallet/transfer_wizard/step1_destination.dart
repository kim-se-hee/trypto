import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../../core/format/formatters.dart';
import '../../../core/theme/theme.dart';
import '../../../core/widgets/numeric_text.dart';
import 'step2_amount.dart';
import 'transfer_draft.dart';

/// 웹의 단일 모달을 전체 화면 3단계 마법사로 바꾼다(사양서 §5.4.4) — 모바일에서는 숫자
/// 키패드가 화면 절반을 덮어 모달 안에 셀렉트·입력·오류·버튼을 함께 두면 조작할 수 없다.
///
/// 후보가 하나면 1단계를 건너뛰고 자동 선택한다.
Future<TransferOutcome?> startTransfer(
  BuildContext context,
  TransferDraft draft,
) {
  if (draft.destinations.isEmpty) return Future.value();

  final navigator = Navigator.of(context, rootNavigator: true);
  if (draft.destinations.length == 1) {
    return navigator.push<TransferOutcome>(
      MaterialPageRoute(
        builder: (context) => TransferAmountPage(
          draft: draft,
          destination: draft.destinations.single,
        ),
      ),
    );
  }
  return navigator.push<TransferOutcome>(
    MaterialPageRoute(builder: (context) => TransferDestinationPage(draft: draft)),
  );
}

class TransferDestinationPage extends StatelessWidget {
  const TransferDestinationPage({super.key, required this.draft});

  final TransferDraft draft;

  Future<void> _select(
    BuildContext context,
    TransferDestination destination,
  ) async {
    final outcome = await Navigator.of(context).push<TransferOutcome>(
      MaterialPageRoute(
        builder: (context) =>
            TransferAmountPage(draft: draft, destination: destination),
      ),
    );
    // 성공은 마법사 밖(자산 상세 시트)까지 그대로 올려 보낸다.
    if (outcome != null && context.mounted) Navigator.of(context).pop(outcome);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: Text('${draft.symbol} 출금')),
      body: ListView(
        padding: const EdgeInsets.all(TryptoSpacing.screen),
        children: [
          Text('도착 거래소', style: theme.textTheme.titleMedium),
          const SizedBox(height: TryptoSpacing.xs),
          Text(
            '${draft.fromExchange.name}에서 보낼 거래소를 고르세요. '
            '가용 ${formatQuantity(draft.available)} ${draft.symbol}',
            style: theme.textTheme.labelMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: TryptoSpacing.lg),
          for (final destination in draft.destinations) ...[
            Card(
              child: ListTile(
                title: Text(
                  destination.exchange.name,
                  style: theme.textTheme.titleMedium,
                ),
                subtitle: Text(
                  '${destination.exchange.baseCurrency} 마켓',
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
                trailing: const Icon(LucideIcons.chevronRight, size: 16),
                onTap: () => _select(context, destination),
              ),
            ),
            const SizedBox(height: TryptoSpacing.sm),
          ],
          const SizedBox(height: TryptoSpacing.sm),
          Row(
            children: [
              Icon(
                LucideIcons.info,
                size: 14,
                color: theme.colorScheme.onSurfaceVariant,
              ),
              const SizedBox(width: TryptoSpacing.xs),
              Expanded(
                child: Text(
                  '같은 라운드의 다른 거래소 지갑으로만 보낼 수 있습니다.',
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

/// 마법사 상단의 진행 표시. 3단계 중 어디인지 한 줄로 알린다.
class TransferStepHeader extends StatelessWidget {
  const TransferStepHeader({
    super.key,
    required this.step,
    required this.title,
    required this.description,
  });

  final int step;
  final String title;
  final String description;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        NumericText(
          '$step / 3',
          size: 11,
          weight: FontWeight.w500,
          color: theme.colorScheme.primary,
        ),
        const SizedBox(height: TryptoSpacing.xs),
        Text(title, style: theme.textTheme.titleLarge),
        const SizedBox(height: TryptoSpacing.xs),
        Text(
          description,
          style: theme.textTheme.labelMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}
