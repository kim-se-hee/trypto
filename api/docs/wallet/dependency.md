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

## CreateWalletWithBalanceUseCase
- `createWalletWithBalance(CreateWalletWithBalanceCommand command) → Long`
- Command `CreateWalletWithBalanceCommand { roundId: Long, exchangeId: Long, baseCurrencyCoinId: Long, initialAmount: BigDecimal, createdAt: LocalDateTime }`

## ManageWalletBalanceUseCase
- `deductBalance(Long walletId, Long coinId, BigDecimal amount) → void`
- `addBalance(Long walletId, Long coinId, BigDecimal amount) → void`
- `lockBalance(Long walletId, Long coinId, BigDecimal amount) → void`
- `unlockBalance(Long walletId, Long coinId, BigDecimal amount) → void`

## GetWalletOwnerIdUseCase
- `getWalletOwnerId(Long walletId) → Long`

## TransferCoinUseCase
- `transferCoin(TransferCoinCommand command) → Transfer`
- 거래소 간 송금 실행 (검증 → 실패 판정 → 잔고 변동 → 저장)

## FindTransferHistoryUseCase
- `findTransferHistory(FindTransferHistoryQuery query) → TransferHistoryCursorResult`
- 지갑 기준 송금 내역(입금/출금) 커서 조회

# 의존

## MarketData
- `FindExchangeDetailUseCase` — 거래소 정보 (입금 주소 발급)
- `FindCoinInfoUseCase` — 기축통화 심볼 조회 (잔고 조회)
- `FindCoinSymbolsUseCase` — 코인 심볼 조회 (송금 내역 응답 보강)

## InvestmentRound
- `FindRoundInfoUseCase` — 잔고 조회 시 소유권 검증
