# 리뷰 이슈 — end-round

리뷰 범위 1차: `4778faf..87f4256` (리팩토링 1커밋)

## 1차 차단 이슈

없음 — 5개 리뷰어 모두 차단 0건. end-round 는 이미 표준에 가까워, `end()` 의 원시 enum 비교(`status == RoundStatus.ENDED`)를 `isEnded()`/`isActive()` 판별 메소드로 표현하는 소규모 정리로 완결. 멱등 전이·소유권·@Version 낙관락 동작 보존(인수 테스트 5 시나리오 통과).

## 1차 참고 이슈 (수정 안 함, 보고용)

- [InvestmentRound.java:116 chargeEmergencyFunding] emergency-funding 코드가 여전히 원시 비교 `status == RoundStatus.ACTIVE` — `isActive()` 재사용 여지. 이번 diff 밖(emergency-funding 완료 기능)이라 미적용 (ddd/oop)

## 1차 판정 요약

- 유효 차단 0건 — 통과.
