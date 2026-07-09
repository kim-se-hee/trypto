## API 명세

`GET /api/wallets/{walletId}/deposit-address?coinId={coinId}`

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| coinId | Long | O | 코인 ID |

### Response

```json
{
  "status": 200,
  "code": "OK",
  "message": "조회 성공",
  "data": {
    "depositAddressId": 1,
    "walletId": 1,
    "address": "a1b2c3d4e5f6..."
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| WALLET_NOT_FOUND | 404 | 지갑을 찾을 수 없음 |
| BASE_CURRENCY_NOT_TRANSFERABLE | 400 | KRW는 송금할 수 없음 |
