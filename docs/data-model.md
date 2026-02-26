# 데이터 모델

## 도메인별 Aggregate 구조

| 도메인 | Aggregate Root | Entity | Value Object |
|--------|---------------|--------|--------------|
| Identity | User | — | Email, Password |
| Wallet | Wallet, Transfer | WalletBalance | WalletAddress, TransferStatus, Chain |
| Trading | Order, Swap | Holding, RuleViolation | Side, OrderType, OrderStatus, Fee, Slippage, ViolationReason, OrderAmountPolicy, TradingVenue, RuleType |
| MarketData | Coin, Exchange | ExchangeCoin, PriceCandle, WithdrawalFee | — |
| Portfolio | PortfolioSnapshot | Ranking, SnapshotDetail | ProfitRate, AvgBuyPrice, TotalBuyAmount, RankingPeriod |
| InvestmentRound | InvestmentRound | InvestmentRule, EmergencyFunding | SeedPolicy, RoundStatus, RuleType, RuleValue |
| RegretAnalysis | RegretReport | RuleScenario, ViolationTrade | AssetHistory, ImpactGap |

**소유 관계:**
- WalletBalance → WalletAddress
- Transfer → TransferStatus, Chain
- TradingVenue → OrderAmountPolicy
- Order → Side, OrderType, OrderStatus, Fee, RuleViolation
- RuleViolation → ViolationReason
- Exchange → ExchangeCoin, WithdrawalFee
- ExchangeCoin → PriceCandle
- Swap → Fee, Slippage
- SnapshotDetail → AvgBuyPrice, TotalBuyAmount
- InvestmentRule → RuleType, RuleValue
- Ranking → RankingPeriod
- RuleScenario → ImpactGap

## 모듈 간 의존

| From → To | 참조 ID | 용도 |
|-----------|---------|------|
| InvestmentRound → Wallet | roundId | InvestmentRound 1:N Wallet |
| Wallet → MarketData | exchangeId, coinId | 출금 수수료 조회 |
| Trading → Wallet | walletId | 잔고 검증, 잔고 반영 |
| Trading → MarketData | — | 현재가 조회 |
| Trading → InvestmentRound | roundId | 투자 원칙 위반 검증 |
| Portfolio → Wallet | userId | 잔고 조회 |
| Portfolio → MarketData | — | 현재가 조회 |
| Portfolio → InvestmentRound | roundId | 랭킹 참여 자격 판단 |
| RegretAnalysis → Trading | orderId | 원칙 위반 주문 체결 이력 조회 |
| RegretAnalysis → MarketData | — | 시세 조회 |
| RegretAnalysis → InvestmentRound | roundId | 투자 원칙 조회 |