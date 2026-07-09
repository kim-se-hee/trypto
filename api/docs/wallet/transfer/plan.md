## API 명세

`POST /api/transfers`

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| idempotencyKey | UUID | O | 멱등성 키 (클라이언트 생성) |
| fromWalletId | Long | O | 출발 지갑 ID |
| toWalletId | Long | O | 도착 지갑 ID |
| coinId | Long | O | 송금 코인 ID |
| amount | BigDecimal | O | 송금 수량 (양수) |

### Request

```json
{
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440001",
  "fromWalletId": 1,
  "toWalletId": 2,
  "coinId": 1,
  "amount": 0.005
}
```

### Response

서버만 알 수 있는 필드만 반환한다.

| 필드 | 타입 | 설명 |
|------|------|------|
| transferId | Long | 생성된 송금 ID (이체 내역 prepend 시 key·cursor로 사용) |
| status | String | `SUCCESS` |

```json
{
  "status": 201,
  "code": "CREATED",
  "message": "송금이 요청되었습니다.",
  "data": {
    "transferId": 1,
    "status": "SUCCESS"
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| WALLET_NOT_FOUND | 404 | 출발/도착 지갑을 찾을 수 없음 |
| DIFFERENT_ROUND_TRANSFER | 400 | 출발 지갑과 도착 지갑이 서로 다른 라운드 |
| INSUFFICIENT_BALANCE | 400 | 가용 잔고 부족 (가용 잔고 < 송금 수량) |
| SAME_WALLET_TRANSFER | 400 | 출발 지갑과 도착 지갑이 동일 |
