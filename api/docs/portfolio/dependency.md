# 제공

패키지: `ksh.tryptobackend.portfolio.application.port.in`

## FindSnapshotsUseCase
- `findLatestByRoundIdAndExchangeId(Long roundId, Long exchangeId) → Optional<SnapshotInfoResult>`
- `findAllByRoundIdAndExchangeId(Long roundId, Long exchangeId) → List<SnapshotInfoResult>`
- Returns `SnapshotInfoResult { snapshotId: Long, roundId: Long, exchangeId: Long, totalAsset: BigDecimal, totalInvestment: BigDecimal, totalProfitRate: BigDecimal, snapshotDate: LocalDate }`

## FindSnapshotSummariesUseCase
- `findLatestSummaries(LocalDate snapshotDate) → List<SnapshotSummaryResult>`
- Returns `SnapshotSummaryResult { userId: Long, roundId: Long, totalAssetKrw: BigDecimal, totalInvestmentKrw: BigDecimal }`

## FindSnapshotDetailsUseCase
- `findLatestSnapshotDetails(Long userId, Long roundId) → List<SnapshotDetailResult>`
- Returns `SnapshotDetailResult { coinId: Long, exchangeId: Long, assetRatio: BigDecimal, profitRate: BigDecimal }`

# 의존

portfolio 응용 서비스는 타 컨텍스트 UseCase 를 직접 주입하지 않는다. marketdata·wallet·investmentround·trading 의 UseCase 는 portfolio 자기 컨텍스트의 ACL 출력 포트(`MarketDataQueryPort`·`WalletQueryPort`·`InvestmentRoundQueryPort`·`TradingQueryPort`)와 그 구현 `PortfolioAcl...QueryAdapter` 가 감싸 portfolio 자기 모델/VO 로 번역해 노출한다.

## ACL 출력 포트 (읽기 번역)

### MarketDataQueryPort ← MarketData (`PortfolioAclMarketDataQueryAdapter`)
소비 UseCase 를 `ExchangeSnapshot`·`KrwConversionRate`·`CoinMetadataMap` 로 번역한다.
- `FindExchangeDetailUseCase` — 거래소 기축통화·국내외 구분(환산율) 조회
- `FindCoinInfoUseCase` — 코인 심볼·이름 조회

### WalletQueryPort ← Wallet (`PortfolioAclWalletQueryAdapter`)
소비 UseCase 를 `PortfolioWallet`·`WalletSnapshots`·잔고로 번역한다.
- `FindWalletUseCase` — 지갑·라운드별 지갑 조회
- `GetWalletOwnerIdUseCase` — 지갑 소유자 확인
- `GetAvailableBalanceUseCase` — 가용/총 잔고 조회

### InvestmentRoundQueryPort ← InvestmentRound (`PortfolioAclInvestmentRoundQueryAdapter`)
소비 UseCase 를 `ActiveRounds`·긴급 충전 합계로 번역한다.
- `FindActiveRoundsUseCase` — 스냅샷 대상 라운드 조회
- `SumEmergencyFundingUseCase` — 긴급 충전 합산

### TradingQueryPort ← Trading (`PortfolioAclTradingQueryAdapter`)
소비 UseCase 를 `PortfolioHoldings`·`EvaluatedHoldings` 로 번역한다.
- `FindEvaluatedHoldingsUseCase` — 평가된 보유 자산 조회
