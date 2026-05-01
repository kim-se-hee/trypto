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

## InvestmentRound
- `SumEmergencyFundingUseCase` — 긴급 충전 합산
- `FindActiveRoundsUseCase` — 스냅샷 대상 라운드 조회

## Wallet
- `FindWalletUseCase` — 라운드별 지갑 조회
- `GetAvailableBalanceUseCase` — 잔고 조회
- `GetWalletOwnerIdUseCase` — 지갑 소유자 확인

## MarketData
- `FindExchangeDetailUseCase` — 거래소 정보
- `FindCoinInfoUseCase` — 코인 정보

## Trading
- `FindEvaluatedHoldingsUseCase` — 평가된 보유 자산 조회
