# Aggregate Root / Entity / Value Object

| Aggregate Root | Entity | Value Object |
|---|---|---|
| PortfolioSnapshot | SnapshotDetail, EvaluatedHolding | ActiveRound, ActiveRounds, ExchangeSnapshot, KrwConversionRate, WalletSnapshot, WalletSnapshots, EvaluatedHoldings, PortfolioHolding, PortfolioHoldings, CoinSnapshot, CoinSnapshotMap, HoldingSnapshot, HoldingSummary, SnapshotOverview, UserSnapshotSummary |

# 소유 관계

- PortfolioSnapshot → SnapshotDetail
- EvaluatedHoldings → EvaluatedHolding
- PortfolioHoldings → PortfolioHolding
- ActiveRounds → ActiveRound
- WalletSnapshots → WalletSnapshot
- CoinSnapshotMap → CoinSnapshot
