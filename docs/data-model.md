# 데이터 모델

## 도메인별 Aggregate 구조

| 도메인 | Aggregate Root | Entity | Value Object |
|--------|---------------|--------|--------------|
| Wallet | Wallet | WalletBalance, DepositAddress | DepositTargetExchange |
| Transfer | Transfer | — | TransferStatus, TransferType, TransferFailureReason, TransferBalanceChange, TransferDestination, TransferDestinationChain, TransferSourceExchange, WithdrawalCondition, TransferWallet, TransferDepositAddress |
| Trading | Order, Holding | RuleViolation | Side, OrderType, OrderStatus, Fee, Quantity, BalanceChange, OrderAmountPolicy, TradingVenue, ListedCoinRef, RuleViolationRef, ViolationRule, ViolationRules, ViolationCheckContext |
| MarketData | Exchange | ExchangeCoinChain, WithdrawalFee | ExchangeMarketType |
| Portfolio | PortfolioSnapshot | SnapshotDetail, EvaluatedHolding | ActiveRound, ExchangeSnapshot, KrwConversionRate, WalletSnapshot, EvaluatedHoldings |
| Ranking | Ranking | — | RankingPeriod, RoundKey, RankingCandidate, RankingCandidates, EligibleRound, EligibleRounds, SnapshotSummary, SnapshotSummaries, RankerHolding |
| InvestmentRound | InvestmentRound | RuleSetting, EmergencyFunding | RoundStatus, SeedAmountPolicy, SeedAllocation, SeedAllocations, SeedFundingSpec |
| RegretAnalysis | RegretReport | RuleImpact, ViolationDetail, AssetSnapshot, ViolatedOrder, ViolationDetails | ImpactGap, ThresholdUnit, AssetTimeline, BtcBenchmark, BtcDailyPrice, CumulativeLossTimeline, ViolationMarkers, ViolationLossStrategy, ViolationLossContext, AnalysisRound, AnalysisRoundStatus, AnalysisRule, AnalysisRules, AnalysisExchange, ActiveRoundExchange, TradeSide, OrderExecution, RuleBreach |
| Common (Shared Kernel) | — | — | RuleType, ProfitRate |

**소유 관계:**
- Wallet → WalletBalance, DepositAddress
- Transfer → TransferBalanceChange, TransferDestination
- TradingVenue → OrderAmountPolicy
- Order → RuleViolation
- ViolationRules → ViolationRule
- Exchange → ExchangeCoinChain, WithdrawalFee
- PortfolioSnapshot → SnapshotDetail
- EvaluatedHoldings → EvaluatedHolding
- RankingCandidates → RankingCandidate
- EligibleRounds → EligibleRound
- SnapshotSummaries → SnapshotSummary
- RuleSetting → RuleType
- SeedAllocations → SeedAllocation
- Ranking → RankingPeriod
- RuleImpact → ImpactGap
- RegretReport → RuleImpact, ViolationDetails
- ViolationDetails → ViolationDetail
- ViolatedOrder → ViolationLossStrategy, ViolationLossContext
- AssetTimeline → AssetSnapshot
- CumulativeLossTimeline → DailyLoss
- ViolationMarkers → ViolationMarker
- BtcBenchmark → BtcDailyPrice
- AnalysisRules → AnalysisRule

## 모듈 간 의존

| From → To | 참조 방식 | 용도 |
|-----------|----------|------|
| InvestmentRound → Wallet | FindWalletUseCase | InvestmentRound 1:N Wallet |
| Wallet → MarketData | exchangeId, coinId | 거래소-코인-체인 지원 확인 |
| Transfer → Wallet | walletId | 잔고 차감/추가/잠금, 입금 주소 역조회 |
| Transfer → MarketData | exchangeId, coinId | 수수료 조회, 체인 지원 확인 |
| Trading → Wallet | walletId | 잔고 검증, 잔고 반영 |
| Trading → MarketData | ListedCoinPort (FindExchangeCoinMappingUseCase), TradingVenuePort (FindExchangeDetailUseCase) | 거래소-코인 매핑 조회, 수수료율·주문금액정책 조회 |
| Trading → InvestmentRound | ViolationRulePort (FindInvestmentRulesUseCase + FindWalletUseCase) | walletId → roundId → 투자 원칙 위반 검증 |
| Portfolio → InvestmentRound | ActiveRoundQueryPort (FindActiveRoundsUseCase), EmergencyFundingSnapshotQueryPort (SumEmergencyFundingUseCase) | 활성 라운드 조회, 긴급 충전 합산 |
| Portfolio → Wallet | WalletSnapshotQueryPort (FindWalletUseCase), BalanceQueryPort (GetAvailableBalanceUseCase) | 라운드별 지갑 조회, 기축통화 잔고 조회 |
| Portfolio → MarketData | ExchangeSnapshotQueryPort (FindExchangeDetailUseCase), EvaluatedHoldingQueryPort (FindExchangeCoinMappingUseCase) | 거래소 기축통화 조회, 코인-거래소코인 매핑 |
| Portfolio → Trading | EvaluatedHoldingQueryPort (FindActiveHoldingsUseCase + GetLivePriceUseCase) | 보유 자산 현재가 평가 |
| Ranking → Portfolio | SnapshotSummaryQueryPort (FindSnapshotSummariesUseCase), RankerHoldingQueryPort (FindSnapshotDetailsUseCase) | 스냅샷 요약 조회, 랭커 보유 코인 조회 |
| Ranking → MarketData | RankerHoldingQueryPort (FindCoinSymbolsUseCase + FindExchangeSummaryUseCase) | 코인 심볼·거래소 정보 조회 |
| Ranking → InvestmentRound | RankerRoundQueryPort (FindRoundInfoUseCase), EligibleRoundQueryPort (FindActiveRoundsUseCase) | 활성 라운드 조회, 랭킹 참여 자격 판단 |
| Ranking → Wallet + Trading | EligibleRoundQueryPort (FindWalletUseCase + CountFilledOrdersUseCase) | 지갑 목록·체결 주문 수 조회 |
| RegretAnalysis → Trading | OrderExecutionQueryPort (FindFilledOrdersUseCase), RuleBreachQueryPort (FindViolationsUseCase), LivePriceQueryPort (GetLivePriceUseCase) | 체결 주문 조회, 규칙 위반 기록 조회, 실시간 가격 조회 |
| RegretAnalysis → MarketData | AnalysisExchangeQueryPort (FindExchangeDetailUseCase + FindCoinSymbolsUseCase), BtcPriceHistoryQueryPort (InfluxDB) | 거래소 정보·코인 심볼 조회, BTC 가격 이력 |
| RegretAnalysis → InvestmentRound | AnalysisRoundQueryPort (FindRoundInfoUseCase), AnalysisRuleQueryPort (FindInvestmentRulesUseCase), ActiveRoundExchangeQueryPort (FindActiveRoundsUseCase) | 라운드 정보·투자 원칙·활성 라운드 조회 |
| RegretAnalysis → Wallet | ActiveRoundExchangeQueryPort (FindWalletUseCase), AnalysisExchangeQueryPort (FindWalletUseCase) | 활성 라운드별 거래소·지갑 존재 확인 |
| RegretAnalysis → Portfolio | AssetSnapshotQueryPort (FindSnapshotsUseCase) | 포트폴리오 스냅샷 조회 |
