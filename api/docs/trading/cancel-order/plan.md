## 엔진 이벤트 발행

트랜잭션 커밋 직후 `OrderCanceledEvent` 를 `ApplicationEventPublisher` 로 발행한다. `EngineInboxPublisher` 가 `@TransactionalEventListener(phase = AFTER_COMMIT)` 로 받아 `engine.inbox` 큐에 `event_type=OrderCanceled` 로 전송한다.

- **AFTER_COMMIT 이유**: DB 가 CANCELED 로 확정되기 전에 엔진이 오더북에서 제거해버리면, 엔진 쪽 취소는 반영됐는데 DB 롤백이 일어나 상태가 어긋날 수 있다.
- **전송 실패 처리**: 엔진 전송이 실패해도 DB 상태는 이미 CANCELED 이므로 사용자 응답에는 영향이 없다. 엔진이 해당 주문을 잘못 체결하려 해도 `orders` UPDATE 의 `WHERE status='PENDING'` 가드에 걸려 skip 되므로 이중 체결은 방어된다. 현재는 warn 로그만 남긴다.

## 트랜잭션 범위

주문 취소(비관적 락으로 점거한 뒤 상태 변경)와 잔고 unlock은 반드시 같은 트랜잭션에서 수행한다. 주문은 CANCELED인데 잔고가 lock된 상태를 방지한다.


## 매칭과의 동시성 제어

취소와 엔진 체결이 동일 `orders` 행을 동시에 갱신하려 할 수 있다. 두 서비스는 각각의 점유 방식으로 행을 배타적으로 잠궈 취소와 체결을 직렬화한다.

- **취소** 비관적락으로 행을 잠근 뒤 잠금
- **체결** 원자적인 update 쿼리로 주문 행 잠금 

두 갱신 모두 같은 행의 배타 잠금을 잡아야 하므로 동시에 진행할 수 없다. 먼저 잠금을 잡고 커밋한 쪽이 이기고, 진 쪽은 각자의 가드에서 걸러진다.

- 엔진 체결이 먼저 커밋되면: 취소가 잠금 획득 후 FILLED 확인 → `ORDER_NOT_CANCELLABLE` (400)
- 취소가 먼저 커밋되면: 엔진 CAS가 `WHERE status='PENDING'`에 0건 매칭 → 해당 체결 skip (잔고·아웃박스·holding 단계 모두 진행 안 함)

## 에러 처리

| 상황 | 처리 |
|------|------|
| 주문 없음 | `ORDER_NOT_FOUND` (404) |
| 소유권 불일치 | `ORDER_NOT_FOUND` (404) — 존재 여부 노출 방지 |
| 이미 체결/취소/실패 (PENDING 아님) | `ORDER_NOT_CANCELLABLE` (400) |
| 엔진 이벤트 전송 실패 | 무시 (엔진이 이후 체결을 시도해도 orders CAS 가드로 방어) |

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
        "status": "CANCELED"
    }
}
```

### 응답 필드 상세

| 필드 | 타입 | 설명 |
|------|------|------|
| orderId | Long | 취소된 주문 ID |
| status | String | 주문 상태 (`CANCELED`) |

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| ORDER_NOT_FOUND | 404 | 주문을 찾을 수 없거나 소유권 불일치 |
| ORDER_NOT_CANCELLABLE | 400 | PENDING이 아닌 주문을 취소하려 함 (이미 FILLED, CANCELED, FAILED) |

## 이벤트 컨트랙트

취소 확정 직후 주문 취소 이벤트를 `engine.inbox` 큐로 발행한다. 채널 규약과 페이로드(OrderCanceled)는 [../../../../docs/contracts/engine-inbox.md](../../../../docs/contracts/engine-inbox.md) 참조.
