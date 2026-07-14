import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:uuid/uuid.dart';

import '../../../core/format/formatters.dart';
import '../../../core/theme/theme.dart';
import 'step1_destination.dart';
import 'step3_confirm.dart';
import 'transfer_draft.dart';

/// 소수점 하나까지만 허용한다. 서버 수량은 소수 8자리다.
final RegExp _kAmountPattern = RegExp(r'^\d*\.?\d*$');

class TransferAmountPage extends StatefulWidget {
  const TransferAmountPage({
    super.key,
    required this.draft,
    required this.destination,
  });

  final TransferDraft draft;
  final TransferDestination destination;

  @override
  State<TransferAmountPage> createState() => _TransferAmountPageState();
}

class _TransferAmountPageState extends State<TransferAmountPage> {
  final TextEditingController _input = TextEditingController();

  /// **2단계 진입 시 1회 생성해 화면 상태에 보관한다**(사양서 §5.4.4). 재시도할 때 키를 다시
  /// 만들면 멱등성이 깨져 같은 송금이 두 번 실행될 수 있다. UUID v4 가 아니면 서버가 500 을
  /// 낸다(R8-1).
  final String _idempotencyKey = const Uuid().v4();

  String _amount = '';

  @override
  void dispose() {
    _input.dispose();
    super.dispose();
  }

  void _setRatio(double ratio) {
    final text = transferAmountOfRatio(widget.draft.available, ratio);
    _input.value = TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: text.length),
    );
    setState(() => _amount = text);
  }

  Future<void> _next() async {
    final amount = parseTransferAmount(_amount);
    if (amount == null) return;

    final outcome = await Navigator.of(context).push<TransferOutcome>(
      MaterialPageRoute(
        builder: (context) => TransferConfirmPage(
          draft: widget.draft,
          destination: widget.destination,
          amount: amount,
          idempotencyKey: _idempotencyKey,
        ),
      ),
    );
    if (outcome != null && mounted) Navigator.of(context).pop(outcome);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final draft = widget.draft;
    final error = validateTransferAmount(_amount, draft.available);

    return Scaffold(
      appBar: AppBar(title: Text('${draft.symbol} 출금')),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(TryptoSpacing.screen),
                children: [
                  TransferStepHeader(
                    step: 2,
                    title: '출금 수량',
                    description:
                        '${draft.fromExchange.name} → '
                        '${widget.destination.exchange.name}',
                  ),
                  const SizedBox(height: TryptoSpacing.xl),
                  TextField(
                    controller: _input,
                    autofocus: true,
                    textAlign: TextAlign.right,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    inputFormatters: [
                      FilteringTextInputFormatter.allow(_kAmountPattern),
                    ],
                    style: TryptoText.numeric(size: 24, weight: FontWeight.w700),
                    onChanged: (text) => setState(() => _amount = text),
                    decoration: InputDecoration(
                      hintText: '0',
                      suffixText: draft.symbol,
                    ),
                  ),
                  const SizedBox(height: TryptoSpacing.sm),
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          '가용 ${formatQuantity(draft.available)} ${draft.symbol}',
                          style: theme.textTheme.labelMedium?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ),
                      // 입력 즉시 검증한다. 빈 입력에서는 오류를 띄우지 않는다.
                      if (_amount.isNotEmpty && error != null)
                        Text(
                          error,
                          style: theme.textTheme.labelMedium?.copyWith(
                            color: theme.colorScheme.error,
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(height: TryptoSpacing.lg),
                  Row(
                    children: [
                      for (final (label, ratio) in const [
                        ('25%', 0.25),
                        ('50%', 0.5),
                        ('최대', 1.0),
                      ])
                        Expanded(
                          child: Padding(
                            padding: const EdgeInsets.only(
                              right: TryptoSpacing.xs,
                            ),
                            child: OutlinedButton(
                              onPressed: draft.available > 0
                                  ? () => _setRatio(ratio)
                                  : null,
                              child: Text(label),
                            ),
                          ),
                        ),
                    ],
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
                  onPressed: error == null ? _next : null,
                  child: const Text('다음'),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
