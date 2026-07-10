## 멱등성

전역 멱등성 메커니즘 [../../idempotency.md](../../idempotency.md) 를 따른다. 리소스 타입은 `PLACE_ORDER` 다.

### 중복 응답 처리

- 재요청 시 컨트롤러가 되돌려줄 값은 **기존 주문 전체**다. `DuplicateRequestException` 을 catch 한 뒤 `clientOrderId` 로 선점 행의 리소스 ID(주문 ID)를 조회(`IdempotencyKeyQueryPort.findResourceId`)하고, 그 ID 로 주문을 다시 읽어(`GetOrderUseCase.getById`) 최초 처리와 동일한 `PlaceOrderResponse` 를 재구성한다.
- 최초 처리·재요청 모두 `201 Created`. 응답 메시지는 조회한 주문 종류로 정한다 — 시장가는 "주문이 체결되었습니다.", 지정가는 "주문이 등록되었습니다.".

## API 명세

### 참고사항

- 클라이언트가 거래소-코인 목록 조회 시 이미 보유한 정보(거래소명, 코인 심볼, 기준 통화 등)는 응답에 포함하지 않는다.
- `exchangeCoinId`로 클라이언트가 로컬 룩업하여 표시한다.

`POST /api/orders`

### Request Body

| 필드             | 타입         | 필수  | 설명                              |
|----------------|------------|-----|---------------------------------|
| clientOrderId  | String     | O   | 멱등성 키 (클라이언트 생성)                |
| walletId       | Long       | O   | 주문 지갑 ID                        |
| exchangeCoinId | Long       | O   | 거래소-코인 ID                       |
| side           | String     | O   | `BUY` \| `SELL`                 |
| orderType      | String     | O   | `MARKET` \| `LIMIT`             |
| volume         | BigDecimal | 조건부 | 주문 수량(코인)                       |
| price          | BigDecimal | 조건부 | 지정가: 단가, 시장가 매수: 주문 총액(기준 통화)   |

#### `volume`/`price` 조합 규칙

| 주문     | volume     | price     |
|--------|------------|-----------|
| 시장가 매수 | 사용 안 함     | 필수 — 주문 총액 |
| 시장가 매도 | 필수 — 주문 수량 | 사용 안 함    |
| 지정가 매수 | 필수 — 주문 수량 | 필수 — 단가   |
| 지정가 매도 | 필수 — 주문 수량 | 필수 — 단가   |

"사용 안 함"인 값이 요청에 담겨 오면 주문을 거부한다.

### Request

**지정가 매수** — 빗썸에서 BTC를 1억원에 0.005개 매수

```json
{
  "clientOrderId": "550e8400-e29b-41d4-a716-446655440001",
  "walletId": 2,
  "exchangeCoinId": 7,
  "side": "BUY",
  "orderType": "LIMIT",
  "volume": 0.005,
  "price": 100000000
}
```

### Response

지정가 주문은 접수 시 `PENDING` 으로 생성되고, 체결 관련 필드는 엔진 매칭 전까지 `null` 이다.

```json
{
  "status": 201,
  "code": "CREATED",
  "message": "주문이 등록되었습니다.",
  "data": {
    "orderId": 42,
    "side": "BUY",
    "orderType": "LIMIT",
    "orderAmount": null,
    "quantity": 0.005,
    "price": 100000000,
    "filledPrice": null,
    "fee": null,
    "status": "PENDING",
    "createdAt": "2026-02-21T14:30:00",
    "filledAt": null
  }
}
```

### 에러 응답

| code                     | status | 설명                |
|--------------------------|--------|-------------------|
| INSUFFICIENT_BALANCE     | 400    | 잔고 부족             |
| BELOW_MIN_ORDER_AMOUNT   | 400    | 최소 주문 금액 미달       |
| ABOVE_MAX_ORDER_AMOUNT   | 400    | 최대 주문 금액 초과       |
| VOLUME_REQUIRED          | 400    | 필수인 volume 누락      |
| PRICE_REQUIRED           | 400    | 필수인 price 누락       |
| VOLUME_NOT_ALLOWED       | 400    | 시장가 매수에 volume 전송  |
| PRICE_NOT_ALLOWED        | 400    | 시장가 매도에 price 전송   |
| WALLET_NOT_FOUND         | 404    | 지갑을 찾을 수 없음       |
| EXCHANGE_COIN_NOT_FOUND  | 404    | 거래소-코인을 찾을 수 없음   |
| INVESTMENT_RULE_NOT_FOUND | 404   | 투자 원칙을 찾을 수 없음   |

## 이벤트 컨트랙트

지정가 주문은 커밋 직후 주문 접수 이벤트를 `engine.inbox` 큐로 발행한다. 시장가 주문은 발행하지 않는다. 채널 규약과 페이로드(OrderPlaced)는 [../../../../docs/contracts/engine-inbox.md](../../../../docs/contracts/engine-inbox.md) 참조.
