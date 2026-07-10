## API 명세

`GET /api/exchanges/{exchangeId}/coins`

### Path Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| exchangeId | Long | O | 거래소 ID |

### Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "거래소 상장 코인 목록을 조회했습니다.",
  "data": [
    {
      "exchangeCoinId": 101,
      "coinId": 1,
      "coinSymbol": "BTC",
      "coinName": "비트코인"
    },
    {
      "exchangeCoinId": 102,
      "coinId": 2,
      "coinSymbol": "ETH",
      "coinName": "이더리움"
    }
  ]
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| EXCHANGE_NOT_FOUND | 404 | 거래소를 찾을 수 없음 |
