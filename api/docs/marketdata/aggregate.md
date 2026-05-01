# Aggregate Root / Entity / Value Object

| Aggregate Root | Entity | Value Object |
|---|---|---|
| Exchange, Coin, ExchangeCoin | ExchangeCoinChain, WithdrawalFee | ExchangeMarketType, Candle, CandleFilter, CandleInterval, CoinSymbols, DailyClosePrice, ExchangeCoinIdMap, ExchangeCoinMapping, ExchangeConfig, ExchangeSummary, ExchangeSymbolKey, LivePrices, MarketMetaEntry, Tick, TickerSnapshot, TickerSnapshots |

# 소유 관계

- TickerSnapshots → TickerSnapshot
