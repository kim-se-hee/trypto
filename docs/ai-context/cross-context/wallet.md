# wallet 크로스 컨텍스트 인터페이스

패키지: `ksh.tryptobackend.wallet.application.port.in`

## UseCase

### FindWalletUseCase
- `findById(Long walletId) → Optional<WalletResult>`
- `findByRoundIdAndExchangeId(Long roundId, Long exchangeId) → Optional<WalletResult>`
- `findByRoundId(Long roundId) → List<WalletResult>`
- `findByRoundIds(List<Long> roundIds) → List<WalletResult>`
- `findByExchangeId(Long exchangeId) → List<WalletResult>`

### GetAvailableBalanceUseCase
- `getAvailableBalance(Long walletId, Long coinId) → BigDecimal`

### CreateWalletWithBalanceUseCase
- `createWalletWithBalance(CreateWalletWithBalanceCommand command) → Long`

### ManageWalletBalanceUseCase
- `deductBalance(Long walletId, Long coinId, BigDecimal amount) → void`
- `addBalance(Long walletId, Long coinId, BigDecimal amount) → void`
- `lockBalance(Long walletId, Long coinId, BigDecimal amount) → void`
- `unlockBalance(Long walletId, Long coinId, BigDecimal amount) → void`

### FindDepositAddressUseCase
- `findByRoundIdAndChainAndAddress(Long roundId, String chain, String address) → Optional<DepositAddressResult>`

### ResolveTransferDestinationUseCase
- `resolveDestination(Long roundId, Long coinId, String chain, String toAddress, String toTag) → TransferDestinationResult`

### GetWalletOwnerIdUseCase
- `getWalletOwnerId(Long walletId) → Long`

## Command DTO

| DTO | 필드 |
|-----|------|
| CreateWalletWithBalanceCommand | roundId: Long, exchangeId: Long, baseCurrencyCoinId: Long, initialAmount: BigDecimal, createdAt: LocalDateTime |

## Result DTO

| DTO | 필드 |
|-----|------|
| WalletResult | walletId: Long, roundId: Long, exchangeId: Long, seedAmount: BigDecimal |
| DepositAddressResult | walletId: Long, chain: String, address: String, tag: String |
| TransferDestinationResult | walletId: Long, failureReason: String |
