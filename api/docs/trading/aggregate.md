# Aggregate Root / Entity / Value Object

| Aggregate Root | Entity | Value Object |
|---|---|---|
| Order, Holding, RuleViolation, OrderFillFailure | — | Side, OrderType, OrderStatus, OrderMode, Quantity, Price, Money, Fee, Fill, ExecutedFill, BalanceChange, ExchangeInfo, MarketInfo, TradingPair, MarketIdentifier, OrderAmountPolicy, RuleViolationRef, FilledOrder, FilledOrderCounts, CoinExchangeMapping, OrderFilledNotification, OrphanOrder, PriceCandidate, PriceCandidates |

# 소유 관계

- MarketInfo → TradingPair, ExchangeInfo
- PriceCandidates → PriceCandidate

# 도메인 이벤트

- Order → OrderPlacedEvent, OrderFilledEvent, OrderCanceledEvent
