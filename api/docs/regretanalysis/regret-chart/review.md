# 리뷰 이슈 — regret-chart

리뷰 범위 1차: `7335605..3b5936e` (리팩토링 5커밋)

## 1차 차단 이슈

- [x] **[api/.../regretanalysis/adapter/out/acl/AclMarketDataQueryAdapter.java:56] private 메소드 나열 순서 위반** (출처: 컨벤션) — **완료(`6ff4718`)**
  - **설명:** `toBtcDailyPrice`(3번째 public `findBtcDailyPrices` 에서 사용)가 `toAnalysisExchange`(1번째 public `getExchange` 에서 사용)보다 먼저 온다. conventions.md "private 메서드는 사용된 순서대로 나열" 위반.
  - **수정 제안:** `toAnalysisExchange` 를 위로, `toBtcDailyPrice` 를 아래로 이동.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [RegretChartResult.from] 회한 분석 계산 조립(`CumulativeLossTimeline`/`BtcBenchmark`/`ViolationMarkers`)이 응용 DTO 팩토리에 위치 — 직전엔 서비스에 있던 것을 이동한 것(새 문제 아님), 도메인 조립 서비스로 뺄 여지 (ddd). from() 21줄 살짝 초과 (oop)
- [RegretReportJpaEntity.toDomain] 미사용 `ruleImpacts` 지연 로딩 — 리포트당 1건이라 영향 미미 (performance)
- [위반 상세 조회] 단일 QueryDSL 조인 → 애그리거트 지연로딩 2~3쿼리로 변화 — 건수 소량이라 미미. 단 concurrency 는 exists+find 통합을 정합성 개선으로 호평 (performance/concurrency)
- [MarketDataQueryPort.findBtcDailyPrices] 데이터 지향 네이밍 — 조회 전용 포트라 영향 작음 (ddd)
- [AssetTimeline(domain/vo)→AssetSnapshot(domain/model)] pre-existing vo→model 소지 — 이번 diff 밖 (convention 비고)

## 판정 메모

- ACL: regret-report ACL 재사용 + `AclPortfolioQueryAdapter` 신규(접두 빈 이름), 직접 주입 제거, 소유권 `AnalysisRound` 재사용. performance: BTC/스냅샷 기간 벌크 1회 유지, exists 별도 쿼리 제거로 개선. concurrency: 읽기 전용 단일 트랜잭션, 2단계→1단계 통합 정합성 개선.
- 인수 테스트(regret-chart) 5 시나리오 통과.

## 2차 재리뷰 (`3b5936e..6ff4718`)

- 차단 1건 적용(`6ff4718`: private 메소드 순서를 사용 순서에 맞춤). 재리뷰 convention 차단 0건 — 규칙 충족 확인. 통과.
