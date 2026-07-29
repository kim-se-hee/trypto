import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:trypto/core/api/api_exception.dart';
import 'package:trypto/features/mypage/feedback_card.dart';
import 'package:trypto/features/mypage/feedback_repository.dart';
import 'package:trypto/features/mypage/nickname_sheet.dart';
import 'package:trypto/features/mypage/user_repository.dart';
import 'package:trypto/models/user.dart';

/// U10 완료 조건: 저장·전송이 **어떤 예외로 끝나도** 버튼이 '저장 중...' · '보내는 중...' 으로
/// 굳지 않는다. 웹은 `finally` 로 어떤 예외에서도 버튼을 되살리고(FeedbackCard.tsx:30-32,
/// MyPage.tsx:91-94), `catch` 하나로 모든 예외를 삼킨 뒤 대체 문구를 보여준다
/// (FeedbackCard.tsx:28-29). 예외가 화면 밖으로 새면 사용자는 실패를 알 수 없다.

/// 응답 시점을 테스트가 정한다 — 로딩 중간 상태를 확인해야 한다.
class _PendingFeedbackRepository extends FeedbackRepository {
  _PendingFeedbackRepository() : super(Dio());

  final Completer<void> completer = Completer<void>();

  @override
  Future<void> sendFeedback(String content) => completer.future;
}

class _PendingUserRepository extends UserRepository {
  _PendingUserRepository() : super(Dio());

  final Completer<ChangeNicknameResponse> completer =
      Completer<ChangeNicknameResponse>();

  @override
  Future<ChangeNicknameResponse> changeNickname(String nickname) =>
      completer.future;
}

void main() {
  Widget host({required List<Override> overrides, required Widget child}) =>
      ProviderScope(
        overrides: overrides,
        child: MaterialApp(home: Scaffold(body: child)),
      );

  Future<void> pumpFeedback(
    WidgetTester tester,
    _PendingFeedbackRepository repository,
  ) async {
    await tester.pumpWidget(
      host(
        overrides: [feedbackRepositoryProvider.overrideWithValue(repository)],
        child: const SingleChildScrollView(child: FeedbackCard()),
      ),
    );
    await tester.enterText(find.byType(TextField), '가' * 20);
    await tester.pump();
  }

  testWidgets('피드백 전송이 실패해도 보내기 버튼이 되살아난다', (tester) async {
    final repository = _PendingFeedbackRepository();
    await pumpFeedback(tester, repository);

    await tester.tap(find.widgetWithText(FilledButton, '보내기'));
    await tester.pump();
    expect(find.text('보내는 중...'), findsOneWidget);

    repository.completer.completeError(
      const ApiException(status: 400, message: '피드백 전송에 실패했습니다.'),
    );
    await tester.pumpAndSettle();

    expect(find.text('보내는 중...'), findsNothing);
    expect(
      tester
          .widget<FilledButton>(find.widgetWithText(FilledButton, '보내기'))
          .onPressed,
      isNotNull,
    );
    expect(find.text('피드백 전송에 실패했습니다.'), findsOneWidget);
  });

  testWidgets('ApiException 이 아닌 예외로 끝나도 보내기 버튼이 되살아난다', (tester) async {
    final repository = _PendingFeedbackRepository();
    await pumpFeedback(tester, repository);

    await tester.tap(find.widgetWithText(FilledButton, '보내기'));
    await tester.pump();
    expect(find.text('보내는 중...'), findsOneWidget);

    // `on ApiException` 이 잡지 못하는 예외. finally 가 없으면 여기서 영영 굳는다.
    repository.completer.completeError(StateError('해석할 수 없는 응답'));
    await tester.pumpAndSettle();

    // 웹처럼 어떤 예외든 화면이 삼킨다. 새어 나가면 flutter_test 가 미처리 오류로
    // 이 테스트를 실패시킨다.
    expect(tester.takeException(), isNull);
    // ApiException 이 아니면 웹은 고정 대체 문구를 보여준다(FeedbackCard.tsx:29).
    expect(find.text('피드백 전송에 실패했습니다.'), findsOneWidget);
    expect(find.text('보내는 중...'), findsNothing);
    expect(
      tester
          .widget<FilledButton>(find.widgetWithText(FilledButton, '보내기'))
          .onPressed,
      isNotNull,
    );
  });

  testWidgets('닉네임 저장이 실패해도 저장 버튼이 되살아난다', (tester) async {
    final repository = _PendingUserRepository();
    await tester.pumpWidget(
      host(
        overrides: [userRepositoryProvider.overrideWithValue(repository)],
        child: Builder(
          builder: (context) => TextButton(
            onPressed: () => showNicknameSheet(context, current: '테스터'),
            child: const Text('열기'),
          ),
        ),
      ),
    );

    await tester.tap(find.text('열기'));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField), '새닉네임');
    await tester.pump();
    await tester.tap(find.widgetWithText(FilledButton, '저장'));
    await tester.pump();
    expect(find.text('저장 중...'), findsOneWidget);

    repository.completer.completeError(
      const ApiException(status: 409, message: '이미 사용 중인 닉네임입니다.'),
    );
    await tester.pumpAndSettle();

    // 시트는 열린 채로 남고 오류만 시트 안에 뜬다.
    expect(find.text('저장 중...'), findsNothing);
    expect(find.text('이미 사용 중인 닉네임입니다.'), findsOneWidget);

    // 값을 고치면 다시 시도할 수 있다 — 저장 플래그가 풀렸다는 뜻이다.
    await tester.enterText(find.byType(TextField), '다른닉네임');
    await tester.pump();
    expect(
      tester
          .widget<FilledButton>(find.widgetWithText(FilledButton, '저장'))
          .onPressed,
      isNotNull,
    );
  });
}
