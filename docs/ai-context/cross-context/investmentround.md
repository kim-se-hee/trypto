# investmentround 크로스 컨텍스트 인터페이스

패키지: `ksh.tryptobackend.investmentround.application.port.in`

## UseCase

### FindRoundInfoUseCase
- `findById(Long roundId) → Optional<RoundInfoResult>`
- `findActiveByUserId(Long userId) → Optional<RoundInfoResult>`

### FindActiveRoundsUseCase
- `findAllActiveRounds() → List<ActiveRoundResult>`

### FindInvestmentRulesUseCase
- `findByRoundId(Long roundId) → List<InvestmentRuleResult>`

### SumEmergencyFundingUseCase
- `sumByRoundId(Long roundId) → BigDecimal`
- `sumByRoundIdAndExchangeId(Long roundId, Long exchangeId) → BigDecimal`

### CheckRuleViolationsUseCase
- `checkViolations(CheckRuleViolationsQuery query) → List<RuleViolationResult>`

## Result DTO

| DTO | 필드 |
|-----|------|
| RoundInfoResult | roundId: Long, userId: Long, roundNumber: long, initialSeed: BigDecimal, emergencyFundingLimit: BigDecimal, emergencyChargeCount: int, status: String, startedAt: LocalDateTime, endedAt: LocalDateTime |
| ActiveRoundResult | roundId: Long, userId: Long, startedAt: LocalDateTime |
| InvestmentRuleResult | ruleId: Long, ruleType: RuleType, thresholdValue: BigDecimal |
| RuleViolationResult | ruleId: Long, violationReason: String, createdAt: LocalDateTime |

## 참조 Enum

- **RuleType**: LOSS_CUT, PROFIT_TAKE, CHASE_BUY_BAN, AVERAGING_DOWN_LIMIT, OVERTRADING_LIMIT
