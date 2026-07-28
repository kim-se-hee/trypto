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
| reportStep | ACTIVE 라운드의 위반 기록 + 주문 이력 | 위반분 우선 매칭, loss 계산(실현분/미실현분 분리), 규칙별 손실 집계 | REGRET_REPORT + RULE_IMPACT + VIOLATION_DETAIL + VIOLATION_REALIZATION 저장 |

## 처리 절차

1. ACTIVE 라운드의 거래소별로:
   - 최신 스냅샷을 조회한다. 스냅샷이 없으면 해당 건은 생성하지 않고 건너뛴다
   - 투자 원칙, 위반 기록, 주문 체결 이력을 조회한다
   - 위반분 우선 매칭으로 `loss_amount`를 계산한다 ([business-rules.md](business-rules.md) 참조)
   - 매칭 결과에 따라 위반 손익을 실현분과 미실현분으로 분리하고, 실현분은 매칭된 매도의 체결일 기준으로 실현일별 금액을 `VIOLATION_REALIZATION`에 남긴다. 부분 실현이 여러 날에 걸치면 실현일도 여러 개다. 미실현분은 `loss_amount`에서 실현분 합을 뺀 값이다
   - 규칙별 손실을 집계한다 (원칙별 위반 횟수·총 위반 손실)
   - 위반 손실 합에서 `totalViolationLoss`를, 최신 스냅샷의 총 자산에서 `actualAsset`을 구한다. 원칙 준수 시 자산은 라운드 단위 원화 자산과 누적 배수로만 정의되므로 거래소 단위 리포트에서는 산출하지 않는다 (조회 시점의 몫)
2. `REGRET_REPORT` + `RULE_IMPACT` + `VIOLATION_DETAIL` + `VIOLATION_REALIZATION`을 upsert한다

금액은 해당 거래소의 기축통화 단위(국내: KRW, 바이낸스: USDT)로 저장한다. 원화 환산과 라운드 단위 합산은 조회 시점의 몫이다.

## 생성 조건

리포트 생성 여부는 위반 유무가 아니라 **스냅샷 유무**로 결정한다.

- 위반이 0건이어도 스냅샷이 있으면 리포트를 생성한다. 이때 `totalViolations`, `totalViolationLoss`는 0이고, `RULE_IMPACT`와 `VIOLATION_DETAIL`은 빈 목록이다. 원칙을 모두 준수한 사용자도 복기 화면에서 자산 추이와 BTC 벤치마크를 확인할 수 있어야 하기 때문이다
- 스냅샷이 없으면 리포트를 생성하지 않는다. 자산 추이를 그릴 근거가 없기 때문이다

## 갱신 정책

- ACTIVE 라운드: 매일 갱신 (새로운 위반이 추가될 수 있으므로)
- ENDED 라운드: 배치 대상에서 제외한다. 종료 전 마지막 배치가 만든 리포트가 그대로 남는다
- 리포트가 없으면 (라운드 시작 당일, 스냅샷 미생성) API는 에러가 아니라 빈 리포트를 반환한다. 배치 실행 후 실제 집계 결과로 채워진다
