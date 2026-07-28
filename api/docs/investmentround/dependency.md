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

## FindEmergencyFundingsUseCase
- `findByRoundId(Long roundId) → List<EmergencyFundingResult>`
- `findByIdempotencyKey(UUID idempotencyKey) → Optional<EmergencyFundingResult>`
- Returns `EmergencyFundingResult { fundingId: Long, exchangeId: Long, amount: BigDecimal, krwConvertedAmount: BigDecimal, chargedAt: LocalDateTime }`
- `amount` 는 투입 거래소 기축통화 단위, `krwConvertedAmount` 는 투입 시점 환산 원화다. 원화 집계에는 환산액을 쓴다

## CheckRuleViolationsUseCase
- `checkViolations(CheckRuleViolationsQuery query) → List<RuleViolationResult>`
- Returns `RuleViolationResult { ruleId: Long, violationReason: String, createdAt: LocalDateTime }`

# 의존

## MarketData
- `FindExchangeDetailUseCase` — 거래소 기축통화·국내/해외 구분 확인, 환산 시세 소스 거래소(빗썸) 아이디 조회
- `FindExchangeCoinMappingUseCase` — 빗썸 USDT 마켓의 거래소-코인 아이디 조회 (원화 환산 시세 소스)
- `GetLivePriceUseCase` — USDT/KRW 현재가 조회 (시드·긴급 충전 원화 환산)

## Wallet
- `CreateWalletWithBalanceUseCase` — 라운드 시작 시 지갑 생성
- `FindWalletUseCase` — 긴급 충전 시 지갑 조회
- `ApplyBalanceChangesUseCase` — 긴급 충전 시 잔고 반영
