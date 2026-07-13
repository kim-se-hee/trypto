# 리뷰 사항

## 1차 차단 이슈

- [x] **[api/src/main/java/ksh/tryptobackend/user/adapter/in/dto/request/SendFeedbackRequest.java:6] 공백만인 본문 검증이 두 계층에 분산되어 문서화된 오류 코드와 실제 동작이 어긋난다** (출처: oop)
  - **설명:** spec.md 는 "공백만으로 채운 본문은 빈 본문으로 본다" 를, plan.md 는 이를 `INVALID_FEEDBACK_LENGTH(400)` 로 응답하도록 명시한다. `FeedbackContent.of()` 가 `strip()` 후 길이를 검사해 이 규칙을 구현하고 있으나, `SendFeedbackRequest.content` 에 붙은 `@NotBlank` 가 공백 문자열을 컨트롤러 진입 전에 걸러 `MethodArgumentNotValidException` 을 던진다. 그 결과 `GlobalControllerAdvice` 가 `VALIDATION_ERROR` 로 응답하며, 도메인이 던지도록 구현한 `INVALID_FEEDBACK_LENGTH` 경로는 실제 요청 흐름에서 절대 실행되지 않는 죽은 코드가 된다. 인수 테스트의 "공백만 채운 본문이면 실패" 시나리오가 상태코드 400 만 검증하고 `code` 값은 검증하지 않아 이 불일치가 걸러지지 않는다.
  - **수정 제안:** `SendFeedbackRequest.content` 의 `@NotBlank` 를 `@NotNull` 로 완화한다. `null` 은 여전히 컨트롤러 진입 전에 걸러지고, 공백만인 본문과 짧은 본문은 모두 `FeedbackContent` 가 스펙대로 `INVALID_FEEDBACK_LENGTH` 로 일관되게 처리한다. 아울러 `send-feedback.feature` 의 "공백만 채운 본문이면 실패" 시나리오에 `그리고 응답 코드는 "INVALID_FEEDBACK_LENGTH"이다` 를 추가해 회귀를 막는다.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [api/src/main/java/ksh/tryptobackend/user/domain/vo/FeedbackContent.java:11-19] 정규화가 정적 팩토리에만 있어 VO 불변식이 생성 경로에 따라 갈린다 — canonical 생성자로 직접 만들면 공백 포함 길이로 검증을 통과할 수 있다 (출처: ddd)
- [api/src/main/java/ksh/tryptobackend/user/adapter/out/persistence/entity/FeedbackJpaEntity.java:21] 본문 최대 길이 규칙(1000자)이 도메인 VO 와 JPA 엔티티에 중복 정의되어 정책 변경 시 어긋날 여지가 있다 (출처: ddd, oop)
- [api/src/main/java/ksh/tryptobackend/common/dto/response/ApiResponseDto.java:18-20] `createdSuccess` 는 기존 범용 팩토리 `of()` 로 대체 가능하며, 공용 DTO 의 API 표면을 늘린다 (출처: oop)
- [api/src/main/java/ksh/tryptobackend/user/application/port/in/SendFeedbackUseCase.java:8] `sendFeedback` 의 반환값(`Feedback`)이 컨트롤러에서 사용되지 않는다 (출처: oop)
- [api/src/test/java/ksh/tryptobackend/acceptance/steps/user/SendFeedbackStepDefinition.java:23-33] 조사(라는/이라는)만 다른 스텝 3 개가 동일한 본문을 중복한다 (출처: oop)
- [api/src/test/java/ksh/tryptobackend/acceptance/testclient/CommonApiClient.java:34] `getLoggedInUserId()` 에 Javadoc 주석이 붙었다 — 컨벤션은 주석을 금하나 같은 파일의 기존 관례를 따른 것이다 (출처: 컨벤션)

## 2차 차단 이슈

없음. 1차 차단 이슈 수정분(`25c19192..HEAD`)을 다섯 리뷰어가 재검토한 결과 차단 이슈 0 건으로 통과했다.

## 2차 참고 이슈 (수정 안 함, 보고용)

- [api/docs/user/send-feedback/plan.md:17] Request Body 표의 `content` 검증이 여전히 `@NotBlank` 로 적혀 있어 코드(`@NotNull` + 도메인 검증)와 어긋난다 (출처: ddd, 컨벤션)
