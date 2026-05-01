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

## Wallet
- `GetAvailableBalanceUseCase` — 잔고 검증
- `ManageWalletBalanceUseCase` — 잔고 반영
- `FindWalletUseCase` — walletId → roundId 조회
- `GetWalletOwnerIdUseCase` — 지갑 소유자 확인

## MarketData
- `GetLivePriceUseCase` — 시세 조회
- `GetPriceChangeRateUseCase` — 가격 변동률 조회 (추격매수 룰 검증)
- `FindExchangeDetailUseCase` — 수수료율 조회
- `FindExchangeCoinMappingUseCase` — 거래소-코인 매핑
- `FindCoinInfoUseCase` — 코인 정보
- `FindTicksUseCase` — 누락 주문 보상용 tick 이력

## InvestmentRound
- `CheckRuleViolationsUseCase` — 투자 원칙 위반 검증
- `FindInvestmentRulesUseCase` — 투자 원칙 조회 (위반 주문 분석)
