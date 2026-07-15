import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

import '../../core/constants/exchanges.dart';
import '../../core/format/formatters.dart';
import '../../core/theme/theme.dart';
import '../../core/widgets/app_snackbar.dart';
import '../../core/widgets/numeric_text.dart';
import 'round_controller.dart';
import 'round_rules.dart';

/// 웹의 다이얼로그를 바텀시트로 옮긴 것(계획서 §4.6.1). 성공하면 투입 금액을 돌려주고 닫힌다.
Future<int?> showEmergencyFundingSheet(
  BuildContext context, {
  required Exchange exchange,
}) {
  return showModalBottomSheet<int>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (context) => EmergencyFundingSheet(exchange: exchange),
  );
}

class EmergencyFundingSheet extends ConsumerStatefulWidget {
  const EmergencyFundingSheet({super.key, required this.exchange});

  /// 자금은 **지금 보고 있는 거래소** 지갑으로 들어간다(사양서 §4.5).
  final Exchange exchange;

  @override
  ConsumerState<EmergencyFundingSheet> createState() =>
      _EmergencyFundingSheetState();
}

class _EmergencyFundingSheetState extends ConsumerState<EmergencyFundingSheet> {
  final TextEditingController _input = TextEditingController();

  /// **UUID v4 필수** — 서버가 `java.util.UUID` 로 역직렬화해 형식이 틀리면 400 이 아니라
  /// 500 을 낸다(사양서 R8-1). 실패해 다시 누를 때 **같은 키를 재사용**한다.
  final String _idempotencyKey = const Uuid().v4();

  int _amount = 0;
  bool _submitting = false;

  @override
  void dispose() {
    _input.dispose();
    super.dispose();
  }

  void _setAmount(int amount) {
    setState(() => _amount = amount);
    final text = amount == 0 ? '' : formatGrouped(amount);
    if (_input.text == text) return;
    _input.value = TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: text.length),
    );
  }

  Future<void> _submit() async {
    if (_submitting) return;
    setState(() => _submitting = true);

    final error = await ref
        .read(roundControllerProvider.notifier)
        .chargeEmergencyFunding(
          exchangeId: widget.exchange.id,
          amount: _amount,
          idempotencyKey: _idempotencyKey,
        );
    if (!mounted) return;
    setState(() => _submitting = false);

    // 웹은 실패해도 화면에 아무 표시를 하지 않는다(콘솔 로그만). 반드시 알린다(R9).
    if (error != null) {
      showAppSnackbar(context, error, isError: true);
      return;
    }
    Navigator.of(context).pop(_amount);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final round = ref.watch(
      roundControllerProvider.select((state) => state.activeRound),
    );
    if (round == null) return const SizedBox.shrink();

    final limit = round.emergencyFundingLimit.toInt();
    final error = EmergencyFundingPolicy.validateCharge(_amount, limit);
    final exhausted = round.emergencyChargeCount <= 0;

    // 시트가 자체 ScaffoldMessenger 를 갖는다. 바깥 Scaffold 의 스낵바는 시트 뒤로 가려진다.
    return ScaffoldMessenger(
      child: Scaffold(
        backgroundColor: Colors.transparent,
        body: Padding(
          padding: EdgeInsets.only(
            bottom: MediaQuery.viewInsetsOf(context).bottom,
          ),
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(
              TryptoSpacing.screen,
              0,
              TryptoSpacing.screen,
              TryptoSpacing.screen,
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('긴급 자금 투입', style: theme.textTheme.titleLarge),
                const SizedBox(height: TryptoSpacing.xs),
                Text(
                  '${widget.exchange.name} 지갑으로 들어갑니다. '
                  '남은 횟수 ${round.emergencyChargeCount}회 · '
                  '1회 상한 ${formatKRW(round.emergencyFundingLimit)}',
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: TryptoSpacing.lg),
                TextField(
                  controller: _input,
                  autofocus: true,
                  textAlign: TextAlign.right,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  style: TryptoText.numeric(size: 20, weight: FontWeight.w700),
                  onChanged: (text) => setState(
                    () => _amount = int.tryParse(text.replaceAll(',', '')) ?? 0,
                  ),
                  decoration: const InputDecoration(
                    hintText: '0',
                    suffixText: '원',
                  ),
                ),
                const SizedBox(height: TryptoSpacing.sm),
                Row(
                  children: [
                    for (final preset in EmergencyFundingPolicy.presets(limit))
                      Expanded(
                        child: Padding(
                          padding: const EdgeInsets.only(
                            right: TryptoSpacing.xs,
                          ),
                          child: OutlinedButton(
                            onPressed: () => _setAmount(preset),
                            child: Text(formatKRWCompact(preset.toDouble())),
                          ),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: TryptoSpacing.sm),
                SizedBox(
                  height: 20,
                  child: _amount > 0 && error != null
                      ? Text(
                          error,
                          style: theme.textTheme.labelMedium?.copyWith(
                            color: theme.colorScheme.error,
                          ),
                        )
                      : NumericText(
                          _amount > 0 ? formatKRW(_amount.toDouble()) : '',
                          size: 12,
                          weight: FontWeight.w500,
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                ),
                const SizedBox(height: TryptoSpacing.lg),
                SizedBox(
                  width: double.infinity,
                  height: 48,
                  child: FilledButton(
                    onPressed: error == null && !exhausted && !_submitting
                        ? _submit
                        : null,
                    child: _submitting
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('투입 확정'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
