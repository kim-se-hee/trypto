## API 명세

`GET /api/exchanges/{exchangeId}/coins/{coinId}/chains`

### Path Parameters

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| exchangeId | Long | O | 거래소 ID |
| coinId | Long | O | 코인 ID |

### Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "코인 체인 목록을 조회했습니다.",
  "data": [
    {
      "exchangeCoinChainId": 1,
      "chain": "Bitcoin",
      "tagRequired": false
    },
    {
      "exchangeCoinChainId": 2,
      "chain": "ERC-20",
      "tagRequired": false
    },
    {
      "exchangeCoinChainId": 3,
      "chain": "BEP-20",
      "tagRequired": false
    }
  ]
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| EXCHANGE_COIN_NOT_FOUND | 404 | 해당 거래소에 상장되지 않은 코인 |
