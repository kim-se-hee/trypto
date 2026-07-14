import 'package:dio/dio.dart';

import '../../models/envelope.dart';

/// 흐름을 바꾸는 서버 코드만 상수로 승격한다(계획서 §4.1.4). 나머지 코드는 서버가 내려주는
/// 한국어 `message` 를 그대로 보여주므로 이름을 알 필요가 없다.
class ErrorCodes {
  const ErrorCodes._();

  static const String unauthenticated = 'UNAUTHENTICATED';
  static const String socialLoginFailed = 'SOCIAL_LOGIN_FAILED';

  /// 409. 예외가 아니라 `null` 로 바꾼다. 놓치면 신규 사용자가 앱에 진입하지 못한다.
  static const String roundNotActive = 'ROUND_NOT_ACTIVE';

  /// 404. 랭커 포트폴리오 → 빈 상태.
  static const String rankingNotFound = 'RANKING_NOT_FOUND';

  /// 403. 100위 초과. 요청 자체를 선제 차단한다.
  static const String portfolioViewNotAllowed = 'PORTFOLIO_VIEW_NOT_ALLOWED';

  /// 서버가 내려주지 않는 클라이언트 합성 코드 2종.
  static const String networkError = 'NETWORK_ERROR';
  static const String invalidResponse = 'INVALID_RESPONSE';
}

/// 모든 API 실패의 단일 타입. 봉투 실패·비봉투 응답·네트워크 오류가 전부 여기로 모인다.
class ApiException implements Exception {
  const ApiException({this.status, this.code, this.message, this.cause});

  factory ApiException.fromEnvelope(ApiEnvelope envelope) => ApiException(
    status: envelope.status,
    code: envelope.code,
    message: envelope.message,
  );

  /// 2xx 인데 봉투 모양이 아니다(프록시 오류 페이지 등).
  factory ApiException.invalidResponse(int? status) => ApiException(
    status: status,
    code: ErrorCodes.invalidResponse,
    message: '서버 응답을 해석할 수 없습니다.',
  );

  /// 타임아웃·연결 실패. 응답 자체가 없다.
  factory ApiException.network(DioException cause) =>
      ApiException(code: ErrorCodes.networkError, cause: cause);

  final int? status;
  final String? code;
  final String? message;
  final Object? cause;

  /// 서버 `message` 는 이미 한국어 완성문이다(사양서 §1.11.4). 전역 코드→문구 표를 만들지 않는다.
  String get userMessage {
    final message = this.message;
    if (message != null && message.isNotEmpty) return message;
    return switch (code) {
      ErrorCodes.networkError => '네트워크에 연결할 수 없습니다.',
      'INTERNAL_SERVER_ERROR' => '일시적인 오류입니다. 잠시 후 다시 시도해 주세요.',
      _ => '요청 처리 중 오류가 발생했습니다.',
    };
  }

  bool get isUnauthenticated => code == ErrorCodes.unauthenticated;

  bool isCode(String code) => this.code == code;

  @override
  String toString() => 'ApiException($status, $code, $message)';
}

extension ApiExceptionX on Object {
  /// 인터셉터가 항상 `ApiException` 을 실어 보내지만, 인터셉터를 타지 않는 경로
  /// (요청 조립 중 오류 등)가 남아 있어 방어한다.
  ApiException get asApiException {
    final self = this;
    if (self is ApiException) return self;
    if (self is DioException) {
      final error = self.error;
      if (error is ApiException) return error;
      return ApiException.network(self);
    }
    return ApiException(message: '요청 처리 중 오류가 발생했습니다.', cause: self);
  }
}

/// Repository 의 모든 호출을 감싼다. 화면 계층은 [ApiException] 하나만 알면 된다 —
/// `DioException` 이 UI 까지 새어 나가면 catch 마다 언랩 코드가 붙는다.
Future<T> apiCall<T>(Future<T> Function() request) async {
  try {
    return await request();
  } on DioException catch (error, stackTrace) {
    Error.throwWithStackTrace((error.error ?? error).asApiException, stackTrace);
  }
}
