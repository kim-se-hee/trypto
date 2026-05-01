# 제공

패키지: `ksh.tryptobackend.investmentround.application.port.in`

## FindRoundInfoUseCase
- `findById(Long roundId) → Optional<RoundInfoResult>`
- `findActiveByUserId(Long userId) → Optional<RoundInfoResult>`
- Returns `RoundInfoResult { roundId: Long, userId: Long, roundNumber: long, initialSeed: BigDecimal, emergencyFundingLimit: BigDecimal, emergencyChargeCount: int, status: String, startedAt: LocalDateTime, endedAt: LocalDateTime }`

## FindActiveRoundsUseCase
- `findAllActiveRounds() → List<ActiveRoundResult>`
- Returns `ActiveRoundResult { roundId: Long, userId: Long, startedAt: LocalDateTime }`

## FindInvestmentRulesUseCase
- `findByRoundId(Long roundId) → List<InvestmentRuleResult>`
- Returns `InvestmentRuleResult { ruleId: Long, ruleType: RuleType, thresholdValue: BigDecimal }`
- `RuleType`: LOSS_CUT, PROFIT_TAKE, CHASE_BUY_BAN, AVERAGING_DOWN_LIMIT, OVERTRADING_LIMIT

## SumEmergencyFundingUseCase
- `sumByRoundId(Long roundId) → BigDecimal`
- `sumByRoundIdAndExchangeId(Long roundId, Long exchangeId) → BigDecimal`

## CheckRuleViolationsUseCase
- `checkViolations(CheckRuleViolationsQuery query) → List<RuleViolationResult>`
- Returns `RuleViolationResult { ruleId: Long, violationReason: String, createdAt: LocalDateTime }`

# 의존

## MarketData
- `FindExchangeDetailUseCase` — 거래소 기축통화 확인

## Wallet
- `CreateWalletWithBalanceUseCase` — 라운드 시작 시 지갑 생성
- `FindWalletUseCase` — 긴급 충전 시 지갑 조회
- `ManageWalletBalanceUseCase` — 긴급 충전 시 잔고 반영
