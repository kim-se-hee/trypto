## API 명세

`GET /api/rounds/active`

### 참고사항

- 현행 라운드 API 패턴에 맞춰 인증 컨텍스트 대신 `userId`를 요청 파라미터로 받는다.
- `status`, `code`, `message`, `data` 형태의 `ApiResponseDto<T>`를 사용한다.

### Request Query

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | O | 사용자 ID |

### Request

```http
GET /api/rounds/active?userId=1
```

### Response

```json
{
  "status": 200,
  "code": "OK",
  "message": "활성 라운드를 조회했습니다.",
  "data": {
    "roundId": 1,
    "userId": 1,
    "roundNumber": 3,
    "status": "ACTIVE",
    "initialSeed": 8000100,
    "emergencyFundingLimit": 500000,
    "emergencyChargeCount": 2,
    "startedAt": "2026-02-27T14:30:00",
    "endedAt": null,
    "rules": [
      { "ruleId": 11, "ruleType": "LOSS_CUT", "thresholdValue": 10 },
      { "ruleId": 12, "ruleType": "PROFIT_TAKE", "thresholdValue": 25 }
    ],
    "wallets": [
      { "walletId": 1, "exchangeId": 1 },
      { "walletId": 2, "exchangeId": 2 },
      { "walletId": 3, "exchangeId": 3 }
    ]
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| ROUND_NOT_ACTIVE | 404 | 진행 중인 라운드가 없음 |
