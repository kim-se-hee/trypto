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

# 의존

## MarketData
- `FindExchangeDetailUseCase` — 거래소 정보 (입금 주소 발급)
- `FindCoinInfoUseCase` — 기축통화 심볼 조회 (잔고 조회)

## InvestmentRound
- `FindRoundInfoUseCase` — 잔고 조회 시 소유권 검증
