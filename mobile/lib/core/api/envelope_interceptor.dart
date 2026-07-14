import 'package:dio/dio.dart';

import '../../models/envelope.dart';
import 'api_exception.dart';

/// 공통 응답 봉투를 벗긴다(사양서 §1.2, §1.11.1).
///
/// 성공 판정은 `2xx && code ∈ {SUCCESS, CREATED}` 세 조건 모두다. 언랩 후 `response.data` 는
/// 봉투의 `data` 이며, 호출부는 봉투를 절대 보지 않는다.
///
/// 실패를 `reject` 가 아니라 `next`·`reject(err, true)` 로 흘리는 이유: Dio 는 `reject` 를 만나면
/// 뒤따르는 인터셉터의 `onError` 를 통째로 건너뛴다. 그렇게 하면 뒤에 붙은
/// [UnauthorizedInterceptor] 가 401 을 영영 보지 못한다.
class EnvelopeInterceptor extends Interceptor {
  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    final envelope = ApiEnvelope.tryParse(response.data);
    if (envelope == null) {
      return handler.reject(
        DioException(
          requestOptions: response.requestOptions,
          response: response,
          error: ApiException.invalidResponse(response.statusCode),
        ),
        true,
      );
    }
    if (!envelope.isSuccess) {
      return handler.reject(
        DioException(
          requestOptions: response.requestOptions,
          response: response,
          error: ApiException.fromEnvelope(envelope),
        ),
        true,
      );
    }
    response.data = envelope.data;
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    final envelope = ApiEnvelope.tryParse(err.response?.data);
    final exception = envelope != null
        ? ApiException.fromEnvelope(envelope)
        : err.response != null
        ? ApiException.invalidResponse(err.response?.statusCode)
        : ApiException.network(err);
    handler.next(err.copyWith(error: exception));
  }
}
