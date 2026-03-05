# 포트폴리오 스냅샷 배치

## 개요

매일 ACTIVE 라운드의 거래소별 자산 상태를 스냅샷으로 저장한다. 이 스냅샷은 랭킹, 랭커 포트폴리오, 후회 그래프, 후회 리포트의 기반 데이터다.

## 전체 구조

```
23:59 KST
    SnapshotJob (스냅샷 생성)
        │
        ├─ RegretReportJob (리포트 갱신) ─┐
        │                                 ├─ 병렬 실행
        └─ RankingJob (순위 매기기) ──────┘
```

- SnapshotJob이 먼저 완료된 후, RegretReportJob과 RankingJob이 병렬로 실행된다
- RegretReportJob과 RankingJob은 서로 의존하지 않는다

---

## Job 1: SnapshotJob

### 목적

ACTIVE 라운드의 거래소별 일별 자산 상태를 캡처한다.

### 실행 주기

매일 23:59 KST

### Step 구조

| Step | Reader | Processor | Writer |
|------|--------|-----------|--------|
| snapshotStep | ACTIVE 라운드의 거래소별 지갑 | 잔고 + 보유코인 x Redis 현재가 = 거래소별 총 자산 | PORTFOLIO_SNAPSHOT + DETAIL 저장 |

### 처리 절차

1. `INVESTMENT_ROUND`에서 `status = ACTIVE`인 모든 라운드를 조회한다
2. 각 라운드의 `WALLET`에서 거래소별 지갑을 조회한다
3. 각 지갑별로:
   - `WALLET_BALANCE`에서 기축통화(KRW/USDT) 잔고를 조회한다
   - `HOLDING`에서 보유 코인 목록을 조회한다
   - Redis에서 각 코인의 현재가를 조회한다
   - 거래소별 총 자산 = 기축통화 잔고 + SUM(보유수량 x 현재가)
   - KRW 환산: 국내 거래소는 그대로, 바이낸스는 USDT x 1,400
   - 총 투입금 = 해당 거래소 시드머니 + 해당 거래소 긴급 자금 합계
   - 수익률 = (총 자산 - 총 투입금) / 총 투입금 x 100
4. `PORTFOLIO_SNAPSHOT` + `PORTFOLIO_SNAPSHOT_DETAIL` 적재

### 저장 데이터

**PORTFOLIO_SNAPSHOT** (거래소별 1행)

| 필드 | 타입 | 설명 |
|------|------|------|
| snapshot_id | Long (PK) | 주 식별자 |
| user_id | Long (FK) | 유저 ID |
| round_id | Long (FK) | 라운드 ID |
| exchange_id | Long (FK) | 거래소 ID |
| total_asset | BigDecimal | 해당 거래소 기축통화 단위 총 자산 |
| total_asset_krw | BigDecimal | KRW 환산 총 자산 (랭킹 집계용) |
| total_investment | BigDecimal | 해당 거래소 총 투입금 (기축통화 단위) |
| total_profit | BigDecimal | 수익금 (기축통화 단위) |
| total_profit_rate | BigDecimal | 수익률 (%) |
| snapshot_date | LocalDate | 스냅샷 날짜 |

**PORTFOLIO_SNAPSHOT_DETAIL** (코인별 1행)

| 필드 | 타입 | 설명 |
|------|------|------|
| detail_id | Long (PK) | 주 식별자 |
| snapshot_id | Long (FK) | 스냅샷 ID |
| coin_id | Long (FK) | 코인 ID |
| quantity | BigDecimal | 보유 수량 |
| avg_buy_price | BigDecimal | 평균 매수가 |
| current_price | BigDecimal | 스냅샷 시점 현재가 |
| profit_rate | BigDecimal | 코인별 수익률 |
| asset_ratio | BigDecimal | 자산 비율 (%) |

> `PORTFOLIO_SNAPSHOT_DETAIL`에서 기존의 `exchange_id`는 제거한다. 상위 스냅샷이 이미 거래소별로 분리되어 있으므로 불필요하다.

### 스냅샷 소비 방식

| 기능 | 조회 방식 |
|------|----------|
| 랭킹 | `SUM(total_asset_krw) GROUP BY user_id` — 전 거래소 KRW 합산 |
| 랭커 포트폴리오 | 최신 snapshot_date의 DETAIL — 코인별 상세 |
| 후회 그래프 | `WHERE exchange_id = ? ORDER BY snapshot_date` — 거래소별 일별 시계열 |
| 후회 리포트 | `WHERE exchange_id = ? ORDER BY snapshot_date DESC LIMIT 1` — 거래소별 마지막 자산 |

---

## Job 2: RegretReportJob

### 목적

ACTIVE 라운드의 복기 리포트를 생성/갱신한다. 계산이 무거우므로 배치로 미리 계산한다.

### 선행 조건

SnapshotJob 완료

### 실행 주기

SnapshotJob 완료 직후

### Step 구조

| Step | Reader | Processor | Writer |
|------|--------|-----------|--------|
| reportStep | ACTIVE 라운드의 위반 기록 + 주문 이력 | 위반분 우선 매칭, loss 계산, 시나리오 생성 | REGRET_REPORT + RULE_IMPACT + VIOLATION_DETAIL 저장 |

### 처리 절차

1. ACTIVE 라운드의 거래소별로:
   - 투자 원칙, 위반 기록, 주문 체결 이력을 조회한다
   - 위반분 우선 매칭으로 `loss_amount`를 계산한다 ([business-rules.md](../regretanalysis/business-rules.md) 참조)
   - 규칙별 시나리오를 생성한다 (`impactGap` 계산)
   - 최신 스냅샷에서 `missedProfit`을 계산한다
2. `REGRET_REPORT` + `RULE_IMPACT` + `VIOLATION_DETAIL`를 upsert한다

### 갱신 정책

- ACTIVE 라운드: 매일 갱신 (새로운 위반이 추가될 수 있으므로)
- ENDED 라운드: 종료 시 1회 확정 생성 후 변하지 않음
- 리포트가 없으면 (라운드 시작 당일) API 조회 시 REPORT_NOT_FOUND 에러를 반환한다. 배치 실행 후 조회 가능하다

---

## Job 3: RankingJob

### 목적

기간별(일간/주간/월간) 수익률 랭킹을 집계한다.

### 선행 조건

SnapshotJob 완료

### 실행 주기

매일 (모든 기간 매일 계산). period는 실행 주기가 아니라 **수익률 산출 기간(window)**을 의미한다.

### Step 구조

| Step | Reader | Processor | Writer |
|------|--------|-----------|--------|
| rankingStep | 오늘 스냅샷 + N일 전 스냅샷의 거래소별 total_asset_krw | 유저별 SUM + 자격 검증 + 윈도우 수익률 계산 + 정렬 | RANKING 저장 (기간별) |

### 처리 절차

1. 참여 자격 필터링:
   - `status = ACTIVE`인 라운드
   - `started_at`이 24시간 이전
   - 최소 1건의 FILLED 주문 존재
2. 오늘 스냅샷 summary map 생성 (유저별 전 거래소 KRW 합산)
3. 라운드별 거래 횟수를 1회 집계하여 캐싱
4. 각 기간(DAILY/WEEKLY/MONTHLY)에 대해:
   - N일 전 스냅샷 summary map 생성 (DAILY=1일, WEEKLY=7일, MONTHLY=30일)
   - 오늘과 N일 전 스냅샷이 **모두 있는** 유저만 후보로 선정
   - 수익률 = (오늘 자산 - N일 전 자산) / N일 전 자산 × 100
   - N일 전 스냅샷이 없는 유저(라운드 시작 직후 등)는 해당 기간 랭킹에서 제외
   - 동률 처리 기준에 따라 고유 순위 부여 ([ranking-list.md](../ranking/ranking-list.md) 참조)
   - `RANKING` 테이블 적재

---

## ENDED 라운드 처리

- 라운드 종료 시 마지막 스냅샷을 1회 생성한다
- 종료된 라운드의 기존 스냅샷은 보존한다 (과거 복기 그래프 조회용)
- 이후 배치에서 ENDED 라운드는 스냅샷을 생성하지 않는다

## 바이낸스 USDT → KRW 환산

- `total_asset_krw` 계산 시 바이낸스 USDT 자산을 KRW로 환산한다
- 고정 환율 `1 USDT = 1,400 KRW`로 계산한다
