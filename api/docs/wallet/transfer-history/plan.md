## API 명세

`GET /api/wallets/{walletId}/transfers?cursor=100&size=20&type=ALL`

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| walletId | Long | O | 지갑 ID |

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| cursor | Long | X | null | 이전 페이지의 마지막 transferId (첫 페이지는 생략) |
| size | Integer | X | 20 | 페이지 크기 (1~50) |
| type | String | X | ALL | `ALL` \| `DEPOSIT` \| `WITHDRAW` |

### Response

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "송금 내역을 조회했습니다.",
  "data": {
    "content": [
      {
        "transferId": 2,
        "type": "WITHDRAW",
        "coinId": 1,
        "coinSymbol": "BTC",
        "amount": 0.005,
        "status": "SUCCESS",
        "createdAt": "2026-03-03T14:30:00",
        "completedAt": "2026-03-03T14:30:00"
      },
      {
        "transferId": 1,
        "type": "DEPOSIT",
        "coinId": 1,
        "coinSymbol": "BTC",
        "amount": 0.005,
        "status": "SUCCESS",
        "createdAt": "2026-03-03T14:00:00",
        "completedAt": "2026-03-03T14:00:00"
      }
    ],
    "nextCursor": 1,
    "hasNext": false
  }
}
```

#### 응답 필드 설명

| 필드 | 설명 |
|------|------|
| type | `DEPOSIT`: 입금 (해당 지갑이 도착지), `WITHDRAW`: 출금 (해당 지갑이 출발지) |
| coinSymbol | 코인 심볼 (예: BTC, ETH). marketdata 컨텍스트에서 coinId로 조회한다 |
| completedAt | 송금이 완료(SUCCESS)된 시각 |
| nextCursor | 다음 페이지의 커서 값. 마지막 페이지이면 null |
| hasNext | 다음 페이지 존재 여부 |

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| WALLET_NOT_FOUND | 404 | 지갑을 찾을 수 없음 |
