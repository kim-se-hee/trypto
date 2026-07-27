## API 명세

### 참고사항

- 배치가 거래소별로 생성한 리포트를 합쳐 라운드 단위로 조회한다. 리포트가 아직 없으면 (라운드 시작 당일 등) 에러가 아니라 빈 리포트를 반환한다. 요약 지표는 모두 0, `violationDetails`는 빈 배열이며, `ruleImpacts`는 라운드에 설정된 원칙을 위반 횟수·손실 0으로 채워 반환한다
- 라운드 단위로 요청한다. 거래소를 가려서 조회하지 않는다
- 요약 지표와 `ruleImpacts` 의 금액은 원화(KRW)다. 바이낸스(USDT) 몫은 고정 환율 **1 USDT = 1,400원**으로 환산해 합산한다
- `violationDetails` 의 금액은 발생 거래소의 기축통화와 원화를 함께 내려준다. 화면은 기축통화로 보여주고, 여러 거래소의 금액을 더하는 그래프 계산에는 원화를 쓴다

`GET /api/rounds/{roundId}/regret`

### Path Parameter

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| roundId | Long | O | 투자 라운드 ID |

### Request

```
GET /api/rounds/1/regret
```

### Response

```json
{
  "status": 200,
  "code": "OK",
  "message": "투자 복기 리포트를 조회했습니다.",
  "data": {
    "roundId": 1,
    "totalViolations": 4,
    "analysisStart": "2026-01-15",
    "analysisEnd": "2026-02-25",
    "totalViolationLoss": 715000,
    "actualAsset": 12000000,
    "ruleFollowedAsset": 12715000,

    "ruleImpacts": [
      {
        "ruleId": 3,
        "ruleType": "CHASE_BUY_BAN",
        "thresholdValue": 20,
        "thresholdUnit": "%",
        "violationCount": 2,
        "totalLossAmount": 245000
      },
      {
        "ruleId": 4,
        "ruleType": "AVERAGING_DOWN_LIMIT",
        "thresholdValue": 2,
        "thresholdUnit": "회",
        "violationCount": 1,
        "totalLossAmount": 120000
      },
      {
        "ruleId": 5,
        "ruleType": "OVERTRADING_LIMIT",
        "thresholdValue": 5,
        "thresholdUnit": "회",
        "violationCount": 1,
        "totalLossAmount": 350000
      },
      {
        "ruleId": 1,
        "ruleType": "LOSS_CUT",
        "thresholdValue": 10,
        "thresholdUnit": "%",
        "violationCount": 0,
        "totalLossAmount": 0
      }
    ],

    "violationDetails": [
      {
        "violationDetailId": 1,
        "orderId": 15,
        "exchangeId": 1,
        "exchangeName": "업비트",
        "currency": "KRW",
        "coinSymbol": "DOGE",
        "violatedRules": [
          { "ruleType": "CHASE_BUY_BAN", "lossAmount": 385000, "lossAmountKrw": 385000 }
        ],
        "totalLossAmount": 385000,
        "totalLossAmountKrw": 385000,
        "occurredAt": "2026-01-22T14:30:00"
      },
      {
        "violationDetailId": 2,
        "orderId": 18,
        "exchangeId": 3,
        "exchangeName": "바이낸스",
        "currency": "USDT",
        "coinSymbol": "SOL",
        "violatedRules": [
          { "ruleType": "CHASE_BUY_BAN", "lossAmount": -100, "lossAmountKrw": -140000 }
        ],
        "totalLossAmount": -100,
        "totalLossAmountKrw": -140000,
        "occurredAt": "2026-01-25T11:00:00"
      },
      {
        "violationDetailId": 3,
        "orderId": 22,
        "exchangeId": 1,
        "exchangeName": "업비트",
        "currency": "KRW",
        "coinSymbol": "SHIB",
        "violatedRules": [
          { "ruleType": "AVERAGING_DOWN_LIMIT", "lossAmount": 120000, "lossAmountKrw": 120000 },
          { "ruleType": "OVERTRADING_LIMIT", "lossAmount": 350000, "lossAmountKrw": 350000 }
        ],
        "totalLossAmount": 470000,
        "totalLossAmountKrw": 470000,
        "occurredAt": "2026-02-03T09:45:00"
      }
    ]
  }
}
```

### 응답 필드 상세

#### 최상위 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| roundId | Long | 투자 라운드 ID |
| totalViolations | Integer | 라운드 전체의 위반 건수. 한 주문이 두 원칙을 어기면 2건으로 센다. 따라서 `violationDetails` 의 행 수보다 클 수 있다 |
| analysisStart | LocalDate | 분석 시작일 (라운드 시작일) |
| analysisEnd | LocalDate | 분석 종료일. 거래소별 리포트 중 가장 늦은 스냅샷 날짜 |
| totalViolationLoss | BigDecimal | 위반 손실 합계 (원화). 양수면 원칙을 지켰다면 더 벌었을 금액, 음수면 어긴 쪽이 이득이었던 금액. 0으로 보정하지 않는다 |
| actualAsset | BigDecimal | 실제 자산 (원화). 마지막 스냅샷의 전 거래소 자산 합 |
| ruleFollowedAsset | BigDecimal | 모든 원칙 준수 시 도달했을 자산 (원화). `actualAsset + totalViolationLoss` |

#### ruleImpacts[]

라운드에 설정된 모든 원칙을 포함한다. 위반이 없는 원칙은 위반 횟수·손실 0으로 채운다.
거래소마다 남은 규칙별 손실 행을 원칙 기준으로 더한 결과이므로 행 식별자는 내려주지 않는다. 원칙은 `ruleId` 로 구분한다.

| 필드 | 타입 | 설명 |
|------|------|------|
| ruleId | Long | 투자 원칙 ID |
| ruleType | String | 원칙 유형 (`LOSS_CUT`, `PROFIT_TAKE`, `CHASE_BUY_BAN`, `AVERAGING_DOWN_LIMIT`, `OVERTRADING_LIMIT`) |
| thresholdValue | BigDecimal | 설정된 기준값 |
| thresholdUnit | String | 기준값 단위. 원칙 유형에 따라 정해진다 (아래 표) |
| violationCount | Integer | 라운드 전체에서 해당 원칙을 어긴 횟수 |
| totalLossAmount | BigDecimal | 해당 원칙 위반으로 잃은 총 금액 (원화. 양수: 손실, 음수: 오히려 이익) |

기준값 단위는 원칙 유형이 결정한다.

| 원칙 유형 | 단위 |
|---|---|
| LOSS_CUT | % |
| PROFIT_TAKE | % |
| CHASE_BUY_BAN | % |
| AVERAGING_DOWN_LIMIT | 회 |
| OVERTRADING_LIMIT | 회 |

#### violationDetails[]

한 행은 주문 하나다. 한 주문이 여러 원칙을 어겼다면 행을 나누지 않고 `violatedRules` 에 나열한다.

| 필드 | 타입 | 설명 |
|------|------|------|
| violationDetailId | Long | 위반 거래 ID |
| orderId | Long (nullable) | 주문 ID. 주문 시점 위반(추격매수, 물타기, 과매매)은 해당 주문 ID. 가격 모니터링 위반(손절, 익절)은 null |
| exchangeId | Long | 위반이 발생한 거래소 ID |
| exchangeName | String | 위반이 발생한 거래소 이름 |
| currency | String | 해당 거래소의 기축통화 (KRW, USDT) |
| coinSymbol | String | 코인 심볼 (예: BTC, ETH, DOGE) |
| violatedRules | Object[] | 이 주문이 어긴 원칙과 원칙별 위반 손실 |
| totalLossAmount | BigDecimal | 이 행의 위반 손실 합계 (기축통화 단위). `violatedRules[].lossAmount` 의 합 |
| totalLossAmountKrw | BigDecimal | 이 행의 위반 손실 합계 (원화) |
| occurredAt | LocalDateTime | 위반 발생 시각. 주문 시점 위반은 체결 시각, 가격 모니터링 위반은 감지 시각 |

#### violationDetails[].violatedRules[]

| 필드 | 타입 | 설명 |
|------|------|------|
| ruleType | String | 원칙 유형 (`LOSS_CUT`, `PROFIT_TAKE`, `CHASE_BUY_BAN`, `AVERAGING_DOWN_LIMIT`, `OVERTRADING_LIMIT`) |
| lossAmount | BigDecimal | 이 주문에서 해당 원칙으로 발생한 위반 손실 (기축통화 단위. 양수: 손실, 음수: 오히려 이익) |
| lossAmountKrw | BigDecimal | 같은 값을 원화로 환산한 금액 |

같은 원칙의 `lossAmountKrw` 를 모든 거래에 걸쳐 합하면 `ruleImpacts[].totalLossAmount` 와 같다. 복기 그래프의 원칙 골라 보기는 이 값과 `occurredAt` 을 사용한다.

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| ROUND_NOT_FOUND | 404 | 투자 라운드를 찾을 수 없음 |
| ROUND_ACCESS_DENIED | 403 | 본인의 라운드가 아님 |

투자 원칙이 없는 라운드는 에러 대신 빈 `ruleImpacts`/`violationDetails`를 반환한다.

## 이벤트 컨트랙트

메시지 큐를 사용하지 않는다. 리포트는 배치(RegretReportJob)가 거래소별로 사전 생성하며 API는 RDB 조회와 라운드 단위 합산만 수행한다. 배치 상세는 [regret-report-batch.md](../regret-report-batch.md)를 참조한다.
