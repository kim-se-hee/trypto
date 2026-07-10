# 리뷰 이슈 — ranking-stats

리뷰 범위 1차: `38d0361..7c40e02` (리팩토링 4커밋 — DB 집계 → 도메인 집계 전환)

## 1차 차단 이슈 (하나의 근본 원인 — DB 집계로 복원하면 일괄 해소)

- [x] **[RankingQueryAdapter.findAllRankings / GetRankingStatsService / RankingSummaries.toStats] 집계를 DB(COUNT/MAX/AVG)에서 전체 행 로드 후 in-app 집계로 바꿔 성능 회귀 + Result DTO 미사용 + QueryDSL 2조건** (출처: performance·oop·convention) — **완료(`acb2847`, DB 집계 복원)**
  - **성능 회귀(performance):** `GET /api/rankings/stats` 는 인증 없는 공개·무캐시 엔드포인트인데, 매 요청마다 기준일 **전체 랭킹 행**을 로드해 in-app 집계한다. plan.md 는 "상위 100명 데이터라 실시간 집계 부담 없음" 으로 정당화하나, **RANKING 테이블은 top-100 으로 제한되지 않는다** — top-100 은 포트폴리오 열람 제한(business-rules)일 뿐, 적재는 참여 자격(활성 라운드+24h+체결1건)을 만족하는 전원이다(`CalculateRankingService`/`RankingCandidates.toRankings` 에 top-100 절단 없음). 즉 plan.md 전제가 사실과 달라 참여자 수 증가 시 DB→앱 전송·GC·순회 비용이 선형 증가하는 실질 회귀(200 TPS 목표 위협). 기존 `COUNT`/`MAX`/`AVG` 는 집계값 3개만 반환했다.
  - **Result DTO 미사용(oop):** `RankingStatsResult` 를 지우고 `GetRankingStatsUseCase` 가 도메인 VO `RankingStats` 를 컨트롤러까지 직접 반환 — "여러 Aggregate 조합 시 Result DTO 반환" 컨벤션 위반, `GetRankingsUseCase`(RankingCursorResult)와 불일치.
  - **QueryDSL 2조건(convention):** `findAllRankings` 가 등가조건 2개(period+referenceDate)뿐인데 QueryDSL — conventions.md "조건 2개 이하 단순 조회는 Spring Data JPA 쿼리 메소드".
  - **수정 제안:** 집계를 **DB 집계(COUNT/MAX/AVG)로 복원**한다(리팩토링 전 `getRankingStats` 어댑터 쿼리 방식). 전체 행 로드 포트(`findAllRankings`)와 in-app 집계(`RankingSummaries.toStats`)를 제거하고, `GetRankingStatsUseCase` 는 `RankingStatsResult` DTO 를 반환한다. **참여자 수는 전원 집계여야 하므로 LIMIT 100 은 의미가 깨져(count 가 100 에 캡) 부적절** — DB 집계가 정답. 서비스는 순수 오케스트레이션 유지.

## 1차 참고 이슈 (수정 안 함, 보고용)

- [RankingSummaries.avgProfitRate] 빈 리스트 시 0 나눗셈 위험 — 현재 흐름상 미도달이나 방어 고려. (DB 집계 복원 시 소멸) (ddd/convention)
- [plan.md] "상위 100명 데이터" 전제가 실제 데이터 규모와 불일치 — 문서 정정 필요(별도). (performance)

## 판정 메모

- concurrency: 읽기 전용 단일 트랜잭션, 이상 없음. 인수 테스트(ranking-stats) 2 시나리오 통과(단, 규모 성능은 미검증).
- ddd 는 도메인 집계 방향을 호평했으나, 이 read-model 집계는 성능(전원 집계·공개 엔드포인트)이 우선이라 place-order 지향의 효율성 관점에서 DB 집계가 맞다.

## 2차 재리뷰 (`7c40e02..acb2847`)

- 차단 1건 적용(`acb2847`: DB 집계 COUNT/MAX/AVG 복원, `findAllRankings`·in-app 집계 제거, `RankingStatsResult` DTO 반환). 재리뷰 performance·oop·convention 모두 차단 0건 — 전송 O(1) 회귀 해소·Result DTO 경계 회복·집계 QueryDSL 적절 확인. 통과.
- 미해결 문서 이슈(별도): plan.md "상위 100명 데이터" 전제가 실제(RANKING 은 참여자 전원 적재)와 불일치 — 문서 정정 권장.
