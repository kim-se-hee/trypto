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

export interface RoundWallet {
  walletId: number;
  exchangeId: number;
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
  wallets: RoundWallet[];
  startedAt: string;
  endedAt: string | null;
}
