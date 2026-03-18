# marketdata 크로스 컨텍스트 인터페이스

패키지: `ksh.tryptobackend.marketdata.application.port.in`

## UseCase

### FindExchangeDetailUseCase
- `findExchangeDetail(Long exchangeId) → Optional<ExchangeDetailResult>`

### FindExchangeSummaryUseCase
- `findExchangeSummary(Long exchangeId) → Optional<ExchangeSummaryResult>`

### FindExchangeNamesUseCase
- `findExchangeNames(Set<Long> exchangeIds) → Map<Long, String>`

### FindCoinInfoUseCase
- `findByIds(Set<Long> coinIds) → Map<Long, CoinInfoResult>`

### FindCoinSymbolsUseCase
- `findSymbolsByIds(Set<Long> coinIds) → Map<Long, String>`

### FindExchangeCoinMappingUseCase
- `findById(Long exchangeCoinId) → Optional<ExchangeCoinMappingResult>`
- `findExchangeCoinIdMap(Long exchangeId, List<Long> coinIds) → Map<Long, Long>`

### FindExchangeCoinChainUseCase
- `findByExchangeIdAndCoinIdAndChain(Long exchangeId, Long coinId, String chain) → Optional<ExchangeCoinChainResult>`

### FindWithdrawalFeeUseCase
- `findByExchangeIdAndCoinIdAndChain(Long exchangeId, Long coinId, String chain) → Optional<WithdrawalFeeResult>`

### GetLivePriceUseCase
- `getCurrentPrice(Long exchangeCoinId) → BigDecimal`

### GetLivePricesUseCase
- `getCurrentPrices(Set<Long> exchangeCoinIds) → Map<Long, BigDecimal>`

### ResolveExchangeCoinMappingUseCase
- `resolve(String exchange, String symbol) → Optional<Long>` (exchangeCoinId)

### WarmupExchangeCoinMappingUseCase
- `warmup()` — 거래소-코인 매핑 캐시 워밍업 (내부 사용, 크로스 컨텍스트 호출 없음)

### ResolveLiveTickerUseCase
- `resolve(String exchange, String symbol, BigDecimal currentPrice, BigDecimal changeRate, BigDecimal quoteTurnover, Long timestamp) → Optional<LiveTickerResult>` — 내부 사용, 크로스 컨텍스트 호출 없음

## Result DTO

| DTO | 필드 |
|-----|------|
| ExchangeDetailResult | name: String, baseCurrencyCoinId: Long, domestic: boolean, feeRate: BigDecimal |
| ExchangeSummaryResult | exchangeId: Long, name: String, baseCurrencySymbol: String |
| CoinInfoResult | symbol: String, name: String |
| ExchangeCoinMappingResult | exchangeCoinId: Long, exchangeId: Long, coinId: Long |
| ExchangeCoinChainResult | tagRequired: boolean |
| WithdrawalFeeResult | fee: BigDecimal, minWithdrawal: BigDecimal |
| LiveTickerResult | exchangeId: Long, coinId: Long, symbol: String, price: BigDecimal, changeRate: BigDecimal, quoteTurnover: BigDecimal, timestamp: Long |
