# 제공

패키지: `ksh.tryptobackend.wallet.application.port.in`

## FindWalletUseCase
- `findById(Long walletId) → Optional<WalletResult>`
- `findByRoundIdAndExchangeId(Long roundId, Long exchangeId) → Optional<WalletResult>`
- `findByRoundId(Long roundId) → List<WalletResult>`
- `findByRoundIds(List<Long> roundIds) → List<WalletResult>`
- `findByExchangeId(Long exchangeId) → List<WalletResult>`
- Returns `WalletResult { walletId: Long, roundId: Long, exchangeId: Long, seedAmount: BigDecimal }`

## GetAvailableBalanceUseCase
- `getAvailableBalance(Long walletId, Long coinId) → BigDecimal`
- `getTotalBalance(Long walletId, Long coinId) → BigDecimal`

## CreateWalletWithBalanceUseCase
- `createWalletWithBalance(CreateWalletWithBalanceCommand command) → Long`
- Command `CreateWalletWithBalanceCommand { roundId: Long, exchangeId: Long, baseCurrencyCoinId: Long, initialAmount: BigDecimal, createdAt: LocalDateTime }`
- 지갑과 함께 거래소의 기축통화·전 거래 코인 잔고를 0으로 사전 생성하고 기축통화에 시드를 예치한다

## ApplyBalanceChangesUseCase
- `applyBalanceChanges(Long walletId, List<BalanceChangeItem> changes) → void`
- `BalanceChangeItem { type: BalanceChangeType, coinId: Long, amount: BigDecimal }`
- `BalanceChangeType`: ADD_AVAILABLE, LOCK, UNLOCK, CONSUME_LOCKED
- 한 트랜잭션에서 관련 잔고를 coinId 오름차순으로 잠근 뒤 항목을 순서대로 반영한다

## GetWalletOwnerIdUseCase
- `getWalletOwnerId(Long walletId) → Long`

## TransferCoinUseCase
- `transferCoin(TransferCoinCommand command) → Transfer`
- 거래소 간 송금 실행 (멱등 확인 → 검증 → 잔고 잠금 조회 → 도메인 서비스가 잔고 이동 → 저장)

## FindTransferHistoryUseCase
- `findTransferHistory(FindTransferHistoryQuery query) → TransferHistoryCursorResult`
- 지갑 기준 송금 내역(입금/출금) 커서 조회

# 의존

## MarketData
- `FindCoinInfoUseCase` — 기축통화 심볼 조회 (잔고 조회)
- `FindCoinSymbolsUseCase` — 코인 심볼 조회 (송금 내역 응답 보강)
- `FindExchangeCoinsUseCase` — 거래소 코인 목록 조회 (지갑 생성 시 잔고 사전 생성)

## InvestmentRound
- `FindRoundInfoUseCase` — 잔고 조회 시 소유권 검증
