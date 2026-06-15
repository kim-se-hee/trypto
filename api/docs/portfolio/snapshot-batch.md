SnapshotJob 상세. 일배치 전체 실행 흐름은 [snapshot-aggregation.md](../batch/snapshot-aggregation.md) 참조.

## 목적

ACTIVE 라운드의 거래소별 일별 자산 상태를 캡처한다. 이 스냅샷은 랭킹, 랭커 포트폴리오, 후회 그래프, 후회 리포트의 기반 데이터다.

## 실행 주기

매일 23:59 KST

## Step 구조

| Step | Reader | Processor | Writer |
|------|--------|-----------|--------|
| snapshotStep | ACTIVE 라운드의 거래소별 지갑 | 잔고 + 보유코인 x Redis 현재가 = 거래소별 총 자산 | PORTFOLIO_SNAPSHOT + DETAIL 저장 |

## 처리 절차

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

## 저장 데이터

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

## 스냅샷 소비 방식

| 기능 | 조회 방식 |
|------|----------|
| 랭킹 | `SUM(total_asset_krw) GROUP BY user_id` — 전 거래소 KRW 합산 |
| 랭커 포트폴리오 | 최신 snapshot_date의 DETAIL — 코인별 상세 |
| 후회 그래프 | `WHERE exchange_id = ? ORDER BY snapshot_date` — 거래소별 일별 시계열 |
| 후회 리포트 | `WHERE exchange_id = ? ORDER BY snapshot_date DESC LIMIT 1` — 거래소별 마지막 자산 |

## ENDED 라운드 처리

- 라운드 종료 시 마지막 스냅샷을 1회 생성한다
- 종료된 라운드의 기존 스냅샷은 보존한다 (과거 복기 그래프 조회용)
- 이후 배치에서 ENDED 라운드는 스냅샷을 생성하지 않는다

## 바이낸스 USDT → KRW 환산

- `total_asset_krw` 계산 시 바이낸스 USDT 자산을 KRW로 환산한다
- 고정 환율 `1 USDT = 1,400 KRW`로 계산한다
