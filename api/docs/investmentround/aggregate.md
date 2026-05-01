# Aggregate Root / Entity / Value Object

| Aggregate Root | Entity | Value Object |
|---|---|---|
| InvestmentRound | RuleSetting, EmergencyFunding, DetectedViolation | RoundStatus, SeedAmountPolicy, SeedAllocation, SeedAllocations, SeedFundingSpec, RoundOverview, ViolationCheckContext, ViolationRule (sealed), ViolationRules |

# 소유 관계

- ViolationRules → ViolationRule
- RuleSetting → RuleType
- SeedAllocations → SeedAllocation
