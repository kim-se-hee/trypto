# 리뷰 이슈 — change-portfolio-visibility

리뷰 범위 1차: `7da2ea1..29abea7` (리팩토링 2커밋: 애그리거트 save 일원화 + 응답 userId 포함[plan.md 계약 정정])

## 1차 차단 이슈

- [x] **[api/.../user/adapter/out/UserCommandAdapter.java:save / UserJpaEntity] load-mutate-save 전환으로 인한 lost update — User 낙관적 락 부재** (출처: concurrency) — **완료(`41d9ef0`·`8a0b54e`, InvestmentRound 패턴의 @Version 낙관락 도입)**
  - **설명:** 기존 벌크 업데이트 포트(`updatePortfolioVisibility`)는 `portfolioPublic` 컬럼만 단독 UPDATE 라 동시 nickname 변경과 무간섭이었다. 애그리거트 save 로 일원화하면서 `save` 가 stale 스냅샷의 전체 필드(nickname+portfolioPublic)를 덮어써, change-portfolio-visibility 와 change-nickname 이 같은 유저에 동시 실행되면 나중 커밋이 상대 변경을 되돌리는 lost update 가 발생한다. (DDD 관점의 애그리거트 save 방향 자체는 올바르며, 누락된 것은 낙관적 락이다.)
  - **수정 제안:** InvestmentRound 의 확립된 낙관적 락 패턴을 User 에 적용한다 — `UserJpaEntity` 에 `@Version` 컬럼 추가, `User` 도메인이 version 을 保持(reconstitute/toDomain 으로 왕복), `UserCommandAdapter.save` 를 `saveAndFlush` + 도메인 version 반영으로 바꿔 동시 수정 시 `OptimisticLockException` 으로 감지되게 한다. (`ddl-auto: create` 라 스키마 마이그레이션 불필요.)

## 1차 참고 이슈 (수정 안 함, 보고용)

- [ChangePortfolioVisibilityService / UserCommandAdapter.save] service.findById + save 내부 findById 로 같은 유저를 두 번 조회 — 동일 트랜잭션 1차 캐시로 실제 SELECT 는 1회. 어댑터가 별도 트랜잭션으로 분리되면 N+1 가능 (performance)
- [UserQueryPort.findById().orElseThrow] 반드시 존재해야 하는 대상이므로 `getById()` 네이밍이 더 부합 — user 모듈 전반 공통이라 모듈 차원 별도 이슈 (convention)

## 판정 메모

- 응답 userId 추가는 plan.md Response(`data.userId`) 계약 정정 — 인수 테스트(3 시나리오) 통과로 확인.
- ddd/oop/convention 모두 벌크 포트 제거 → 애그리거트 save 일원화를 호평(애그리거트 경유 변경 원칙 부합).

## 2차 재리뷰 (`29abea7..be65444`)

- 차단 1건 적용(`41d9ef0` 엔티티/도메인 @Version, `8a0b54e` saveAndFlush, `be65444` 테스트 픽스처). 재리뷰 4개 리뷰어 모두 차단 0건 — concurrency 가 lost update 방지 확인(동일 트랜잭션 identity map + saveAndFlush + @Version). 통과.
- 참고(비차단): 도메인 `version` 이 쓰기 경로에서 직접 소비되지 않고 관리 엔티티 버전으로 락이 동작 — InvestmentRound 와 동일한 확립된 트레이드오프라 수용. change-nickname·change-portfolio-visibility 인수 테스트 통과.
