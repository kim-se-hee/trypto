다른 컨텍스트에 노출하는 UseCase가 없다.

# 의존

regretanalysis 응용 서비스는 타 컨텍스트 UseCase 를 직접 주입하지 않는다. investmentround·wallet·trading·marketdata·portfolio 의 UseCase 는 regretanalysis 자기 컨텍스트의 ACL 출력 포트(`InvestmentRoundQueryPort`·`WalletQueryPort`·`TradingQueryPort`·`MarketDataQueryPort`·`PortfolioQueryPort`)와 그 구현 `Acl...QueryAdapter` 가 감싸 regretanalysis 자기 모델/VO 로 번역해 노출한다. 동일 클래스명이 타 컨텍스트에 있어 빈 이름은 모두 `regretanalysisAcl...QueryAdapter` 로 둔다.

## ACL 출력 포트 (읽기 번역)

### InvestmentRoundQueryPort ← InvestmentRound (`AclInvestmentRoundQueryAdapter`)
소비 UseCase 를 `AnalysisRound`·`AnalysisRules`·`AnalysisActiveRound` 로 번역한다.
- `FindRoundInfoUseCase` — 라운드 정보 조회
- `FindInvestmentRulesUseCase` — 투자 원칙 조회
- `FindActiveRoundsUseCase` — 후회 리포트 대상 활성 라운드 조회

### WalletQueryPort ← Wallet (`AclWalletQueryAdapter`)
소비 UseCase 를 지갑 존재 여부·`AnalysisWallet` 로 번역한다.
- `FindWalletUseCase` — 라운드별 지갑 조회

### TradingQueryPort ← Trading (`AclTradingQueryAdapter`)
소비 UseCase 를 `ViolatedOrders` 로 번역한다.
- `FindViolatedOrdersUseCase` — 위반 주문 + 손익 분석 결과 조회

### MarketDataQueryPort ← MarketData (`AclMarketDataQueryAdapter`)
소비 UseCase 를 `AnalysisExchange`·코인 심볼·`BtcDailyPrices`·`CurrentPrices` 로 번역한다.
- `FindExchangeDetailUseCase` — 거래소 정보 조회
- `FindCoinSymbolsUseCase` — 코인 심볼 조회
- `FindBtcDailyPricesUseCase` — BTC 벤치마크용 일별 종가 조회
- `GetLivePriceUseCase` — 실시간 가격 조회

### PortfolioQueryPort ← Portfolio (`AclPortfolioQueryAdapter`)
소비 UseCase 를 `AssetTimeline`·`AssetSnapshot` 로 번역한다.
- `FindSnapshotsUseCase` — 포트폴리오 스냅샷 조회

# 배치

후회 리포트 생성 배치(`RegretReportItemProcessor`)는 adapter/in 계층이므로 자기 컨텍스트 UseCase 를 직접 호출한다. `GenerateRegretReportUseCase` 를 그대로 호출해 `RegretReport` 를 만들고, `RegretReportItemWriter` 가 `SaveRegretReportsUseCase` 로 저장한다. 통과 계층이던 `GenerateRegretReportBatchUseCase`·`GenerateRegretReportBatchService`·`GeneratedRegretReportResult` 는 제거했다.
