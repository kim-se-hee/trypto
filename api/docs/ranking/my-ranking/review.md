# 리뷰 이슈 — my-ranking

리뷰 범위 1차: `9633d67..f7b4b55` (리팩토링 1커밋)

## 1차 차단 이슈

없음 — 5개 리뷰어 모두 차단 0건. `GetMyRankingService` 가 user `FindUserPublicInfoUseCase` 직접 주입 → ranking-list 가 확립한 **`UserQueryPort`+`AclUserQueryAdapter` 재사용**(중복 생성 없음), 닉네임 조립을 `MyRankingResult.of(RankingSummary, UserProfiles)` 정적 팩토리로. 조회 쿼리 1회 유지. 인수 테스트(my-ranking) 4 시나리오 통과(참여/미참여 null/기간없음 null/잘못된 period 400).

## 1차 참고 이슈 (수정 안 함, 보고용)

- [GetMyRankingService] 단건 조회에 배치 포트 `findByUserIds(Set.of(userId))` 재사용 — 쿼리 1회로 동일하나 단건 오버로드 고려 여지 (oop)

## 1차 판정 요약

- 유효 차단 0건 — 통과.
