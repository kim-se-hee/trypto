## API 명세

`POST /api/transfers`

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| clientTransferId | UUID | O | 멱등성 키 (클라이언트 생성) |
| fromWalletId | Long | O | 출발 지갑 ID |
| coinId | Long | O | 송금 코인 ID |
| chain | String | O | 사용 체인 (예: "ERC-20", "Bitcoin") |
| toAddress | String | O | 도착 주소 (직접 입력) |
| toTag | String | X | 태그/메모 |
| amount | BigDecimal | O | 송금 수량 |

### Request

```json
{
  "clientTransferId": "550e8400-e29b-41d4-a716-446655440001",
  "fromWalletId": 1,
  "coinId": 1,
  "chain": "Bitcoin",
  "toAddress": "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq",
  "toTag": null,
  "amount": 0.005
}
```

### Response

요청에 포함된 값(coinId, chain, amount 등)은 프론트가 이미 알고 있으므로 응답에서 제외한다. 서버만 알 수 있는 필드만 반환한다.

| 필드 | 타입 | 설명 |
|------|------|------|
| transferId | Long | 생성된 송금 ID (이체 내역 prepend 시 key·cursor로 사용) |
| status | String | `SUCCESS` / `FROZEN` |
| fee | BigDecimal | 출금 수수료 |
| failureReason | String? | `WRONG_ADDRESS` / `WRONG_CHAIN` / `MISSING_TAG` (SUCCESS이면 null) |
| frozenUntil | LocalDateTime? | 동결 해제 예정 시각 (SUCCESS이면 null) |

#### SUCCESS

```json
{
  "status": 201,
  "code": "CREATED",
  "message": "송금이 완료되었습니다.",
  "data": {
    "transferId": 1,
    "status": "SUCCESS",
    "fee": 0.0005,
    "failureReason": null,
    "frozenUntil": null
  }
}
```

#### FROZEN

```json
{
  "status": 201,
  "code": "CREATED",
  "message": "송금 자금이 동결되었습니다.",
  "data": {
    "transferId": 2,
    "status": "FROZEN",
    "fee": 0.0008,
    "failureReason": "WRONG_ADDRESS",
    "frozenUntil": "2026-03-04T14:30:00"
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| WALLET_NOT_FOUND | 404 | 지갑을 찾을 수 없음 |
| BASE_CURRENCY_NOT_TRANSFERABLE | 400 | KRW는 송금할 수 없음 |
| UNSUPPORTED_CHAIN | 400 | 출발 거래소가 해당 코인+체인을 지원하지 않음 |
| BELOW_MIN_WITHDRAWAL | 400 | 최소 출금 수량 미달 |
| INSUFFICIENT_BALANCE | 400 | 잔고 부족 (가용 잔고 < 송금 수량 + 수수료) |
