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
        "chain": "ERC-20",
        "toAddress": "0xinvalidaddress",
        "toTag": null,
        "amount": 0.005,
        "fee": 0.0008,
        "status": "FROZEN",
        "failureReason": "WRONG_ADDRESS",
        "frozenUntil": "2026-03-04T14:30:00",
        "createdAt": "2026-03-03T14:30:00",
        "completedAt": null
      },
      {
        "transferId": 1,
        "type": "DEPOSIT",
        "coinId": 1,
        "coinSymbol": "BTC",
        "chain": "Bitcoin",
        "toAddress": "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq",
        "toTag": null,
        "amount": 0.005,
        "fee": 0,
        "status": "SUCCESS",
        "failureReason": null,
        "frozenUntil": null,
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
| fee | 입금(DEPOSIT)인 경우 0. 수수료는 출발 지갑에서 부담한다 |
| frozenUntil | FROZEN 상태일 때만 값이 있다. 이 시각 이후 자동 반환된다 |
| completedAt | 송금이 완료(SUCCESS) 또는 반환(REFUNDED)된 시각. FROZEN 상태이면 null |
| nextCursor | 다음 페이지의 커서 값. 마지막 페이지이면 null |
| hasNext | 다음 페이지 존재 여부 |

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| WALLET_NOT_FOUND | 404 | 지갑을 찾을 수 없음 |
