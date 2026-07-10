# 리뷰 이슈 — change-nickname

리뷰 범위 1차: `62c7dff..be6b821` (리팩토링 4커밋)

## 1차 차단 이슈

- [x] **[api/src/main/java/ksh/tryptobackend/user/domain/vo/Nickname.java / User.java:reconstitute] Nickname VO 의 미검증 생성 경로** (출처: oop) — **완료(`c7c95f6`, compact constructor 검증)**
  - **설명:** `Nickname` record 의 canonical 생성자가 public 이라 `new Nickname(value)` 로 길이 검증(`Nickname.of`)을 우회할 수 있다. 실제로 `User.reconstitute` 가 `new Nickname(nickname)` 으로 미검증 생성 중이다. 이번 리팩토링이 도입한 VO 의 불변식(길이 2~20)이 모든 생성 경로에서 보장되지 않는다. trading `Order.reconstitute` 는 `Price.of(...)` 처럼 검증 팩토리를 재사용하는 패턴이다.
  - **수정 제안:** Nickname 을 미검증으로 만들 수 없게 한다. 권장: compact constructor 에 길이 검증을 두어 `new`·`of` 모두 검증되게 하거나, `Nickname.of` 를 reconstitute 에서도 재사용한다. (동작 보존 — DB 데이터는 규칙을 만족하므로 복원 시 검증해도 안전.)

## 1차 참고 이슈 (수정 안 함, 보고용)

- [Nickname.java / User.java] `changeNickname(String)`·`hasSameValueAs(String)` 가 원시 String 을 받음 — VO 로 승격했으니 경계에서 `Nickname` 을 받으면 개념 일관 (ddd)
- [NicknameUniquenessCheckerImpl] 새 도메인 서비스 구현체 단위 테스트 부재 — 추가 권장 (oop)
- [ChangeNicknameService:30-31] 상태 변경 후 유일성 검사 순서 — "검증 후 반영" 순서가 더 읽기 쉬움. 롤백으로 안전성은 동일 (oop)

## 판정 메모

- ddd 확인: 닉네임 **시스템 전역 유일성**은 단일 User 애그리거트가 보장 불가한 규칙 → 조회 포트에 의존하는 도메인 서비스(`NicknameUniquenessChecker`)로 표현한 것은 정당(과잉 설계 아님).
- 인수 테스트(user/change-nickname) 6 시나리오 통과 — 동작 보존 확인.

## 2차 재리뷰 (`be6b821..c7c95f6`)

- 차단 1건 적용(`c7c95f6`: Nickname compact constructor 검증으로 모든 생성 경로 강제). 재리뷰 5개 리뷰어 모두 차단 0건 — 해소 확인. 통과.
