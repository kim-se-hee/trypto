RegretReportJob 상세. 일배치 전체 실행 흐름은 [snapshot-aggregation.md](../batch/snapshot-aggregation.md) 참조.

## 목적

ACTIVE 라운드의 복기 리포트를 생성/갱신한다. 계산이 무거우므로 배치로 미리 계산한다.

## 선행 조건

SnapshotJob 완료

## 실행 주기

SnapshotJob 완료 직후

## Step 구조

| Step | Reader | Processor | Writer |
|------|--------|-----------|--------|
| reportStep | ACTIVE 라운드의 위반 기록 + 주문 이력 | 위반분 우선 매칭, loss 계산, 시나리오 생성 | REGRET_REPORT + RULE_IMPACT + VIOLATION_DETAIL 저장 |

## 처리 절차

1. ACTIVE 라운드의 거래소별로:
   - 투자 원칙, 위반 기록, 주문 체결 이력을 조회한다
   - 위반분 우선 매칭으로 `loss_amount`를 계산한다 ([business-rules.md](business-rules.md) 참조)
   - 규칙별 시나리오를 생성한다 (`impactGap` 계산)
   - 최신 스냅샷에서 `missedProfit`을 계산한다
2. `REGRET_REPORT` + `RULE_IMPACT` + `VIOLATION_DETAIL`를 upsert한다

## 갱신 정책

- ACTIVE 라운드: 매일 갱신 (새로운 위반이 추가될 수 있으므로)
- ENDED 라운드: 종료 시 1회 확정 생성 후 변하지 않음
- 리포트가 없으면 (라운드 시작 당일) API 조회 시 REPORT_NOT_FOUND 에러를 반환한다. 배치 실행 후 조회 가능하다
