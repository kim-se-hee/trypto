## 개요

출금 수수료를 조회하는 REST API다. FindWithdrawalFeeUseCase와 Service는 이미 구현되어 있으며,
HTTP Controller만 추가한다.

## API 명세

`GET /api/withdrawal-fees?exchangeId={exchangeId}&coinId={coinId}&chain={chain}`

### Query Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| exchangeId | Long | O | 거래소 ID |
| coinId | Long | O | 코인 ID |
| chain | String | O | 네트워크 (ERC20, TRC20, SOL 등) |

### Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "출금 수수료를 조회했습니다.",
  "data": {
    "fee": 0.0005,
    "minWithdrawal": 0.001
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| WITHDRAWAL_FEE_NOT_FOUND | 404 | 해당 조합의 수수료 정보 없음 |
