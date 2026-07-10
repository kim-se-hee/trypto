# 제공

패키지: `ksh.tryptobackend.trading.application.port.in`

## FindEvaluatedHoldingsUseCase
- `findEvaluatedHoldings(Long walletId, Long exchangeId) → List<EvaluatedHoldingResult>`
- Returns `EvaluatedHoldingResult { coinId: Long, avgBuyPrice: BigDecimal, totalQuantity: BigDecimal, currentPrice: BigDecimal }`

## CountTradesByRoundIdsUseCase
- `countTradesByRoundIds(List<Long> roundIds) → Map<Long, Integer>`

## FindViolatedOrdersUseCase
- `findViolatedOrders(FindViolatedOrdersQuery query) → List<ViolatedOrderResult>`

# 의존

trading 응용 서비스는 타 컨텍스트 UseCase 를 직접 주입하지 않는다. marketdata·wallet·investmentround 의 UseCase 는 trading 자기 컨텍스트의 ACL 출력 포트(`MarketQueryPort`·`WalletQueryPort`·`InvestmentRoundQueryPort`)와 그 구현 `TradingAcl...QueryAdapter` 가 감싸 trading 자기 모델(VO)로 번역해 노출한다. 잔고 반영·위반 검증처럼 협력형 로직은 도메인 서비스와 그 어댑터 구현이 감싼다. 자기 컨텍스트 내부 조회는 각 서비스가 자기 출력 포트를 직접 호출해 합성한다.

## ACL 출력 포트 (읽기 번역)

### MarketQueryPort ← MarketData (`TradingAclMarketQueryAdapter`)
소비 UseCase 를 `MarketInfo`·`TradingPair`·`MarketIdentifier`·`Price`·`CoinExchangeMapping`·`PriceCandidates` 로 번역한다.
- `GetLivePriceUseCase` — 현재가 조회
- `FindExchangeDetailUseCase` — 거래소 상세(수수료율·기준통화) 조회
- `FindExchangeCoinMappingUseCase` — 거래소-코인 매핑
- `FindCoinInfoUseCase` — 코인 심볼
- `FindTicksUseCase` — 가격 후보용 tick 이력

### WalletQueryPort ← Wallet (`TradingAclWalletQueryAdapter`)
소비 UseCase 를 `WalletRef` 로 번역한다.
- `GetWalletOwnerIdUseCase` — 지갑 소유자 확인
- `GetAvailableBalanceUseCase` — 가용 잔고 조회
- `FindWalletUseCase` — 라운드/거래소별 지갑 조회

### InvestmentRoundQueryPort ← InvestmentRound (`TradingAclInvestmentRoundQueryAdapter`)
소비 UseCase 를 `InvestmentRule` 로 번역한다.
- `FindInvestmentRulesUseCase` — 라운드 투자 원칙 조회

## 도메인 서비스 어댑터 (연동형)

인터페이스는 `domain/service` 에, 구현은 `adapter/out/service` 에 두고 타 컨텍스트 UseCase 로 위임한다.
- `BalanceChangeApplier` ← Wallet `ApplyBalanceChangesUseCase` — 체결·취소 시 잔고 변경 묶음을 한 호출로 반영
- `RuleViolationChecker` ← InvestmentRound `CheckRuleViolationsUseCase` — 주문 시점 투자 원칙 위반 검증

## 자기 컨텍스트 내부 조회 합성

응용 서비스는 자기 출력 포트(`PositionQueryPort`·`OrderQueryPort`·`RuleViolationQueryPort` 등)를 직접 조합해 조회를 구성한다. 여러 서비스가 같은 조회를 필요로 하면 각 서비스가 출력 포트를 직접 호출한다(공용 협력자 계층은 두지 않는다).
