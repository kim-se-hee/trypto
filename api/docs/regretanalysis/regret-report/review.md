# 리뷰 이슈 — regret-report

리뷰 범위 1차: `4ba06ec..5caaf37` (리팩토링 4커밋)

## 1차 차단 이슈

없음 — 5개 리뷰어(ddd·oop·concurrency·performance·convention) 모두 차단 0건.

핵심 개선: investmentround·wallet·marketdata **3개 컨텍스트 협력을 ACL 어댑터**(`AclInvestmentRoundQueryAdapter`·`AclMarketDataQueryAdapter`·`AclWalletQueryAdapter`, 모두 `@Component("regretanalysisAcl...")` 접두 빈 이름으로 충돌 선제 방지)로 분리(직접 주입 제거), 소유권 검증을 `AnalysisRound.validateOwnedBy` 도메인으로 이동, 기축통화 판정(domestic→KRW/USDT)을 ACL 어댑터 번역으로. 심볼 벌크 조회 유지(N+1 없음, 조회 6회 동일). 인수 테스트(regret-report) 4 시나리오 통과.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [GetRegretReportService existsWallet] 지갑 존재 판정이 서비스 인라인 if-throw — 소유권을 도메인으로 옮긴 것과 스타일 비대칭. 단 `GetRegretChartService`/`FindExchangeCoinsService` 에 이미 동일 패턴(pre-existing)이라 이번 diff 위반 아님 (ddd/oop/convention)
- [아웃 포트 명명] 연동 대상/CRUD 노출(`getRound`/`getExchange`) — 조회 전용 포트라 영향 작음, 코드베이스 관례 (ddd)

## 1차 판정 요약

- 유효 차단 0건 — 통과. RegretReport 애그리거트군 읽기 형태를 ACL 표준으로 확립(regret-chart 가 재사용).
