## API 명세

`POST /api/rounds`

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| seeds | Array | O | 거래소별 시드머니 배분 |
| seeds[].exchangeId | Long | O | 거래소 ID |
| seeds[].amount | BigDecimal | O | 기축통화 금액 (국내: KRW, 바이낸스: USDT) |
| emergencyFundingLimit | BigDecimal | O | 1회 긴급 자금 투입 상한 (0 = 미사용) |
| rules | Array | X | 투자 원칙 목록 (빈 배열 = 원칙 없음) |
| rules[].ruleType | String | O | `LOSS_CUT` \| `PROFIT_TAKE` \| `CHASE_BUY_BAN` \| `AVERAGING_DOWN_LIMIT` \| `OVERTRADING_LIMIT` |
| rules[].thresholdValue | BigDecimal | O | 기준값 (비율: %, 횟수: 회) |

### Request

```json
{
  "seeds": [
    { "exchangeId": 1, "amount": 5000000 },
    { "exchangeId": 2, "amount": 3000000 },
    { "exchangeId": 3, "amount": 100 }
  ],
  "emergencyFundingLimit": 500000,
  "rules": [
    { "ruleType": "LOSS_CUT", "thresholdValue": 10 },
    { "ruleType": "PROFIT_TAKE", "thresholdValue": 30 },
    { "ruleType": "CHASE_BUY_BAN", "thresholdValue": 15 },
    { "ruleType": "AVERAGING_DOWN_LIMIT", "thresholdValue": 3 },
    { "ruleType": "OVERTRADING_LIMIT", "thresholdValue": 10 }
  ]
}
```

### Response

```json
{
  "status": 201,
  "code": "CREATED",
  "message": "투자 라운드가 시작되었습니다.",
  "data": {
    "roundId": 1,
    "roundNumber": 1,
    "status": "ACTIVE",
    "initialSeed": 8000100,
    "emergencyFundingLimit": 500000,
    "emergencyChargeCount": 3,
    "rules": [
      { "ruleId": 1, "ruleType": "LOSS_CUT", "thresholdValue": 10 },
      { "ruleId": 2, "ruleType": "PROFIT_TAKE", "thresholdValue": 30 },
      { "ruleId": 3, "ruleType": "CHASE_BUY_BAN", "thresholdValue": 15 },
      { "ruleId": 4, "ruleType": "AVERAGING_DOWN_LIMIT", "thresholdValue": 3 },
      { "ruleId": 5, "ruleType": "OVERTRADING_LIMIT", "thresholdValue": 10 }
    ],
    "wallets": [
      { "walletId": 1, "exchangeId": 1 },
      { "walletId": 2, "exchangeId": 2 },
      { "walletId": 3, "exchangeId": 3 }
    ],
    "startedAt": "2026-02-27T14:30:00"
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| ACTIVE_ROUND_EXISTS | 409 | 이미 진행 중인 라운드 존재 |
| INVALID_SEED_AMOUNT | 400 | 거래소별 시드머니 범위 초과 |
| INVALID_EMERGENCY_FUNDING_LIMIT | 400 | 긴급 자금 상한 초과 (최대 100만) |
| INVALID_RULE_THRESHOLD | 400 | 원칙 설정값 유효성 위반 (비율 0 이하, 횟수 0 이하 등) |
