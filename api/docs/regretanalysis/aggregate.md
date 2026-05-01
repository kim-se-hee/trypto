# Aggregate Root / Entity / Value Object

| Aggregate Root | Entity | Value Object |
|---|---|---|
| RegretReport | RuleImpact, ViolationDetail, ViolationDetails, AssetSnapshot, ViolatedOrder, ViolatedOrders | ImpactGap, ThresholdUnit, AssetTimeline, BtcBenchmark, BtcDailyPrice, BtcDailyPrices, CumulativeLossTimeline, ViolationMarkers, ViolationLossContext, AnalysisRound, AnalysisRoundStatus, AnalysisRule, AnalysisRules, AnalysisExchange, ActiveRoundExchange, TradeSide, OrderExecution, OrderExecutions, CurrentPrices, RuleBreach |

Strategy: `ViolationLossStrategy` (enum)

# 소유 관계

- RegretReport → RuleImpact, ViolationDetails
- RuleImpact → ImpactGap
- ViolationDetails → ViolationDetail
- ViolatedOrders → ViolatedOrder
- ViolatedOrder → ViolationLossStrategy, ViolationLossContext
- AssetTimeline → AssetSnapshot
- CumulativeLossTimeline → DailyLoss (inner record)
- ViolationMarkers → ViolationMarker (inner record)
- BtcBenchmark → BtcDailyPrice
- BtcDailyPrices → BtcDailyPrice
- AnalysisRules → AnalysisRule
- OrderExecutions → OrderExecution
