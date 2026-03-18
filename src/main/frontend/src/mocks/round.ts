import type { InvestmentRound, RuleType } from "@/lib/types/round";

export type { RoundStatus, RuleType, InvestmentRule, InvestmentRound, RoundWallet } from "@/lib/types/round";

/** DEV_SKIP_AUTH와 함께 사용 — 기본 라운드를 미리 생성 */
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
    wallets: [],
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
