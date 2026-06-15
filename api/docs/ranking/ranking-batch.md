RankingJob 상세. 일배치 전체 실행 흐름은 [snapshot-aggregation.md](../batch/snapshot-aggregation.md) 참조.

## 목적

기간별(일간/주간/월간) 수익률 랭킹을 집계한다.

## 선행 조건

SnapshotJob 완료

## 실행 주기

매일 (모든 기간 매일 계산). period는 실행 주기가 아니라 **수익률 산출 기간(window)**을 의미한다.

## Step 구조

| Step | Reader | Processor | Writer |
|------|--------|-----------|--------|
| rankingStep | 오늘 스냅샷 + N일 전 스냅샷의 거래소별 total_asset_krw | 유저별 SUM + 자격 검증 + 윈도우 수익률 계산 + 정렬 | RANKING 저장 (기간별) |

## 처리 절차

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
   - 동률 처리 기준에 따라 고유 순위 부여 ([ranking-list/](ranking-list/index.md) 참조)
   - `RANKING` 테이블 적재
