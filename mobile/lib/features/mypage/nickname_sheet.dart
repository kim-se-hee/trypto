import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_exception.dart';
import '../../core/theme/theme.dart';
import '../../core/theme/trypto_colors.dart';
import '../auth/auth_controller.dart';
import 'user_repository.dart';

const int kNicknameMin = 2;
const int kNicknameMax = 20;

/// 서버 제약은 2~20자다(위반 시 400 `INVALID_NICKNAME_LENGTH`). 웹은 길이 검증을 하지 않고
/// 실패해도 화면에 아무 표시가 없다(사양서 §7.5.1 · R9). **입력 단계에서** 막는다.
String? validateNickname(String raw) {
  final value = raw.trim();
  if (value.length < kNicknameMin || value.length > kNicknameMax) {
    return '닉네임은 $kNicknameMin~$kNicknameMax자여야 합니다.';
  }
  return null;
}

/// 닉네임을 실제로 바꿨으면 `true`. 호출부가 성공 스낵바를 띄운다 — 시트가 닫힌 뒤에는 시트의
/// `context` 로 스낵바를 띄울 수 없다.
Future<bool> showNicknameSheet(
  BuildContext context, {
  required String current,
}) async {
  final changed = await showModalBottomSheet<bool>(
    context: context,
    useSafeArea: true,
    isScrollControlled: true,
    builder: (context) => Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.viewInsetsOf(context).bottom,
      ),
      child: _NicknameSheet(current: current),
    ),
  );
  return changed ?? false;
}

class _NicknameSheet extends ConsumerStatefulWidget {
  const _NicknameSheet({required this.current});

  final String current;

  @override
  ConsumerState<_NicknameSheet> createState() => _NicknameSheetState();
}

class _NicknameSheetState extends ConsumerState<_NicknameSheet> {
  late final TextEditingController _controller = TextEditingController(
    text: widget.current,
  );

  bool _saving = false;
  String? _error;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    final value = _controller.text.trim();
    final invalid = validateNickname(value);
    if (invalid != null) {
      setState(() => _error = invalid);
      return;
    }
    // 값이 그대로면 요청을 보내지 않는다.
    if (value == widget.current) {
      Navigator.of(context).pop(false);
      return;
    }

    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      final response = await ref
          .read(userRepositoryProvider)
          .changeNickname(value);
      ref.read(authControllerProvider.notifier).updateNickname(
        response.nickname,
      );
      ref.invalidate(userProfileProvider);
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } on ApiException catch (error) {
      if (!mounted) return;
      // 시트 안에서 알린다. 시트 뒤로 스낵바를 던지면 가려서 보이지 않는다.
      setState(() {
        _saving = false;
        _error = error.userMessage;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final error = _error;

    return SafeArea(
      child: Padding(
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
            Text('닉네임 변경', style: theme.textTheme.titleLarge),
            const SizedBox(height: TryptoSpacing.lg),
            TextField(
              controller: _controller,
              autofocus: true,
              maxLength: kNicknameMax,
              enabled: !_saving,
              textInputAction: TextInputAction.done,
              decoration: InputDecoration(
                hintText: '$kNicknameMin~$kNicknameMax자',
                errorText: error,
                counterText: '',
              ),
              onChanged: (value) {
                final invalid = validateNickname(value);
                if (invalid == _error) return;
                setState(() => _error = invalid);
              },
              onSubmitted: (_) => _save(),
            ),
            const SizedBox(height: TryptoSpacing.xs),
            Text(
              '${_controller.text.trim().length} / $kNicknameMax자',
              style: theme.textTheme.labelSmall?.copyWith(
                color: error == null
                    ? theme.colorScheme.onSurfaceVariant
                    : context.tryptoColors.negative,
              ),
            ),
            const SizedBox(height: TryptoSpacing.lg),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: _saving || error != null ? null : _save,
                child: Text(_saving ? '저장 중...' : '저장'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
