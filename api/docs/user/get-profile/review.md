# 리뷰 이슈 — get-profile

리뷰 범위 1차: `f558928..d91d492` (리팩토링 2커밋)

## 1차 차단 이슈

없음 — 5개 리뷰어(ddd·oop·concurrency·performance·convention) 모두 차단 0건·참고 0건. 코드 변경 없이 통과.

개선점: `toResult` private 매핑 메소드를 `UserPublicInfoResult.from(User)` 정적 팩토리로 이동(애플리케이션 서비스 private 메소드 금지 규칙 부합), 조회 서비스에 `@Transactional(readOnly = true)` 부여로 트랜잭션 경계 일관성 확보. 크로스컨텍스트 제공 `FindUserPublicInfoUseCase` 는 도메인(User) 대신 `UserPublicInfoResult` DTO 반환 — 애그리거트 유출 방지 규칙 준수.

## 1차 판정 요약

- 유효 차단 0건 — 통과. get-profile 은 이미 표준에 가까워 소규모 정리로 완결.
