# 제공

패키지: `ksh.tryptobackend.marketdata.application.port.in`

## FindExchangeDetailUseCase
- `findExchangeDetail(Long exchangeId) → Optional<ExchangeDetailResult>`
- Returns `ExchangeDetailResult { name: String, baseCurrencyCoinId: Long, domestic: boolean, feeRate: BigDecimal }`
- `findExchangeIdByName(String name) → Optional<Long>`

## FindExchangeNamesUseCase
- `findExchangeNames(Set<Long> exchangeIds) → Map<Long, String>`

## FindCoinInfoUseCase
- `findByIds(Set<Long> coinIds) → Map<Long, CoinInfoResult>`
- Returns `CoinInfoResult { symbol: String, name: String }`

## FindCoinSymbolsUseCase
- `findSymbolsByIds(Set<Long> coinIds) → Map<Long, String>`

## FindExchangeCoinMappingUseCase
- `findById(Long exchangeCoinId) → Optional<ExchangeCoinMappingResult>`
- `findExchangeCoinMappings(Long exchangeId, List<Long> coinIds) → List<ExchangeCoinMappingResult>`
- Returns `ExchangeCoinMappingResult { exchangeCoinId: Long, exchangeId: Long, coinId: Long, suspended: boolean }`

## GetLivePriceUseCase
- `getCurrentPrice(Long exchangeCoinId) → BigDecimal`

## GetPriceChangeRateUseCase
- `getChangeRate(Long exchangeCoinId) → BigDecimal` — 추격매수 위반 검증용 단기 변동률

## FindTicksUseCase
- `findTicks(String exchangeName, String marketSymbol, Instant from, Instant to) → List<TickResult>`
- Returns `TickResult { time: Instant, price: BigDecimal }`

## FindBtcDailyPricesUseCase
- `findBtcDailyPrices(LocalDate startDate, LocalDate endDate, String currency) → List<BtcDailyPriceResult>` — 후회 차트 BTC 벤치마크

## GetMarketStatusUseCase
- `isSuspended(Long exchangeCoinId) → boolean` — 상장 코인의 거래지원 종료 여부

## GetCoinMarketStatusUseCase
- `isSuspended(Long exchangeId, Long coinId) → boolean` — 거래소·코인 기준 거래지원 종료 여부

## FindSuspendedExchangeCoinIdsUseCase
- `findSuspendedExchangeCoinIds() → List<Long>` — 거래지원 종료 상태인 상장 코인 식별자 목록

# 의존

다른 컨텍스트에 의존하지 않는다.
