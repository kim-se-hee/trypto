## 멱등성

전역 멱등성 메커니즘 [../../idempotency.md](../../idempotency.md) 를 따른다. 


### 동시 재요청 (레이스)

- 키 UNIQUE 인덱스 하나가 정합성까지 보장한다. 동일 키 동시 두 요청 중 하나만 선점에 성공하고, 진 쪽은 승자 커밋까지 블록됐다가 중복으로 깨어나 **비즈니스 로직을 아예 실행하지 않는다** (fail-fast, 라운드·잔고 락 경합 없음). 하나만 성공하는 것은 애플리케이션이 아니라 DB 엔진이 보장한다.
- 중복 키 위반만 골라 "중복 요청" 도메인 신호로 번역하고, 그 외 무결성 오류는 그대로 전파해 오탐을 막는다. 이 번역은 영속성 경계 안에서만 일어나고 도메인·애플리케이션 계층은 인프라 예외를 보지 않는다.
- 선점·연결·이력 저장이 같은 트랜잭션이라, 중간 실패 시 선점 행도 함께 롤백되어 키가 풀린다 → "처리 중 잠김" 없이 재시도 가능하다.

### 중복 응답 처리

- catch 후 컨트롤러가 되돌려줄 값은 **현재 잔여 충전 횟수(`remainingChargeCount`)** 다. 이를 위해 컨트롤러가 **기존 라운드 조회 UseCase(`FindRoundInfoUseCase.findById`) 를 호출해** 그 값(`RoundInfoResult.emergencyChargeCount()`)을 읽는다.
- 이 조회는 라운드 개요 투영(`RoundOverview`)만 읽는 경량 조회라 애그리거트 전체를 로드하지 않는다. 긴급 충전 전용 조회 UseCase 를 **따로 구현하지 않고 기존 것을 재사용**한다.
- 최초 처리·재요청 모두 `200 OK`.

## 충전 횟수 차감 동시성

**멱등성만으로는 이중 충전을 막지 못한다.** 멱등성 테이블은 *같은 키*의 재요청만 걸러낸다.
서로 **다른 멱등키**를 가진 두 정당한 요청이 같은 라운드에 동시에 들어오면 키 UNIQUE 에 걸리지 않고 둘 다 잔여 횟수를 차감한다.
동시성은 **비관적 락으로 라운드 행을 잠궈서 처리한다.**

## 트랜잭션 경계

성공한 긴급 충전은 세 가지를 하나의 `@Transactional` 안에서 원자적으로 반영한다.

1. 라운드 잔여 횟수 차감 (investmentround)
2. 대상 지갑 잔고 증가 (wallet)
3. 긴급 충전 이력 INSERT (investmentround)

컨텍스트는 다르지만 같은 트랜잭션으로 커밋한다.

## 잔고 레코드 전제

지갑을 생성할 때 모든 코인에 대한 잔고 레코드가 0 으로 함께 선생성된다. 따라서 긴급 충전 처리 중 (지갑, 기축통화 코인) 잔고 레코드가 DB 에 없는 경우는 고려하지 않는다. 잔고 증가 시 레코드가 없어 새로 삽입하거나(upsert) 빈 값을 다루는 분기가 필요 없다.

## 인수테스트 셋업 전제

**라운드 생성은 긴급 충전 범위 밖의 별도 기능이다.** 이 기능은 라운드 생성 API 를 구현하지 않으며, 인수테스트도 그 API 에 의존하지 않는다.

- 인수테스트의 배경("활성 라운드가 존재한다", "긴급 자금이 비활성인 라운드가 존재한다")은 라운드·지갑·잔고를 **JPA 리포지토리로 직접 픽스쳐를 만든다**.
- **검증 대상 HTTP 엔드포인트는 `POST /api/rounds/{roundId}/emergency-funding` 하나뿐이다.**

## API 명세

`POST /api/rounds/{roundId}/emergency-funding`

### Path Parameter

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| roundId | Long | O | 라운드 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | O | 사용자 ID (소유자 검증용) |
| exchangeId | Long | O | 투입 대상 거래소 ID (서버가 내부 지갑을 결정) |
| amount | BigDecimal | O | 긴급 자금 투입 금액 |
| idempotencyKey | UUID | O | 멱등성 키 (클라이언트 생성) |

```json
{
  "userId": 1,
  "exchangeId": 2,
  "amount": 300000,
  "idempotencyKey": "7f1f53b3-f9f6-4d9e-8fd8-fca6f5ff9c13"
}
```

### Response

최초 처리와 동일 키 재요청 모두 `200 OK`. 사용자에게 멱등 중복을 노출하지 않기 위해 상태 코드로 두 경로를 구분하지 않는다 — 재요청도 그냥 "성공" 으로 보인다. (201 은 재요청 시 아무 리소스도 생성되지 않아 부정확하므로 쓰지 않는다.)

| 필드 | 타입 | 설명 |
|------|------|------|
| roundId | Long | 라운드 ID |
| exchangeId | Long | 투입 대상 거래소 ID |
| chargedAmount | BigDecimal | 투입 금액 |
| remainingChargeCount | int | 잔여 투입 횟수 (최초=차감 후, 재요청=현재값) |

> 투입 시각(`chargedAt`)은 감사·랭킹 계산을 위해 `emergency_funding` 이력 테이블에는 기록하지만, 클라이언트가 되돌려받아 쓸 일이 없어 **응답에는 포함하지 않는다.**

```json
{
  "status": 200,
  "code": "OK",
  "message": "긴급 자금을 투입했습니다.",
  "data": {
    "roundId": 1,
    "exchangeId": 2,
    "chargedAmount": 300000,
    "remainingChargeCount": 1
  }
}
```

### 에러 응답

| code | status | 설명 |
|------|--------|------|
| ROUND_NOT_FOUND | 404 | 라운드를 찾을 수 없음 |
| ROUND_ACCESS_DENIED | 403 | 본인 라운드가 아님 |
| ROUND_NOT_ACTIVE | 404 | 진행 중인 라운드가 아님 |
| EMERGENCY_FUNDING_DISABLED | 400 | 긴급 자금 기능이 비활성(상한 0) |
| EMERGENCY_FUNDING_CHANCE_EXHAUSTED | 400 | 잔여 긴급 투입 횟수가 없음 |
| INVALID_EMERGENCY_FUNDING_AMOUNT | 400 | 투입 금액이 0 이하이거나 상한 초과 |
| WALLET_NOT_FOUND | 404 | 해당 라운드/거래소 지갑이 없음 |
