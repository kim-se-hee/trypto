## API 명세

### 참고사항

- 배치가 생성한 리포트를 조회한다. 리포트가 없으면 `REPORT_NOT_FOUND` 에러를 반환한다
- 거래소별로 요청한다

`GET /api/rounds/{roundId}/regret?exchangeId={exchangeId}`

### Path Parameter

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| roundId | Long | O | 투자 라운드 ID |

### Query Parameter

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| exchangeId | Long | O | 거래소 ID |

### Request

```
GET /api/rounds/1/regret?exchangeId=1
```

### Response

```json
{
  "status": 200,
  "code": "OK",
  "message": "투자 복기 리포트를 조회했습니다.",
  "data": {
    "reportId": 1,
    "roundId": 1,
    "exchangeId": 1,
    "exchangeName": "업비트",
    "currency": "KRW",
    "totalViolations": 5,
    "analysisStart": "2026-01-15",
    "analysisEnd": "2026-02-25",
    "missedProfit": 893837,
    "actualProfitRate": 4.0,
    "ruleFollowedProfitRate": 12.9,

    "ruleImpacts": [
      {
        "ruleImpactId": 1,
        "ruleId": 3,
        "ruleType": "CHASE_BUY_BAN",
        "thresholdValue": 20,
        "thresholdUnit": "%",
        "violationCount": 2,
        "totalLossAmount": 265000,
        "impactGap": 4.5
      },
      {
        "ruleImpactId": 2,
        "ruleId": 4,
        "ruleType": "AVERAGING_DOWN_LIMIT",
        "thresholdValue": 2,
        "thresholdUnit": "회",
        "violationCount": 1,
        "totalLossAmount": 120000,
        "impactGap": 1.2
      },
      {
        "ruleImpactId": 3,
        "ruleId": 1,
        "ruleType": "LOSS_CUT",
        "thresholdValue": 10,
        "thresholdUnit": "%",
        "violationCount": 2,
        "totalLossAmount": 350000,
        "impactGap": 3.5
      }
    ],

    "violationDetails": [
      {
        "violationDetailId": 1,
        "orderId": 15,
        "coinSymbol": "DOGE",
        "violatedRules": ["CHASE_BUY_BAN"],
        "profitLoss": -385000,
        "occurredAt": "2026-01-22T14:30:00"
      },
      {
        "violationDetailId": 2,
        "orderId": 18,
        "coinSymbol": "SOL",
        "violatedRules": ["CHASE_BUY_BAN"],
        "profitLoss": 120000,
        "occurredAt": "2026-01-25T11:00:00"
      },
      {
        "violationDetailId": 3,
        "orderId": 22,
        "coinSymbol": "SHIB",
        "violatedRules": ["LOSS_CUT", "AVERAGING_DOWN_LIMIT"],
        "profitLoss": -220000,
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
| reportId | Long | 리포트 ID |
| roundId | Long | 투자 라운드 ID |
| exchangeId | Long | 거래소 ID |
| exchangeName | String | 거래소 이름 |
| currency | String | 기축통화 (KRW, USDT) |
| totalViolations | Integer | 해당 거래소의 총 위반 횟수 |
| analysisStart | LocalDate | 분석 시작일 (라운드 시작일) |
| analysisEnd | LocalDate | 분석 종료일 (배치가 적재한 마지막 스냅샷 날짜) |
| missedProfit | BigDecimal | 놓친 수익 금액 (기축통화 단위) |
| actualProfitRate | BigDecimal | 실제 수익률 (%) |
| ruleFollowedProfitRate | BigDecimal | 모든 원칙 준수 시 시뮬레이션 수익률 (%) |

#### ruleImpacts[]

| 필드 | 타입 | 설명 |
|------|------|------|
| ruleImpactId | Long | 시나리오 ID |
| ruleId | Long | 투자 원칙 ID |
| ruleType | String | 원칙 유형 (`LOSS_CUT`, `PROFIT_TAKE`, `CHASE_BUY_BAN`, `AVERAGING_DOWN_LIMIT`, `OVERTRADING_LIMIT`) |
| thresholdValue | BigDecimal | 설정된 기준값 |
| thresholdUnit | String | 기준값 단위 (`%` 또는 `회`) |
| violationCount | Integer | 해당 거래소에서 해당 규칙의 위반 횟수 |
| totalLossAmount | BigDecimal | 위반으로 인한 총 손실 금액 (기축통화 단위. 양수: 손실, 음수: 오히려 이익) |
| impactGap | BigDecimal | 수익률 영향 차이 (%p). 원칙 준수 시 수익률 - 실제 수익률 |

#### violationDetails[]

| 필드 | 타입 | 설명 |
|------|------|------|
| violationDetailId | Long | 위반 거래 ID |
| orderId | Long (nullable) | 주문 ID. 주문 시점 위반(추격매수, 물타기, 과매매)은 해당 주문 ID. 가격 모니터링 위반(손절, 익절)은 null |
| coinSymbol | String | 코인 심볼 (예: BTC, ETH, DOGE) |
| violatedRules | String[] | 위반한 규칙 유형 목록 (하나의 거래가 여러 규칙 위반 가능). `LOSS_CUT`, `PROFIT_TAKE`, `CHASE_BUY_BAN`, `AVERAGING_DOWN_LIMIT`, `OVERTRADING_LIMIT` |
| profitLoss | BigDecimal | 해당 거래의 실현/미실현 손익 (기축통화 단위). 음수: 손실, 양수: 이익 |
| occurredAt | LocalDateTime | 위반 발생 시각. 주문 시점 위반은 체결 시각, 가격 모니터링 위반은 감지 시각 |

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| ROUND_NOT_FOUND | 404 | 투자 라운드를 찾을 수 없음 |
| ROUND_ACCESS_DENIED | 403 | 본인의 라운드가 아님 |
| WALLET_NOT_FOUND | 404 | 해당 거래소의 지갑이 라운드에 존재하지 않음 |
| REPORT_NOT_FOUND | 404 | 복기 리포트가 아직 생성되지 않음 (배치 실행 전 조회 시) |

투자 원칙이 없는 라운드는 에러 대신 빈 `ruleImpacts`/`violationDetails`를 반환한다.

## 이벤트 컨트랙트

메시지 큐를 사용하지 않는다. 리포트는 배치(RegretReportJob)가 사전 생성하며 API는 RDB 조회만 수행한다. 배치 상세는 [regret-report-batch.md](../regret-report-batch.md)를 참조한다.
