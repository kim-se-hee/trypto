# Aggregate Root / Entity / Value Object

| Aggregate Root | Entity | Value Object |
|---|---|---|
| Order, Holding, OrderFillFailure | RuleViolation | Side, OrderType, OrderStatus, OrderMode, Fee, Quantity, BalanceChange, OrderAmountPolicy, TradingVenue, RuleViolationRef, FilledOrder, FilledOrderCounts, CoinExchangeMapping, OrderFilledEvent, OrderFilledNotification, OrderPlacedEvent, MarketIdentifier, OrphanOrder, PriceCandidate, PriceCandidates, TradingContext |

# 소유 관계

- TradingVenue → OrderAmountPolicy
- Order → RuleViolation, MarketIdentifier
- PriceCandidates → PriceCandidate
