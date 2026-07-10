다른 컨텍스트에 노출하는 UseCase가 없다.

# 의존

ranking 응용 서비스는 타 컨텍스트 UseCase 를 직접 주입하지 않는다. investmentround·trading·portfolio·marketdata·user 의 UseCase 는 ranking 자기 컨텍스트의 ACL 출력 포트(`InvestmentRoundQueryPort`·`TradingQueryPort`·`PortfolioQueryPort`·`MarketDataQueryPort`·`UserQueryPort`)와 그 구현 `Acl...QueryAdapter` 가 감싸 ranking 자기 모델/VO 로 번역해 노출한다.

## ACL 출력 포트 (읽기 번역)

### InvestmentRoundQueryPort ← InvestmentRound (`AclInvestmentRoundQueryAdapter`)
소비 UseCase 를 활성 라운드 ID·`ActiveRounds` 로 번역한다. 동일 클래스명이 타 컨텍스트에 있어 빈 이름은 `rankingAclInvestmentRoundQueryAdapter` 로 둔다.
- `FindRoundInfoUseCase` — 라운드 정보(활성 라운드 ID) 조회
- `FindActiveRoundsUseCase` — 랭킹 산출 대상 활성 라운드 조회

### TradingQueryPort ← Trading (`AclTradingQueryAdapter`)
소비 UseCase 를 `RoundTradeCounts` 로 번역한다. 라운드 ID 리스트로 체결 수를 한 번에 조회하는 벌크 경로다. 동일 클래스명이 타 컨텍스트에 있어 빈 이름은 `rankingAclTradingQueryAdapter` 로 둔다.
- `CountTradesByRoundIdsUseCase` — 라운드별 체결 수 조회

### PortfolioQueryPort ← Portfolio (`AclPortfolioQueryAdapter`)
소비 UseCase 를 `Holdings`·`SnapshotSummaries` 로 번역한다. 동일 클래스명이 타 컨텍스트에 있어 빈 이름은 `rankingAclPortfolioQueryAdapter` 로 둔다.
- `FindSnapshotDetailsUseCase` — 스냅샷 상세(보유 자산) 조회
- `FindSnapshotSummariesUseCase` — 스냅샷 요약 조회

### MarketDataQueryPort ← MarketData (`AclMarketDataQueryAdapter`)
소비 UseCase 를 코인 심볼·거래소 이름으로 번역한다.
- `FindCoinSymbolsUseCase` — 코인 심볼 조회
- `FindExchangeNamesUseCase` — 거래소 이름 조회

### UserQueryPort ← User (`AclUserQueryAdapter`)
소비 UseCase 를 사용자 공개 프로필로 번역한다.
- `FindUserPublicInfoUseCase` — 닉네임/공개 여부 조회
