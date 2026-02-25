# API 명세 — CEX 주문 (Trading)

> 업비트·빗썸·바이낸스 공통. 단일 API로 모든 CEX 거래소의 시장가/지정가 주문을 처리한다.
> 상세 계산 로직과 비즈니스 규칙은 `business-rules.md`를 참고한다.

## 공통 사항

### 인증

모든 API는 인증된 사용자만 호출 가능하다. (인증 방식은 Identity 도메인에서 정의)

### 응답 설계 원칙

클라이언트가 거래소-코인 목록 조회 시 이미 보유한 정보(거래소명, 코인 심볼, 기준 통화 등)는 응답에 포함하지 않는다. `exchangeCoinId`로 클라이언트가 로컬 룩업하여 표시한다.

### `amount` 필드 규칙

| side | amount 의미 | 단위                          |
|------|-----------|-----------------------------|
| BUY  | 주문 총액     | 기준 통화 (국내: KRW, 바이낸스: USDT) |
| SELL | 주문 수량     | 코인                          |

---

## API 목록

| Method | Endpoint                     | 설명          |
|--------|------------------------------|-------------|
| POST   | /api/orders                  | 주문 생성       |
| GET    | /api/orders/available        | 주문 가능 정보 조회 |
| GET    | /api/orders                  | 주문 내역 조회    |
| POST   | /api/orders/{orderId}/cancel | 미체결 주문 취소   |

---

## 1. 주문 생성

`POST /api/orders`

### 멱등성

클라이언트가 `clientOrderId`(String)를 생성하여 전송한다. 서버는 동일한 `clientOrderId`로 중복 요청이 들어오면 기존 주문 결과를 반환하고 새 주문을 생성하지 않는다.

### Request Body

| 필드             | 타입         | 필수  | 설명                              |
|----------------|------------|-----|---------------------------------|
| clientOrderId  | String     | O   | 멱등성 키 (클라이언트 생성)                |
| walletId       | Long       | O   | 주문 지갑 ID                        |
| exchangeCoinId | Long       | O   | 거래소-코인 ID                       |
| side           | String     | O   | `BUY` \| `SELL`                 |
| orderType      | String     | O   | `MARKET` \| `LIMIT`             |
| price          | BigDecimal | 조건부 | 지정가 (LIMIT일 때 필수, MARKET일 때 무시) |
| amount         | BigDecimal | O   | 매수: 주문 총액, 매도: 주문 수량            |

### Request 예시

**시장가 매수** — 업비트에서 BTC 10만원어치 매수

```json
{
  "clientOrderId": "550e8400-e29b-41d4-a716-446655440000",
  "walletId": 1,
  "exchangeCoinId": 3,
  "side": "BUY",
  "orderType": "MARKET",
  "amount": 100000
}
```

**지정가 매수** — 빗썸에서 BTC를 1억원에 50만원어치 매수

```json
{
  "clientOrderId": "550e8400-e29b-41d4-a716-446655440001",
  "walletId": 2,
  "exchangeCoinId": 7,
  "side": "BUY",
  "orderType": "LIMIT",
  "price": 100000000,
  "amount": 500000
}
```

**시장가 매도** — 바이낸스에서 ETH 0.5개 매도

```json
{
  "clientOrderId": "550e8400-e29b-41d4-a716-446655440002",
  "walletId": 5,
  "exchangeCoinId": 15,
  "side": "SELL",
  "orderType": "MARKET",
  "amount": 0.5
}
```

**지정가 매도** — 업비트에서 BTC 0.001개를 1.1억원에 매도

```json
{
  "clientOrderId": "550e8400-e29b-41d4-a716-446655440003",
  "walletId": 1,
  "exchangeCoinId": 3,
  "side": "SELL",
  "orderType": "LIMIT",
  "price": 110000000,
  "amount": 0.001
}
```

### Response — 시장가 즉시 체결 (201 Created)

```json
{
  "status": 201,
  "code": "CREATED",
  "message": "주문이 체결되었습니다.",
  "data": {
    "orderId": 42,
    "side": "BUY",
    "orderType": "MARKET",
    "orderAmount": 99872.54,
    "quantity": 0.00099726,
    "price": null,
    "filledPrice": 100274000,
    "fee": 49.94,
    "status": "FILLED",
    "createdAt": "2026-02-21T14:30:00",
    "filledAt": "2026-02-21T14:30:00"
  }
}
```

### Response — 지정가 대기 (201 Created)

```json
{
  "status": 201,
  "code": "CREATED",
  "message": "주문이 등록되었습니다.",
  "data": {
    "orderId": 43,
    "side": "BUY",
    "orderType": "LIMIT",
    "orderAmount": 500000,
    "quantity": 0.005,
    "price": 100000000,
    "filledPrice": 100000000,
    "fee": 250,
    "status": "PENDING",
    "createdAt": "2026-02-21T14:31:00",
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
| PRICE_REQUIRED_FOR_LIMIT | 400    | 지정가 주문 시 price 누락 |
| WALLET_NOT_FOUND         | 404    | 지갑을 찾을 수 없음       |
| EXCHANGE_COIN_NOT_FOUND  | 404    | 거래소-코인을 찾을 수 없음   |

---

## 2. 주문 가능 정보 조회

`GET /api/orders/available`

주문 화면에서 "주문 가능" 금액/수량과 현재가를 표시하기 위한 API.

### Query Parameters

| 파라미터           | 타입     | 필수 | 설명              |
|----------------|--------|----|-----------------|
| walletId       | Long   | O  | 지갑 ID           |
| exchangeCoinId | Long   | O  | 거래소-코인 ID       |
| side           | String | O  | `BUY` \| `SELL` |

### Response (200 OK)

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "조회 성공",
  "data": {
    "available": 5000000,
    "currentPrice": 100274000
  }
}
```

| 필드           | 설명                                      |
|--------------|-----------------------------------------|
| available    | 매수: 주문 가능 금액 (기준 통화), 매도: 주문 가능 수량 (코인) |
| currentPrice | 현재가 (기준 통화 기준)                          |

---

## 3. 주문 내역 조회

`GET /api/orders`

미체결/체결 탭 구분을 위해 `status` 필터를 사용한다.

- **미체결 탭:** `status=PENDING`
- **체결 탭:** `status=FILLED`

### Query Parameters

| 파라미터           | 타입     | 필수 | 기본값 | 설명                                               |
|----------------|--------|----|-----|--------------------------------------------------|
| walletId       | Long   | O  | —   | 지갑 ID                                            |
| exchangeCoinId | Long   | X  | —   | 거래소-코인 ID                                        |
| side           | String | X  | —   | `BUY` \| `SELL`                                  |
| status         | String | X  | —   | `FILLED` \| `PENDING` \| `CANCELLED` \| `FAILED` |
| page           | int    | X  | 0   | 페이지 번호 (0부터 시작)                                  |
| size           | int    | X  | 20  | 페이지 크기 (1~50)                                    |

### Response — 체결 내역 (200 OK)

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "조회 성공",
  "data": {
    "page": 0,
    "size": 20,
    "totalPages": 3,
    "content": [
      {
        "orderId": 42,
        "exchangeCoinId": 5,
        "side": "BUY",
        "orderType": "LIMIT",
        "filledPrice": 74600,
        "price": 74600,
        "quantity": 5.44649808,
        "orderAmount": 406309,
        "fee": 203,
        "createdAt": "2024-12-14T00:00:00",
        "filledAt": "2024-12-14T00:00:00"
      },
      {
        "orderId": 41,
        "exchangeCoinId": 5,
        "side": "SELL",
        "orderType": "LIMIT",
        "filledPrice": 74000,
        "price": 74000,
        "quantity": 8.56284413,
        "orderAmount": 633650,
        "fee": 317,
        "createdAt": "2024-12-13T15:57:00",
        "filledAt": "2024-12-13T15:57:00"
      }
    ]
  }
}
```

### Response — 미체결 내역 (200 OK)

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "조회 성공",
  "data": {
    "page": 0,
    "size": 20,
    "totalPages": 1,
    "content": [
      {
        "orderId": 43,
        "exchangeCoinId": 3,
        "side": "BUY",
        "orderType": "LIMIT",
        "filledPrice": 100000000,
        "price": 100000000,
        "quantity": 0.005,
        "orderAmount": 500000,
        "fee": 250,
        "createdAt": "2026-02-21T14:31:00",
        "filledAt": null
      }
    ]
  }
}
```

---

## 4. 미체결 주문 취소

`POST /api/orders/{orderId}/cancel`

PENDING 상태의 지정가 주문만 취소 가능하다. 취소 시 점유된 잔고가 해제된다.

### 멱등성

이미 취소된 주문에 대한 재요청은 에러 없이 현재 상태(CANCELLED)를 그대로 반환한다.

### Path Parameters

| 파라미터    | 타입   | 설명        |
|---------|------|-----------|
| orderId | Long | 취소할 주문 ID |

### Response (200 OK)

```json
{
  "status": 200,
  "code": "SUCCESS",
  "message": "주문이 취소되었습니다.",
  "data": {
    "orderId": 43,
    "status": "CANCELLED"
  }
}
```

### 에러 응답

| code                  | status | 설명                  |
|-----------------------|--------|---------------------|
| ORDER_NOT_FOUND       | 404    | 주문을 찾을 수 없음         |
| ORDER_NOT_CANCELLABLE | 400    | 이미 체결/실패된 주문은 취소 불가 |
