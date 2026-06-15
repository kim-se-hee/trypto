## 선행 구현 사항

- 입금 주소 조회 API (`GET /api/wallets/{walletId}/deposit-address`) — `docs/wallet/deposit-address/index.md`
- 잔고 관리 (available/locked) — `docs/trading/place-order/index.md`

## 도메인 모델

### Transfer (Aggregate Root)

```
Transfer (Aggregate Root)
├── transferId: Long
├── idempotencyKey: UUID
├── fromWalletId: Long
├── toWalletId: Long (nullable — FROZEN일 때 null 가능)
├── coinId: Long
├── chain: Chain (VO)
├── toAddress: String
├── toTag: String (nullable)
├── amount: BigDecimal
├── fee: BigDecimal
├── status: TransferStatus (SUCCESS / FROZEN / REFUNDED)
├── failureReason: TransferFailureReason (WRONG_ADDRESS / WRONG_CHAIN / MISSING_TAG, nullable)
├── frozenUntil: LocalDateTime (nullable)
└── createdAt: LocalDateTime
```

**팩토리 메서드:**
- `Transfer.success(...)` — 성공 송금 생성
- `Transfer.frozen(..., failureReason)` — 동결 송금 생성 (frozenUntil = now + 24h)

**상태 전이:**
- `refund()` — FROZEN → REFUNDED (배치에서 호출)

## 타 컨텍스트 의존성

### 크로스 컨텍스트 포트

| 컴포넌트 | 방향 | 책임 |
|----------|------|------|
| TransferWalletPort | transfer → wallet | 지갑 조회, 잔고 차감/추가/잠금 |
| TransferDepositPort | transfer → wallet | 입금 주소로 지갑 역조회 (라운드 내) |
| WithdrawalFeePort | transfer → marketdata | 수수료 + 최소 출금 수량 조회 |
| ExchangeCoinChainPort | transfer → marketdata | 체인 지원 확인, 태그 필수 여부 조회 |

## 같은 컨텍스트 내부 의존

### Input Port (transfer 컨텍스트)

| 컴포넌트 | 책임 |
|----------|------|
| TransferCoinUseCase | 송금 유스케이스 |
| TransferCoinService | 송금 오케스트레이션 (검증 → 실패 판정 → 잔고 변동 → 저장) |

### Output Port (transfer 컨텍스트)

| 컴포넌트 | 책임 |
|----------|------|
| TransferPersistencePort | 송금 저장, 멱등 키 조회 |

## 시퀀스 플로우

```mermaid
sequenceDiagram
    participant Client
    participant Controller as TransferController
    participant Service as TransferCoinService
    participant TransferAdapter as TransferPersistenceAdapter
    participant WalletAdapter as TransferWalletAdapter
    participant DepositAdapter as TransferDepositAdapter
    participant FeeAdapter as WithdrawalFeeAdapter
    participant ChainAdapter as ExchangeCoinChainAdapter
    participant Transfer
    participant MySQL

    Client->>Controller: POST /api/transfers
    Controller->>Service: transferCoin(command)

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 01 멱등성 검사
    end
    Service->>TransferAdapter: findByIdempotencyKey(clientTransferId)
    TransferAdapter->>MySQL: SELECT by idempotency_key

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 02 지갑 조회 + 소유권 검증
    end
    Service->>WalletAdapter: getWallet(fromWalletId)
    WalletAdapter->>MySQL: SELECT wallet

    rect rgb(60, 60, 60)
        Note over Service: STEP 03 KRW 송금 불가 확인
    end
    Note over Service: baseCurrencyCoinId == coinId이면서 KRW이면 에러

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 04 출발 거래소 체인 지원 확인
    end
    Service->>ChainAdapter: getExchangeCoinChain(exchangeId, coinId, chain)
    ChainAdapter->>MySQL: SELECT exchange_coin_chain

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 05 수수료 + 최소 출금 수량 조회
    end
    Service->>FeeAdapter: getWithdrawalFee(exchangeId, coinId, chain)
    FeeAdapter->>MySQL: SELECT withdrawal_fee
    Note over Service: amount < minWithdrawal이면 에러

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 06 잔고 검증
    end
    Service->>WalletAdapter: getAvailableBalance(fromWalletId, coinId)
    WalletAdapter->>MySQL: SELECT available
    Note over Service: available < amount + fee이면 에러

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 07 실패 판정
    end
    Service->>DepositAdapter: findWalletByAddress(roundId, chain, toAddress)
    DepositAdapter->>MySQL: SELECT deposit_address + wallet

    alt 주소 불일치
        Service->>Transfer: Transfer.frozen(WRONG_ADDRESS)
    else 도착 거래소 체인 미지원
        Service->>ChainAdapter: findExchangeCoinChain(toExchangeId, coinId, chain)
        Note over Service: 미지원이면 FROZEN(WRONG_CHAIN)
        Service->>Transfer: Transfer.frozen(WRONG_CHAIN)
    else 태그 누락
        Note over Service: tagRequired && toTag 비어있음
        Service->>Transfer: Transfer.frozen(MISSING_TAG)
    else 모두 통과
        Service->>Transfer: Transfer.success(...)
    end

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 08 잔고 변동
    end
    alt SUCCESS
        Service->>WalletAdapter: deductAvailable(fromWalletId, coinId, amount + fee)
        Service->>WalletAdapter: addAvailable(toWalletId, coinId, amount)
    else FROZEN
        Service->>WalletAdapter: lock(fromWalletId, coinId, amount + fee)
    end
    WalletAdapter->>MySQL: UPDATE wallet_balance

    rect rgb(60, 60, 60)
        Note over Service,MySQL: STEP 09 송금 저장
    end
    Service->>TransferAdapter: save(transfer)
    TransferAdapter->>MySQL: INSERT transfer

    Service-->>Controller: Transfer
    Controller-->>Client: 201 Created
```

## task 목록

- [ ] Transfer Aggregate 도메인 모델 정의(success/frozen 팩토리, refund 상태 전이)
- [ ] 송금 UseCase와 서비스 구현(검증 단계 → 실패 판정 → 잔고 변동 → 저장 오케스트레이션)
- [ ] 멱등성 검사 연동(clientTransferId 기존 송금 조회)
- [ ] 출발 지갑 조회·소유권 검증 연동
- [ ] 출발 거래소 체인 지원·수수료·최소 출금 수량 조회 연동
- [ ] 잔고 검증 및 결과별 잔고 변동 연동(SUCCESS 차감/추가, FROZEN 잠금)
- [ ] 입금 주소 역조회·체인 호환·태그 검증으로 실패 판정 구현
- [ ] 송금 REST 어댑터와 요청/응답 DTO

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
