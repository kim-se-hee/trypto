export type RoundStatus = "ACTIVE" | "BANKRUPT" | "ENDED";

export type RuleType =
  | "STOP_LOSS"
  | "TAKE_PROFIT"
  | "NO_CHASE_BUY"
  | "AVERAGING_LIMIT"
  | "OVERTRADE_LIMIT";

export interface InvestmentRule {
  ruleId: number;
  ruleType: RuleType;
  thresholdValue: number;
}

export interface InvestmentRound {
  roundId: number;
  userId: number;
  roundNumber: number;
  initialSeed: number;
  emergencyFundingLimit: number;
  emergencyChargeCount: number;
  status: RoundStatus;
  rules: InvestmentRule[];
  startedAt: string;
  endedAt: string | null;
}

/** DEV_SKIP_AUTH와 함께 사용 — 기본 라운드를 미리 생성 */
const DEV_DEFAULT_ROUND: InvestmentRound = {
  roundId: 1,
  userId: 1,
  roundNumber: 1,
  initialSeed: 10_000_000,
  emergencyFundingLimit: 1_000_000,
  emergencyChargeCount: 3,
  status: "ACTIVE",
  rules: [
    { ruleId: 1, ruleType: "STOP_LOSS", thresholdValue: 10 },
    { ruleId: 2, ruleType: "TAKE_PROFIT", thresholdValue: 30 },
  ],
  startedAt: new Date().toISOString(),
  endedAt: null,
};

export let mockActiveRound: InvestmentRound | null = null;

let nextRoundId = 2;
let nextRuleId = 3;

export function createMockRound(
  userId: number,
  initialSeed: number,
  emergencyFundingLimit: number,
  rules: { ruleType: RuleType; thresholdValue: number }[],
): InvestmentRound {
  const round: InvestmentRound = {
    roundId: nextRoundId++,
    userId,
    roundNumber: 1,
    initialSeed,
    emergencyFundingLimit,
    emergencyChargeCount: 3,
    status: "ACTIVE",
    rules: rules.map((r) => ({
      ruleId: nextRuleId++,
      ruleType: r.ruleType,
      thresholdValue: r.thresholdValue,
    })),
    startedAt: new Date().toISOString(),
    endedAt: null,
  };
  mockActiveRound = round;
  return round;
}

export function clearMockRound(): void {
  mockActiveRound = null;
}

export function setMockActiveRound(round: InvestmentRound | null): void {
  mockActiveRound = round;
}
