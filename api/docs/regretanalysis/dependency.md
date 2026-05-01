다른 컨텍스트에 노출하는 UseCase가 없다.

# 의존

## InvestmentRound
- `FindInvestmentRulesUseCase` — 투자 원칙 조회
- `FindActiveRoundsUseCase` — 활성 라운드 조회
- `FindRoundInfoUseCase` — 라운드 정보 조회

## Trading
- `FindViolatedOrdersUseCase` — 위반 주문 + 손익 분석 결과 조회

## MarketData
- `GetLivePriceUseCase` — 실시간 가격 조회
- `FindCoinSymbolsUseCase` — 코인 심볼 조회
- `FindExchangeDetailUseCase` — 거래소 정보 조회
- `FindBtcDailyPricesUseCase` — BTC 벤치마크용 일별 종가 조회

## Wallet
- `FindWalletUseCase` — 활성 라운드별 지갑 조회

## Portfolio
- `FindSnapshotsUseCase` — 포트폴리오 스냅샷 조회
