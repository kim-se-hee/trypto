/// 모든 REST 응답을 감싸는 공통 봉투(사양서 §1.2).
///
/// `data` 는 엔드포인트마다 타입이 달라 제네릭 코드 생성이 어색하다. 봉투를 읽는 곳은
/// 인터셉터와 예외 두 곳뿐이므로 손으로 파싱한다.
class ApiEnvelope {
  const ApiEnvelope({this.status, this.code, this.message, this.data});

  final int? status;
  final String? code;
  final String? message;
  final Object? data;

  /// 201 이 `CREATED` 로도 `SUCCESS` 로도 온다(`POST /api/feedbacks` 만 후자).
  static const Set<String> successCodes = {'SUCCESS', 'CREATED'};

  bool get isSuccess => code != null && successCodes.contains(code);

  /// 봉투가 아니면 null. 호출부가 '비봉투 응답' 을 구분할 수 있어야 한다.
  static ApiEnvelope? tryParse(Object? body) {
    if (body is! Map) return null;
    final code = body['code'];
    if (code is! String) return null;
    final status = body['status'];
    final message = body['message'];
    return ApiEnvelope(
      status: status is num ? status.toInt() : null,
      code: code,
      message: message is String ? message : null,
      data: body['data'],
    );
  }
}
