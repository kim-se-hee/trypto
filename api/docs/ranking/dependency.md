다른 컨텍스트에 노출하는 UseCase가 없다.

# 의존

## InvestmentRound
- `FindRoundInfoUseCase` — 라운드 정보 조회
- `FindActiveRoundsUseCase` — 활성 라운드 조회

## Portfolio
- `FindSnapshotDetailsUseCase` — 스냅샷 상세 조회
- `FindSnapshotSummariesUseCase` — 스냅샷 요약 조회

## MarketData
- `FindCoinSymbolsUseCase` — 코인 심볼 조회
- `FindExchangeNamesUseCase` — 거래소 이름 조회

## Trading
- `CountTradesByRoundIdsUseCase` — 라운드별 체결 수 조회

## User
- `FindUserPublicInfoUseCase` — 닉네임/공개 여부 조회
