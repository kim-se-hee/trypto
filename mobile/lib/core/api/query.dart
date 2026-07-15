/// 쿼리 파라미터 정제(사양서 §1.11.6).
///
/// `null`·빈 문자열은 **키 자체를 생략**한다. 서버가 `size=` 같은 빈 값을 받으면
/// 400 `TYPE_MISMATCH` 를 낸다.
///
/// 요청에 `userId` 를 넣지 않는다(사양서 R4-7). 서버는 `@LoginUser` 로 세션에서 얻으며
/// 클라이언트가 보낸 값을 신뢰하지도, 사용하지도 않는다.
Map<String, dynamic> query(Map<String, dynamic> params) {
  return {...params}..removeWhere((_, value) => value == null || value == '');
}
