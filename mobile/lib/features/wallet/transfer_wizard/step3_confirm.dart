import 'package:decimal/decimal.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/api/api_exception.dart';
import '../../../core/format/formatters.dart';
import '../../../core/json/decimal_x.dart';
import '../../../core/theme/theme.dart';
import '../../../core/widgets/app_snackbar.dart';
import '../../../core/widgets/numeric_text.dart';
import '../../../models/transfer.dart';
import '../transfer_repository.dart';
import 'step1_destination.dart';
import 'transfer_draft.dart';

/// 웹에 없는 단계다(사양서 §5.4.4). 모의투자라도 실전 리허설이 제품 목적이므로 확인 단계를
/// 둔다 — 출발·도착·수량·출금 후 남는 잔고를 한 화면에서 보고 확정한다.
class TransferConfirmPage extends ConsumerStatefulWidget {
  const TransferConfirmPage({
    super.key,
    required this.draft,
    required this.destination,
    required this.amount,
    required this.idempotencyKey,
  });

  final TransferDraft draft;
  final TransferDestination destination;
  final Decimal amount;

  /// 2단계에서 만든 키를 그대로 받는다. 실패 후 다시 누를 때도 같은 키를 보낸다.
  final String idempotencyKey;

  @override
  ConsumerState<TransferConfirmPage> createState() =>
      _TransferConfirmPageState();
}

class _TransferConfirmPageState extends ConsumerState<TransferConfirmPage> {
  bool _submitting = false;

  Future<void> _submit() async {
    if (_submitting) return;
    setState(() => _submitting = true);

    final draft = widget.draft;
    try {
      final response = await ref
          .read(transferRepositoryProvider)
          .transfer(
            TransferCoinRequest(
              idempotencyKey: widget.idempotencyKey,
              fromWalletId: draft.fromWalletId,
              toWalletId: widget.destination.walletId,
              coinId: draft.coinId,
              amount: widget.amount.toDouble(),
            ),
          );
      if (!mounted) return;
      Navigator.of(context).pop(
        TransferOutcome(
          transferId: response.transferId,
          amount: widget.amount.toDouble(),
          symbol: draft.symbol,
        ),
      );
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() => _submitting = false);
      // 웹은 서버 메시지를 버리고 "송금에 실패했습니다." 한 줄만 붉게 띄운다(R9).
      showAppSnackbar(context, transferErrorMessage(error), isError: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final draft = widget.draft;
    final amount = widget.amount.toDouble();
    final remaining = (draft.available.dec - widget.amount).toDouble();

    return Scaffold(
      appBar: AppBar(title: Text('${draft.symbol} 출금')),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(TryptoSpacing.screen),
                children: [
                  const TransferStepHeader(
                    step: 3,
                    title: '출금 확인',
                    description: '아래 내용으로 송금합니다.',
                  ),
                  const SizedBox(height: TryptoSpacing.xl),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(TryptoSpacing.lg),
                      child: Column(
                        children: [
                          Row(
                            children: [
                              Expanded(
                                child: _Endpoint(
                                  label: '출발',
                                  name: draft.fromExchange.name,
                                ),
                              ),
                              Expanded(
                                child: _Endpoint(
                                  label: '도착',
                                  name: widget.destination.exchange.name,
                                  alignEnd: true,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: TryptoSpacing.lg),
                          const Divider(),
                          const SizedBox(height: TryptoSpacing.lg),
                          _Row(
                            label: '출금 수량',
                            value:
                                '${formatQuantity(amount)} ${draft.symbol}',
                            emphasized: true,
                          ),
                          const SizedBox(height: TryptoSpacing.md),
                          _Row(
                            label: '출금 후 가용 잔고',
                            value:
                                '${formatQuantity(remaining)} ${draft.symbol}',
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: TryptoSpacing.md),
                  Text(
                    '* 모의투자 데이터입니다. 실제 자산이 아닙니다.',
                    style: theme.textTheme.labelSmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(TryptoSpacing.screen),
              child: SizedBox(
                width: double.infinity,
                height: 48,
                child: FilledButton(
                  onPressed: _submitting ? null : _submit,
                  child: _submitting
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Text('출금하기'),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Endpoint extends StatelessWidget {
  const _Endpoint({
    required this.label,
    required this.name,
    this.alignEnd = false,
  });

  final String label;
  final String name;
  final bool alignEnd;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: alignEnd
          ? CrossAxisAlignment.end
          : CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: theme.textTheme.labelSmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 2),
        Text(name, style: theme.textTheme.titleMedium),
      ],
    );
  }
}

class _Row extends StatelessWidget {
  const _Row({
    required this.label,
    required this.value,
    this.emphasized = false,
  });

  final String label;
  final String value;
  final bool emphasized;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Row(
      children: [
        Expanded(
          child: Text(
            label,
            style: theme.textTheme.labelLarge?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ),
        NumericText(
          value,
          size: emphasized ? 18 : 14,
          weight: emphasized ? FontWeight.w700 : FontWeight.w500,
        ),
      ],
    );
  }
}
