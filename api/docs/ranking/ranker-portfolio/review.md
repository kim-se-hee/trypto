# 리뷰 이슈 — ranker-portfolio

리뷰 범위 1차: `26e3ad3..82bee9d` (리팩토링 7커밋 + 빈 이름 충돌 fix 1커밋)

## 1차 차단 이슈

없음 — 5개 리뷰어(ddd·oop·concurrency·performance·convention) 모두 차단 0건.

핵심 개선: user·portfolio·marketdata·investmentround **4개 컨텍스트 협력을 ACL 어댑터**(`AclUserQueryAdapter` 재사용 + `AclPortfolioQueryAdapter`·`AclMarketDataQueryAdapter`·`AclInvestmentRoundQueryAdapter` 신규)로 분리(팬아웃 최대 기능의 직접 주입 5개 전부 제거), 열람 판정(상위100 `RankingSummary.assertViewable`·공개여부 `UserProfile.assertPortfolioPublic`)·보유자산(`Holdings` 일급 컬렉션)을 도메인으로 이동. 심볼/거래소명 벌크 조회 유지(N+1 없음, 조회 7회 동일). 빈 이름 충돌은 `rankingAcl...` 접두로 해소(러너, `82bee9d`) — convention 리뷰어가 4그룹 전수 확인. 인수 테스트(ranker-portfolio) 3건 실패→해소→전건 통과.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [GetRankerPortfolioService] `viewer` 변수/파라미터명이 의미상 반대 — `query.userId()` 는 열람 *대상*(랭커)이므로 `ranker`/`portfolioOwner` 로 (ddd/oop)
- [RankingSummary.assertViewable / UserProfile.assertPortfolioPublic] `assert*` 가 코드베이스 유일 — 다른 도메인 판정 메소드는 `validate*` 관례. conventions.md 명문 규칙은 아니라 참고 (oop)
- [PortfolioQueryPort.findLatestHoldings(userId, roundId)] 원시값 2개 데이터 클럼프 — 같은 패키지 `RoundKey` VO 재사용 고려 (oop)
- [아웃 포트 명명] 연동 대상 노출(`getActiveRoundId` 등) — 조회 전용 포트라 영향 작음 (ddd)

## 1차 판정 요약

- 유효 차단 0건 — 통과. Ranking 컨텍스트 완료(list·me·stats·ranker-portfolio). 팬아웃 최대 기능까지 ACL 표준화로 68 부채 대폭 감소.
