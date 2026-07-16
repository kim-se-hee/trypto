import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../core/format/formatters.dart';
import '../../core/theme/theme.dart';
import '../../core/widgets/app_snackbar.dart';
import '../../core/widgets/numeric_text.dart';
import '../../models/enums.dart';
import '../auth/auth_controller.dart';
import 'round_controller.dart';
import 'round_rules.dart';

enum _AmountField { seed, emergency }

/// 2단계 스텝 + 하단 고정 CTA(계획서 §5.3). 검증은 **입력 단계에서** 한다 — 웹은 제출 시점에
/// 서버 400·500 을 받고 그 메시지마저 버린다.
class RoundCreatePage extends ConsumerStatefulWidget {
  const RoundCreatePage({super.key});

  @override
  ConsumerState<RoundCreatePage> createState() => _RoundCreatePageState();
}

class _RoundCreatePageState extends ConsumerState<RoundCreatePage> {
  static const List<int> _seedPresets = [1000000, 5000000, 10000000, 50000000];
  static const List<int> _emergencyPresets = [100000, 500000, 1000000];

  int _step = 0;
  int _seed = 0;
  int _emergencyLimit = 0;
  _AmountField _target = _AmountField.seed;
  bool _submitting = false;

  final Map<RuleType, int> _values = {
    for (final config in ruleConfigs) config.ruleType: config.defaultValue,
  };
  final Set<RuleType> _enabled = {};

  RoundDraft get _draft => RoundDraft(
    seed: _seed,
    emergencyLimit: _emergencyLimit,
    rules: {for (final type in _enabled) type: _values[type]!},
  );

  bool get _fundingReady =>
      _draft.seedError == null && _draft.emergencyError == null;

  void _key(String key) {
    setState(() {
      final current = _target == _AmountField.seed ? _seed : _emergencyLimit;
      final next = key == '<' ? current ~/ 10 : _append(current, key);
      if (_target == _AmountField.seed) {
        _seed = next;
      } else {
        _emergencyLimit = next;
      }
    });
  }

  int _append(int current, String digits) {
    final text = '$current$digits';
    if (text.length > 11) return current;
    return int.parse(text);
  }

  Future<void> _submit() async {
    final draft = _draft;
    if (!draft.canSubmit || _submitting) return;

    setState(() => _submitting = true);
    final error = await ref
        .read(roundControllerProvider.notifier)
        .createRound(draft.toRequest());
    if (!mounted) return;
    setState(() => _submitting = false);

    // 성공 시 화면 이동은 하지 않는다 — 활성 라운드가 생기면 가드가 /market 으로 보낸다.
    if (error != null) showAppSnackbar(context, error, isError: true);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        leading: _step == 0
            ? null
            : IconButton(
                icon: const Icon(LucideIcons.arrowLeft),
                onPressed: () => setState(() => _step = 0),
              ),
        title: const Text('투자 라운드 시작'),
        actions: [
          IconButton(
            icon: const Icon(LucideIcons.logOut),
            tooltip: '로그아웃',
            onPressed: () => ref.read(authControllerProvider.notifier).logout(),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            _StepBar(step: _step),
            Expanded(
              child: _step == 0 ? _buildFundingStep(theme) : _buildRulesStep(),
            ),
            _Cta(
              label: _step == 0 ? '다음' : '라운드 시작하기',
              hint: _step == 0
                  ? (_fundingReady ? null : '시드머니와 긴급 자금 상한을 설정해 주세요.')
                  : _draft.rulesError,
              busy: _submitting,
              onPressed: _step == 0
                  ? (_fundingReady ? () => setState(() => _step = 1) : null)
                  : (_draft.canSubmit ? _submit : null),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFundingStep(ThemeData theme) {
    return Column(
      children: [
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(TryptoSpacing.screen),
            children: [
              Text(
                '시드머니와 투자 원칙을 설정하고 모의투자를 시작하세요.',
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: TryptoSpacing.lg),
              _AmountCard(
                title: '시작 자금',
                description: '모의투자에 사용할 초기 자본금',
                amount: _seed,
                presets: _seedPresets,
                selected: _target == _AmountField.seed,
                error: _seed > 0 ? _draft.seedError : null,
                onSelect: () => setState(() => _target = _AmountField.seed),
                onPreset: (value) => setState(() {
                  _seed = value;
                  _target = _AmountField.seed;
                }),
              ),
              const SizedBox(height: TryptoSpacing.md),
              _AmountCard(
                title: '긴급 자금 투입 상한',
                description: '1회당 최대 투입 금액',
                amount: _emergencyLimit,
                presets: _emergencyPresets,
                selected: _target == _AmountField.emergency,
                error: _emergencyLimit > 0 ? _draft.emergencyError : null,
                note:
                    '라운드 진행 중 최대 ${EmergencyFundingPolicy.chargeCount}회까지 '
                    '긴급 자금을 투입할 수 있습니다',
                onSelect: () => setState(() => _target = _AmountField.emergency),
                onPreset: (value) => setState(() {
                  _emergencyLimit = value;
                  _target = _AmountField.emergency;
                }),
              ),
            ],
          ),
        ),
        _Keypad(onKey: _key),
      ],
    );
  }

  Widget _buildRulesStep() {
    final theme = Theme.of(context);

    return ListView(
      padding: const EdgeInsets.all(TryptoSpacing.screen),
      children: [
        Row(
          children: [
            Text('투자 원칙', style: theme.textTheme.titleMedium),
            const Spacer(),
            if (_enabled.isEmpty)
              Text(
                '최소 1개 이상',
                style: theme.textTheme.labelMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              )
            else
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                  color: theme.colorScheme.primary.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Text(
                  '${_enabled.length}개 활성',
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: theme.colorScheme.primary,
                  ),
                ),
              ),
          ],
        ),
        const SizedBox(height: TryptoSpacing.md),
        for (final config in ruleConfigs) ...[
          _RuleCard(
            config: config,
            enabled: _enabled.contains(config.ruleType),
            value: _values[config.ruleType]!,
            onToggle: () => setState(() {
              if (!_enabled.remove(config.ruleType)) {
                _enabled.add(config.ruleType);
              }
            }),
            onChanged: (value) => setState(
              () => _values[config.ruleType] = config.clamp(value),
            ),
          ),
          const SizedBox(height: TryptoSpacing.md),
        ],
      ],
    );
  }
}

class _StepBar extends StatelessWidget {
  const _StepBar({required this.step});

  final int step;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Padding(
      padding: const EdgeInsets.fromLTRB(
        TryptoSpacing.screen,
        TryptoSpacing.md,
        TryptoSpacing.screen,
        0,
      ),
      child: Row(
        children: [
          for (var i = 0; i < 2; i++) ...[
            Expanded(
              child: Container(
                height: 4,
                decoration: BoxDecoration(
                  color: i <= step
                      ? theme.colorScheme.primary
                      : TryptoPalette.secondary,
                  borderRadius: BorderRadius.circular(TryptoRadius.xs),
                ),
              ),
            ),
            const SizedBox(width: TryptoSpacing.xs),
          ],
          Text(
            '${step + 1}/2',
            style: theme.textTheme.labelMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}

class _AmountCard extends StatelessWidget {
  const _AmountCard({
    required this.title,
    required this.description,
    required this.amount,
    required this.presets,
    required this.selected,
    required this.onSelect,
    required this.onPreset,
    this.error,
    this.note,
  });

  final String title;
  final String description;
  final int amount;
  final List<int> presets;
  final bool selected;
  final VoidCallback onSelect;
  final ValueChanged<int> onPreset;
  final String? error;
  final String? note;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final empty = amount == 0;

    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onSelect,
      child: Container(
        padding: const EdgeInsets.all(TryptoSpacing.lg),
        decoration: BoxDecoration(
          color: TryptoPalette.card,
          borderRadius: BorderRadius.circular(TryptoRadius.xl),
          border: Border.all(
            color: selected ? theme.colorScheme.primary : TryptoPalette.border,
            width: selected ? 1.5 : 1,
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: theme.textTheme.titleMedium),
            const SizedBox(height: TryptoSpacing.xs),
            Text(
              description,
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: TryptoSpacing.md),
            NumericText(
              '₩${formatGrouped(amount)}',
              size: 28,
              weight: FontWeight.w700,
              color: empty
                  ? theme.colorScheme.onSurfaceVariant.withValues(alpha: 0.4)
                  : theme.colorScheme.onSurface,
            ),
            if (!empty) ...[
              const SizedBox(height: TryptoSpacing.xs),
              Text(
                formatKRW(amount.toDouble()),
                style: theme.textTheme.labelMedium?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
            if (error != null) ...[
              const SizedBox(height: TryptoSpacing.sm),
              Text(
                error!,
                style: theme.textTheme.labelMedium?.copyWith(
                  color: theme.colorScheme.error,
                ),
              ),
            ],
            const SizedBox(height: TryptoSpacing.md),
            Row(
              children: [
                for (final preset in presets) ...[
                  Expanded(
                    child: _PresetButton(
                      value: preset,
                      active: preset == amount,
                      onTap: () => onPreset(preset),
                    ),
                  ),
                  if (preset != presets.last)
                    const SizedBox(width: TryptoSpacing.xs),
                ],
              ],
            ),
            if (note != null) ...[
              const SizedBox(height: TryptoSpacing.sm),
              Text(
                note!,
                style: TryptoText.micro.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// 프리셋은 토글이 아니라 값 덮어쓰기다(사양서 §7.3.1). 현재 값과 정확히 일치할 때만 활성 모양이다.
class _PresetButton extends StatelessWidget {
  const _PresetButton({
    required this.value,
    required this.active,
    required this.onTap,
  });

  final int value;
  final bool active;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 32,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: active
              ? theme.colorScheme.primary
              : TryptoPalette.secondary,
          borderRadius: BorderRadius.circular(TryptoRadius.md),
        ),
        child: Text(
          formatKRWCompact(value.toDouble()),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: theme.textTheme.labelMedium?.copyWith(
            color: active
                ? theme.colorScheme.onPrimary
                : theme.colorScheme.onSurface,
          ),
        ),
      ),
    );
  }
}

/// 금액 입력 전용 키패드. 시스템 키보드를 띄우면 카드 두 장이 화면 밖으로 밀린다.
class _Keypad extends StatelessWidget {
  const _Keypad({required this.onKey});

  final ValueChanged<String> onKey;

  static const List<List<String>> _rows = [
    ['1', '2', '3'],
    ['4', '5', '6'],
    ['7', '8', '9'],
    ['00', '0', '<'],
  ];

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: TryptoSpacing.sm,
        vertical: TryptoSpacing.sm,
      ),
      decoration: const BoxDecoration(
        color: TryptoPalette.secondary,
        border: Border(top: BorderSide(color: TryptoPalette.border)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (final row in _rows)
            Row(
              children: [
                for (final key in row)
                  Expanded(
                    child: InkWell(
                      onTap: () => onKey(key),
                      borderRadius: BorderRadius.circular(TryptoRadius.md),
                      child: SizedBox(
                        height: 48,
                        child: Center(
                          child: key == '<'
                              ? const Icon(LucideIcons.delete, size: 20)
                              : NumericText(
                                  key,
                                  size: 20,
                                  color: theme.colorScheme.onSurface,
                                ),
                        ),
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

/// 카드 전체가 스위치다(사양서 §7.3.2).
class _RuleCard extends StatelessWidget {
  const _RuleCard({
    required this.config,
    required this.enabled,
    required this.value,
    required this.onToggle,
    required this.onChanged,
  });

  final RuleConfig config;
  final bool enabled;
  final int value;
  final VoidCallback onToggle;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final muted = theme.colorScheme.onSurfaceVariant;

    return Semantics(
      toggled: enabled,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onToggle,
        child: AnimatedContainer(
          duration: TryptoMotion.colorTransition,
          padding: const EdgeInsets.all(TryptoSpacing.lg),
          decoration: BoxDecoration(
            color: enabled
                ? TryptoPalette.card
                : TryptoPalette.secondary.withValues(alpha: 0.3),
            borderRadius: BorderRadius.circular(TryptoRadius.xl),
            border: Border.all(
              color: enabled
                  ? theme.colorScheme.primary.withValues(alpha: 0.5)
                  : Colors.transparent,
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          config.label,
                          style: theme.textTheme.titleMedium?.copyWith(
                            color: enabled
                                ? theme.colorScheme.onSurface
                                : muted,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          config.description,
                          style: theme.textTheme.labelMedium?.copyWith(
                            color: muted,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Switch(value: enabled, onChanged: (_) => onToggle()),
                ],
              ),
              if (enabled) ...[
                const SizedBox(height: TryptoSpacing.md),
                if (config.input == RuleInputKind.slider)
                  _SliderInput(
                    config: config,
                    value: value,
                    onChanged: onChanged,
                  )
                else
                  _StepperInput(
                    config: config,
                    value: value,
                    onChanged: onChanged,
                  ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _SliderInput extends StatelessWidget {
  const _SliderInput({
    required this.config,
    required this.value,
    required this.onChanged,
  });

  final RuleConfig config;
  final int value;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Slider(
            value: value.toDouble(),
            min: config.min.toDouble(),
            max: config.max.toDouble(),
            divisions: config.max - config.min,
            onChanged: (next) => onChanged(next.round()),
          ),
        ),
        SizedBox(
          width: 64,
          child: Align(
            alignment: Alignment.centerRight,
            child: NumericText('$value${config.unit}', size: 16),
          ),
        ),
      ],
    );
  }
}

class _StepperInput extends StatelessWidget {
  const _StepperInput({
    required this.config,
    required this.value,
    required this.onChanged,
  });

  final RuleConfig config;
  final int value;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text('${config.min} ~ ${config.max}${config.unit}'),
        Row(
          children: [
            IconButton.outlined(
              icon: const Icon(LucideIcons.minus, size: 16),
              onPressed: value > config.min ? () => onChanged(value - 1) : null,
            ),
            SizedBox(
              width: 72,
              child: Center(
                child: NumericText('$value${config.unit}', size: 16),
              ),
            ),
            IconButton.outlined(
              icon: const Icon(LucideIcons.plus, size: 16),
              onPressed: value < config.max ? () => onChanged(value + 1) : null,
            ),
          ],
        ),
      ],
    );
  }
}

class _Cta extends StatelessWidget {
  const _Cta({
    required this.label,
    required this.busy,
    required this.onPressed,
    this.hint,
  });

  final String label;
  final bool busy;
  final VoidCallback? onPressed;
  final String? hint;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      padding: const EdgeInsets.all(TryptoSpacing.screen),
      decoration: const BoxDecoration(
        color: TryptoPalette.background,
        border: Border(top: BorderSide(color: TryptoPalette.border)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (hint != null) ...[
            Text(
              hint!,
              textAlign: TextAlign.center,
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: TryptoSpacing.sm),
          ],
          SizedBox(
            width: double.infinity,
            height: 48,
            child: FilledButton(
              onPressed: busy ? null : onPressed,
              child: busy
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : Text(label),
            ),
          ),
        ],
      ),
    );
  }
}
