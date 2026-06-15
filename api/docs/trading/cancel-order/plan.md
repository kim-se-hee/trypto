## 도메인 모델

### Order (수정)

- 미체결에서 취소로의 도메인 상태 전이를 담당한다.
- 잔고 해제 대상과 금액을 주문 정보(매수면 체결금액 + 수수료, 매도면 체결 수량)에서 끌어낸다.

## 타 컨텍스트 의존성

- MarketData.FindExchangeCoinMappingUseCase — 취소 시 잔고 해제 대상 코인 결정
- MarketData.FindExchangeDetailUseCase — 기준 통화·수수료율 조회
- Wallet.ManageWalletBalanceUseCase — 점유 잔고 해제

## 주문 상태 전이 (CAS UPDATE)

QueryDSL CAS UPDATE로 원자적 상태 전이를 수행한다.

```sql
UPDATE orders SET status = 'CANCELLED' WHERE order_id = ? AND status = 'PENDING'
```

- affected rows = 1이면 CAS 성공 (취소 진행)
- affected rows = 0이면 CAS 실패 (이미 체결/취소됨 → `ORDER_NOT_CANCELLABLE` 예외)

CAS 성공 후 조회한 주문에 대해 `Order.cancel()` 도메인 메서드로 도메인 상태를 동기화한다.

## 잔고 unlock

주문 생성 시 lock된 잔고를 해제한다. unlock 대상 코인을 결정하기 위해 `exchangeCoinId → coinId` 매핑과 거래소 상세(baseCurrencyCoinId)를 조회한다.

| 주문 | unlock 대상 | 금액 |
|------|------------|------|
| 지정가 매수 | 기준 통화 (KRW/USDT) | 체결금액 + 수수료 (`getSettlementDebit()`) |
| 지정가 매도 | 코인 | 체결수량 (`getQuantity().value()`) |

## 엔진 이벤트 발행

트랜잭션 커밋 직후 `OrderCanceledEvent` 를 `ApplicationEventPublisher` 로 발행한다. `EngineInboxPublisher` 가 `@TransactionalEventListener(phase = AFTER_COMMIT)` 로 받아 `engine.inbox` 큐에 `event_type=OrderCanceled` 로 전송한다.

- **AFTER_COMMIT 이유**: DB 가 CANCELLED 로 확정되기 전에 엔진이 오더북에서 제거해버리면, 엔진 쪽 취소는 반영됐는데 DB 롤백이 일어나 상태가 어긋날 수 있다.
- **전송 실패 처리**: 엔진 전송이 실패해도 DB 상태는 이미 CANCELLED 이므로 사용자 응답에는 영향이 없다. 엔진이 해당 주문을 잘못 체결하려 해도 `orders` UPDATE 의 `WHERE status='PENDING'` 가드에 걸려 skip 되므로 이중 체결은 방어된다. 현재는 warn 로그만 남긴다.

## 트랜잭션 범위

주문 CAS UPDATE와 잔고 unlock은 반드시 같은 트랜잭션에서 수행한다. 주문은 CANCELLED인데 잔고가 lock된 상태를 방지한다.

```
@Transactional 범위:
    ├─ 주문 조회 + 소유권 검증
    ├─ CAS UPDATE (PENDING → CANCELLED)
    └─ 잔고 unlock (CAS UPDATE)

AFTER_COMMIT:
    └─ EngineInboxPublisher: engine.inbox 로 OrderCanceled 전송
```

## 매칭과의 동시성 제어

취소 요청과 엔진의 체결 DB 쓰기가 동일 주문에 대해 동시에 발생할 수 있다. 양쪽 모두 `WHERE status = 'PENDING'` 조건의 CAS UPDATE를 사용하므로, DB 레벨에서 하나만 성공한다.

- 체결 CAS가 먼저 성공하면: 취소 CAS가 실패 (affected rows = 0) → `ORDER_NOT_CANCELLABLE` (400)
- 취소 CAS가 먼저 성공하면: 체결 CAS가 실패 (affected rows = 0) → 엔진 쪽에서 잔고·outbox·holding 단계 모두 skip

| 전략 | 장점 | 단점 |
|------|------|------|
| 비관적 락 (`SELECT FOR UPDATE`) | 확실한 직렬화 | 엔진 체결은 배치/고빈도 → 락 대기로 성능 저하 |
| 낙관적 락 (`@Version`) | 충돌 시에만 실패 | 예외 기반 제어, JPA 변경 감지에 의존 |
| CAS UPDATE (`WHERE status = 'PENDING'`) | 단순, 원자적, 예외 없이 결과 확인 | — |

취소와 체결의 경합은 극히 드물고(사용자가 체결 직전에 취소 버튼을 누르는 경우), CAS UPDATE는 예외 없이 affected rows로 결과를 판단할 수 있어 가장 단순한 선택이다.

## 에러 처리

| 상황 | 처리 |
|------|------|
| 주문 없음 | `ORDER_NOT_FOUND` (404) |
| 소유권 불일치 | `ORDER_NOT_FOUND` (404) — 존재 여부 노출 방지 |
| CAS 실패 (이미 체결/취소/실패) | `ORDER_NOT_CANCELLABLE` (400) |
| 엔진 이벤트 전송 실패 | 무시 (엔진이 이후 체결을 시도해도 orders CAS 가드로 방어) |

## 시퀀스 다이어그램

```mermaid
sequenceDiagram
    participant Client
    participant Controller as OrderController
    participant Service as CancelOrderService
    participant OrderPort as OrderCommandPort
    participant MappingUC as FindExchangeCoinMappingUseCase
    participant ExchangeUC as FindExchangeDetailUseCase
    participant Balance as ManageWalletBalanceUseCase
    participant MySQL
    participant Publisher as EngineInboxPublisher
    participant MQ as RabbitMQ

    Client->>Controller: POST /api/orders/{orderId}/cancel {walletId}
    Controller->>Service: cancelOrder(command)

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 01 주문 조회 + 소유권 검증
    end
    Service->>OrderPort: findById(orderId)
    OrderPort->>MySQL: SELECT order
    OrderPort-->>Service: Order
    Note over Service: 소유권 검증 (walletId 일치)

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 02 CAS UPDATE (상태 전이)
    end
    Service->>OrderPort: cancelOrder(orderId)
    OrderPort->>MySQL: UPDATE orders SET status='CANCELLED' WHERE status='PENDING'

    alt CAS 실패 (이미 체결/취소)
        Note over Service: ORDER_NOT_CANCELLABLE 예외
    end

    rect rgb(60, 60, 60)
        Note over Service,Balance: STEP 03 잔고 unlock (CAS UPDATE)
    end
    Service->>MappingUC: findById(exchangeCoinId)
    MappingUC-->>Service: ExchangeCoinMappingResult

    alt 매수 주문
        Service->>ExchangeUC: findExchangeDetail(exchangeId)
        ExchangeUC-->>Service: ExchangeDetailResult
        Service->>Balance: unlockBalance(walletId, baseCurrencyCoinId, settlementDebit)
    else 매도 주문
        Service->>Balance: unlockBalance(walletId, coinId, quantity)
    end
    Balance->>MySQL: CAS UPDATE wallet_balance

    rect rgb(60, 60, 60)
        Note over Service: STEP 04 도메인 이벤트 발행
    end
    Service->>Service: publishEvent(OrderCanceledEvent)

    Service-->>Controller: Order
    Controller-->>Client: 200 OK {orderId, status: CANCELLED}

    rect rgb(60, 60, 60)
        Note over Publisher,MQ: AFTER_COMMIT — 엔진 오더북 제거
    end
    Publisher->>MQ: send(engine.inbox, event_type=OrderCanceled)
```

### CAS 경합 시

```mermaid
sequenceDiagram
    participant Client
    participant Engine as DbWriterThread (engine)
    participant Cancel as CancelOrderService (api)
    participant MySQL

    Note over Engine,Cancel: 동일 주문에 대해 동시 실행

    Engine->>MySQL: UPDATE orders SET status='FILLED' WHERE status='PENDING'
    MySQL-->>Engine: affected rows = 1 (CAS 성공)

    Cancel->>MySQL: UPDATE orders SET status='CANCELLED' WHERE status='PENDING'
    MySQL-->>Cancel: affected rows = 0 (CAS 실패)

    Note over Cancel: ORDER_NOT_CANCELLABLE (400)
    Cancel-->>Client: 400 ORDER_NOT_CANCELLABLE
```

## task 목록

- [ ] Order 도메인에 취소 상태 전이 추가
- [ ] 취소 UseCase와 서비스 구현(소유권 검증·원자적 전이·잔고 해제)
- [ ] 잔고 해제 대상 결정 연동(매핑·거래소 상세 조회)
- [ ] 주문 취소 이벤트 발행(확정 직후)
- [ ] 취소 REST 어댑터와 요청/응답 DTO

## API 명세

`POST /api/orders/{orderId}/cancel`

### Path Parameter

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| orderId | Long | O | 취소할 주문 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| walletId | Long | O | 지갑 ID (소유권 검증용) |

### Request

```
POST /api/orders/42/cancel

{
    "walletId": 1
}
```

### Response

```json
{
    "status": 200,
    "code": "OK",
    "message": "주문이 취소되었습니다.",
    "data": {
        "orderId": 42,
        "status": "CANCELLED"
    }
}
```

### 응답 필드 상세

| 필드 | 타입 | 설명 |
|------|------|------|
| orderId | Long | 취소된 주문 ID |
| status | String | 주문 상태 (`CANCELLED`) |

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| ORDER_NOT_FOUND | 404 | 주문을 찾을 수 없거나 소유권 불일치 |
| ORDER_NOT_CANCELLABLE | 400 | PENDING이 아닌 주문을 취소하려 함 (이미 FILLED, CANCELLED, FAILED) |

## 이벤트 컨트랙트

취소 확정 직후 주문 취소 이벤트를 `engine.inbox` 큐로 발행한다. 채널 규약과 페이로드(OrderCanceled)는 [../../../../docs/contracts/engine-inbox.md](../../../../docs/contracts/engine-inbox.md) 참조.
