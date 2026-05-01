# Aggregate Root / Entity / Value Object

| Aggregate Root | Entity | Value Object |
|---|---|---|
| Ranking | — | RankingPeriod, RoundKey, RankingCandidate, RankingCandidates, EligibleRound, EligibleRounds, SnapshotSummary, SnapshotSummaries, RoundTradeCounts, ExchangeNames, CoinSymbols, RankingSummary, RankingStats |

# 소유 관계

- Ranking → RankingPeriod
- RankingCandidates → RankingCandidate
- EligibleRounds → EligibleRound
- SnapshotSummaries → SnapshotSummary
