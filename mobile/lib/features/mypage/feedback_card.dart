import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_exception.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../../core/widgets/app_snackbar.dart';
import 'feedback_repository.dart';

const int kFeedbackMin = 20;
const int kFeedbackMax = 1000;

/// 길이 기준은 `trim()` 후의 길이다 — 서버도 strip 후 20~1000자를 강제한다(사양서 §7.5.3).
String? validateFeedback(String raw) {
  final value = raw.trim();
  if (value.length < kFeedbackMin) return '최소 $kFeedbackMin자 이상 입력해주세요.';
  if (value.length > kFeedbackMax) return '$kFeedbackMax자 이하로 입력해주세요.';
  return null;
}

class FeedbackCard extends ConsumerStatefulWidget {
  const FeedbackCard({super.key});

  @override
  ConsumerState<FeedbackCard> createState() => _FeedbackCardState();
}

class _FeedbackCardState extends ConsumerState<FeedbackCard> {
  final TextEditingController _controller = TextEditingController();

  bool _sending = false;
  int _length = 0;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _send() async {
    final content = _controller.text.trim();
    if (validateFeedback(content) != null) return;

    setState(() => _sending = true);
    try {
      await ref.read(feedbackRepositoryProvider).sendFeedback(content);
      if (!mounted) return;
      _controller.clear();
      setState(() {
        _sending = false;
        _length = 0;
      });
      showAppSnackbar(context, '피드백이 접수되었습니다. 소중한 의견 감사합니다.');
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() => _sending = false);
      showAppSnackbar(context, error.userMessage, isError: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = context.tryptoColors;
    final short = _length < kFeedbackMin;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(TryptoSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('피드백 보내기', style: theme.textTheme.titleMedium),
            const SizedBox(height: TryptoSpacing.xs),
            Text(
              '사용하면서 느낀 점이나 개선했으면 하는 부분을 알려주세요.',
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: TryptoSpacing.md),
            TextField(
              controller: _controller,
              maxLines: 5,
              maxLength: kFeedbackMax,
              enabled: !_sending,
              decoration: const InputDecoration(
                hintText: '어떤 점이 좋았고, 무엇이 아쉬웠나요?',
                counterText: '',
              ),
              onChanged: (value) =>
                  setState(() => _length = value.trim().length),
            ),
            const SizedBox(height: TryptoSpacing.xs),
            Align(
              alignment: Alignment.centerRight,
              child: Text(
                short && _length > 0
                    ? '최소 $kFeedbackMin자 이상 입력해주세요.'
                    : '$_length / $kFeedbackMax자',
                style: theme.textTheme.labelSmall?.copyWith(
                  color: short && _length > 0
                      ? colors.negative
                      : theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ),
            const SizedBox(height: TryptoSpacing.md),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: _sending || short ? null : _send,
                child: Text(_sending ? '보내는 중...' : '보내기'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
