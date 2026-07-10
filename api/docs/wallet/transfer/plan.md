## 멱등성

전역 멱등성 메커니즘 [../../idempotency.md](../../idempotency.md) 를 따른다.


### 동시 재요청 (레이스)

- 키 UNIQUE 인덱스 하나가 정합성까지 보장한다. 동일 키 동시 두 요청 중 하나만 선점에 성공하고, 진 쪽은 승자 커밋까지 블록됐다가 중복으로 깨어나 **비즈니스 로직을 아예 실행하지 않는다** (fail-fast, 출발·도착 지갑 잔고 락 경합 없음). 하나만 성공하는 것은 애플리케이션이 아니라 DB 엔진이 보장한다.
- 중복 키 위반만 골라 "중복 요청" 도메인 신호로 번역하고, 그 외 무결성 오류는 그대로 전파해 오탐을 막는다. 이 번역은 영속성 경계 안에서만 일어나고 도메인·애플리케이션 계층은 인프라 예외를 보지 않는다.
- 선점·연결·이력 저장이 같은 트랜잭션이라, 중간 실패 시 선점 행도 함께 롤백되어 키가 풀린다 → "처리 중 잠김" 없이 재시도 가능하다.

### 중복 응답 처리

- catch 후 컨트롤러가 되돌려줄 값은 **최초 처리가 생성한 송금 ID(`transferId`)** 다. 이 값은 멱등 테이블의 "연결" 단계에서 선점 행의 리소스 ID 에 채워둔 그 송금 ID 다 — 컨트롤러가 **멱등 키로 멱등 테이블을 읽어 리소스 ID(=`transferId`) 를 되읽는다.** (긴급 충전은 리소스 ID 를 되읽지 않고 별도 라운드 상태를 조회하지만, 송금은 정반대로 연결해 둔 리소스 ID 자체가 재요청 응답값이다.)
- 재요청은 상태를 바꾸지 않으므로(선점 단계에서 걸려 롤백) 이 조회는 읽기 전용이며, 송금 애그리거트 전체를 로드하지 않고 멱등 테이블 한 행만 읽는다.
- 최초 처리·재요청 모두 `201 CREATED`, `data` 는 `{ transferId, status: SUCCESS }` 로 동일하다. 재요청도 그냥 "송금 성공" 으로 보이며, 상태 코드로 두 경로를 구분하지 않는다. (송금은 리소스 생성 커맨드이므로 최초 성공 코드가 201 이고, 재요청도 같은 규칙에 따라 201 로 맞춘다.)

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
