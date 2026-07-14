import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../models/user.dart';

class FeedbackRepository {
  const FeedbackRepository(this._dio);

  final Dio _dio;

  /// 20~1000자. 응답이 **201 + `code: SUCCESS`** 로 오는 유일한 엔드포인트다
  /// (다른 201 은 `CREATED`). 성공 판정 집합이 두 코드여야 하는 이유가 이것이다.
  Future<void> sendFeedback(String content) => apiCall(
    () async => _dio.post<void>(
      '/api/feedbacks',
      data: SendFeedbackRequest(content: content).toJson(),
    ),
  );
}

final feedbackRepositoryProvider = Provider<FeedbackRepository>(
  (ref) => FeedbackRepository(ref.watch(dioProvider)),
);
