# 데이터 모델

## 도메인별 Aggregate 구조

| 도메인 | Aggregate Root | Entity | Value Object |
|--------|---------------|--------|--------------|
| User | User | — | — |
| Wallet | Wallet | WalletBalance, DepositAddress | DepositTargetExchange, WalletBalances |
| Transfer | Transfer | — | TransferStatus, TransferType, TransferBalanceChange, TransferWallet |
| Trading | Order, Holding, OrderFillFailure | RuleViolation | Side, OrderType, OrderStatus, OrderMode, Fee, Quantity, BalanceChange, OrderAmountPolicy, TradingVenue, RuleViolationRef, FilledOrder, FilledOrderCounts, CoinExchangeMapping, PendingOrder, ExchangeSymbolKey, OrderFilledEvent |
| MarketData | Exchange, Coin, ExchangeCoin | ExchangeCoinChain, WithdrawalFee | ExchangeMarketType, CoinSymbols, DailyClosePrice, ExchangeCoinIdMap, ExchangeSummary, LivePrices |
| Portfolio | PortfolioSnapshot | SnapshotDetail, EvaluatedHolding | ActiveRound, ActiveRounds, ExchangeSnapshot, KrwConversionRate, WalletSnapshot, WalletSnapshots, EvaluatedHoldings, PortfolioHolding, PortfolioHoldings, CoinSnapshot, CoinSnapshotMap, HoldingSnapshot, HoldingSummary, SnapshotOverview, UserSnapshotSummary |
| Ranking | Ranking | — | RankingPeriod, RoundKey, RankingCandidate, RankingCandidates, EligibleRound, EligibleRounds, SnapshotSummary, SnapshotSummaries, RoundTradeCounts, ExchangeNames, CoinSymbols, RankingSummary, RankingStats |
| InvestmentRound | InvestmentRound | RuleSetting, EmergencyFunding, DetectedViolation | RoundStatus, SeedAmountPolicy, SeedAllocation, SeedAllocations, SeedFundingSpec, RoundOverview, ViolationCheckContext, ViolationRule (sealed), ViolationRules |
| RegretAnalysis | RegretReport | RuleImpact, ViolationDetail, ViolationDetails, AssetSnapshot, ViolatedOrder, ViolatedOrders | ImpactGap, ThresholdUnit, AssetTimeline, BtcBenchmark, BtcDailyPrice, BtcDailyPrices, CumulativeLossTimeline, ViolationMarkers, ViolationLossContext, AnalysisRound, AnalysisRoundStatus, AnalysisRule, AnalysisRules, AnalysisExchange, ActiveRoundExchange, TradeSide, OrderExecution, OrderExecutions, CurrentPrices, RuleBreach |
| RegretAnalysis (Strategy) | — | — | ViolationLossStrategy (enum) |
| Common (Shared Kernel) | — | — | RuleType, ProfitRate |

**소유 관계:**
- Wallet → WalletBalance, DepositAddress
- WalletBalances → WalletBalance
- Transfer → TransferBalanceChange
- TradingVenue → OrderAmountPolicy
- Order → RuleViolation
- ViolationRules → ViolationRule
- PortfolioSnapshot → SnapshotDetail
- EvaluatedHoldings → EvaluatedHolding
- PortfolioHoldings → PortfolioHolding
- ActiveRounds → ActiveRound
- WalletSnapshots → WalletSnapshot
- CoinSnapshotMap → CoinSnapshot
- RankingCandidates → RankingCandidate
- EligibleRounds → EligibleRound
- SnapshotSummaries → SnapshotSummary
- RuleSetting → RuleType
- SeedAllocations → SeedAllocation
- Ranking → RankingPeriod
- RuleImpact → ImpactGap
- RegretReport → RuleImpact, ViolationDetails
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

## 모듈 간 의존

서비스가 다른 컨텍스트의 UseCase(Input Port)를 직접 주입받는다. 크로스 컨텍스트 전용 Output Port/Adapter는 만들지 않는다.

| From → To | UseCase | 용도 |
|-----------|---------|------|
| Wallet → MarketData | FindExchangeDetailUseCase, FindExchangeCoinChainUseCase, FindCoinInfoUseCase | 입금 주소 발급 시 거래소·체인 확인, 잔고 조회 시 기축통화 심볼 조회 |
| Wallet → InvestmentRound | FindRoundInfoUseCase | 잔고 조회 시 소유권 검증 |
| InvestmentRound → MarketData | FindExchangeDetailUseCase | 거래소 기축통화 확인 |
| InvestmentRound → Wallet | CreateWalletWithBalanceUseCase, FindWalletUseCase, ManageWalletBalanceUseCase | 지갑 생성, 긴급 충전 시 지갑 조회·잔고 반영 |
| Trading → Wallet | GetAvailableBalanceUseCase, ManageWalletBalanceUseCase, FindWalletUseCase | 잔고 검증·반영, walletId→roundId 조회 |
| Trading → MarketData | GetLivePriceUseCase, FindExchangeDetailUseCase, FindExchangeCoinMappingUseCase | 시세 조회, 거래소-코인 매핑·수수료율 조회 |
| Trading → InvestmentRound | CheckRuleViolationsUseCase | 투자 원칙 위반 검증 |
| Transfer → Wallet | FindWalletUseCase, GetAvailableBalanceUseCase, ManageWalletBalanceUseCase, GetWalletOwnerIdUseCase | 잔고 차감/추가, 지갑 소유자 확인 |
| Transfer → InvestmentRound | FindRoundInfoUseCase | 송금 내역 조회 시 라운드 정보 |
| Portfolio → InvestmentRound | FindRoundInfoUseCase, SumEmergencyFundingUseCase, FindActiveRoundsUseCase | 라운드 정보 조회, 긴급 충전 합산, 스냅샷 대상 라운드 조회 |
| Portfolio → Wallet | FindWalletUseCase, GetAvailableBalanceUseCase, GetWalletOwnerIdUseCase | 라운드별 지갑·잔고 조회, 지갑 소유자 확인 |
| Portfolio → MarketData | FindExchangeDetailUseCase, FindCoinInfoUseCase, FindExchangeCoinMappingUseCase, GetLivePriceUseCase, GetLivePricesUseCase | 거래소·코인 정보, 실시간 시세 조회 |
| Portfolio → Trading | FindActiveHoldingsUseCase, FindEvaluatedHoldingsUseCase | 보유 자산 조회, 평가된 보유 자산 조회 |
| Ranking → InvestmentRound | FindRoundInfoUseCase, FindActiveRoundsUseCase | 라운드 정보·활성 라운드 조회 |
| Ranking → Portfolio | FindSnapshotDetailsUseCase, FindSnapshotSummariesUseCase | 스냅샷 상세·요약 조회 |
| Ranking → MarketData | FindCoinSymbolsUseCase, FindExchangeSummaryUseCase, FindExchangeNamesUseCase | 코인 심볼·거래소 정보·거래소 이름 조회 |
| Ranking → Wallet | FindWalletUseCase | 지갑 목록 조회 |
| Ranking → Trading | CountFilledOrdersUseCase | 체결 주문 수 조회 |
| RegretAnalysis → InvestmentRound | FindInvestmentRulesUseCase, FindActiveRoundsUseCase, FindRoundInfoUseCase | 투자 원칙·활성 라운드·라운드 정보 조회 |
| RegretAnalysis → Trading | FindViolationsUseCase, FindFilledOrdersUseCase | 체결 주문·규칙 위반 기록 조회 |
| RegretAnalysis → MarketData | GetLivePriceUseCase, FindCoinSymbolsUseCase, FindExchangeDetailUseCase | 실시간 가격·코인 심볼·거래소 정보 조회 |
| RegretAnalysis → Wallet | FindWalletUseCase | 활성 라운드별 지갑 조회 |
| RegretAnalysis → Portfolio | FindSnapshotsUseCase | 포트폴리오 스냅샷 조회 |
