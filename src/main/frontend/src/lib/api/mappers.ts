import type { RankingPeriod } from "@/lib/types/ranking";
import type { RuleType } from "@/lib/types/round";

export type BackendRuleType =
  | "LOSS_CUT"
  | "PROFIT_TAKE"
  | "CHASE_BUY_BAN"
  | "AVERAGING_DOWN_LIMIT"
  | "OVERTRADING_LIMIT";

export type BackendRankingPeriod = "DAILY" | "WEEKLY" | "MONTHLY";

const FRONT_TO_BACK_RULE_TYPE: Record<RuleType, BackendRuleType> = {
  STOP_LOSS: "LOSS_CUT",
  TAKE_PROFIT: "PROFIT_TAKE",
  NO_CHASE_BUY: "CHASE_BUY_BAN",
  AVERAGING_LIMIT: "AVERAGING_DOWN_LIMIT",
  OVERTRADE_LIMIT: "OVERTRADING_LIMIT",
};

const BACK_TO_FRONT_RULE_TYPE: Record<BackendRuleType, RuleType> = {
  LOSS_CUT: "STOP_LOSS",
  PROFIT_TAKE: "TAKE_PROFIT",
  CHASE_BUY_BAN: "NO_CHASE_BUY",
  AVERAGING_DOWN_LIMIT: "AVERAGING_LIMIT",
  OVERTRADING_LIMIT: "OVERTRADE_LIMIT",
};

const FRONT_TO_BACK_PERIOD: Record<RankingPeriod, BackendRankingPeriod> = {
  daily: "DAILY",
  weekly: "WEEKLY",
  monthly: "MONTHLY",
};

const BACK_TO_FRONT_PERIOD: Record<BackendRankingPeriod, RankingPeriod> = {
  DAILY: "daily",
  WEEKLY: "weekly",
  MONTHLY: "monthly",
};

export function toBackendRuleType(ruleType: RuleType): BackendRuleType {
  return FRONT_TO_BACK_RULE_TYPE[ruleType];
}

export function toFrontRuleType(ruleType: BackendRuleType): RuleType {
  return BACK_TO_FRONT_RULE_TYPE[ruleType];
}

export function toBackendRankingPeriod(period: RankingPeriod): BackendRankingPeriod {
  return FRONT_TO_BACK_PERIOD[period];
}

export function toFrontRankingPeriod(period: BackendRankingPeriod): RankingPeriod {
  return BACK_TO_FRONT_PERIOD[period];
}

