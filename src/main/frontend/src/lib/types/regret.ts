import type { RuleType } from "./round";

// ── 타입 정의 ──────────────────────────────────────────

export interface AssetSnapshot {
  date: string;       // 표시용 라벨
  fullDate: string;   // yyyy-MM-dd (hover 상세용)
  actual: number;
  ruleFollowed: number; // 전체 규칙 준수
}

export interface RegretSummary {
  missedProfit: number;
  actualProfitRate: number;
  ruleFollowedProfitRate: number;
  totalViolations: number;
}

export interface RuleToggleItem {
  ruleType: RuleType;
  label: string;
  color: string;
  thresholdValue: number;
  thresholdUnit: string;
  violationCount: number;
}

export interface BenchmarkItem {
  id: string;
  label: string;
  color: string;
  profitRate: number;
}

export type ViolationEmotion = "FOMO" | "감이 좋아서" | "복수 매매";

export interface ViolationTrade {
  id: number;
  coinSymbol: string;
  date: string;
  emotion?: ViolationEmotion;
  violatedRules: RuleType[];
  profitLoss: number;
}

export type ViolationFilter = "ALL" | "LOSS" | "PROFIT";

export interface ViolationMarker {
  date: string;
  value: number;
  type: "loss" | "gain";
}

// ── RuleType → 한국어/색상 매핑 ──────────────────────────

export const RULE_LABELS: Record<RuleType, string> = {
  STOP_LOSS: "손절",
  TAKE_PROFIT: "익절",
  NO_CHASE_BUY: "추격 매수 금지",
  AVERAGING_LIMIT: "물타기 제한",
  OVERTRADE_LIMIT: "과매매 제한",
};

export const RULE_COLORS: Record<RuleType, string> = {
  STOP_LOSS: "#ED4B9E",
  TAKE_PROFIT: "#31D0AA",
  NO_CHASE_BUY: "#FFB237",
  AVERAGING_LIMIT: "#e84142",
  OVERTRADE_LIMIT: "#1FC7D4",
};

/**
 * 규칙별 영향도 가중치 (합 = 1).
 * 활성화된 규칙의 가중치 합만큼 actual → ruleFollowed 사이를 보간한다.
 */
export const RULE_IMPACT_WEIGHTS: Record<RuleType, number> = {
  STOP_LOSS: 0.30,
  NO_CHASE_BUY: 0.25,
  TAKE_PROFIT: 0.20,
  OVERTRADE_LIMIT: 0.15,
  AVERAGING_LIMIT: 0.10,
};

/** 활성화된 규칙 기반으로 시뮬레이션 자산 시계열을 계산한다. */
export function computeSimulationLine(
  snapshots: AssetSnapshot[],
  enabledRules: Set<RuleType>,
): number[] {
  const totalWeight = Array.from(enabledRules).reduce(
    (sum, r) => sum + (RULE_IMPACT_WEIGHTS[r] ?? 0),
    0,
  );
  return snapshots.map((s) => Math.round(s.actual + (s.ruleFollowed - s.actual) * totalWeight));
}

// ── 감정 라벨 색상 ──────────────────────────────────────

export const EMOTION_STYLES: Record<ViolationEmotion, { bg: string; text: string }> = {
  FOMO: { bg: "bg-amber-500/15", text: "text-amber-600" },
  "감이 좋아서": { bg: "bg-chart-2/15", text: "text-chart-2" },
  "복수 매매": { bg: "bg-negative/15", text: "text-negative" },
};

/** 시즌 기간에 따른 x축 라벨 표시 간격 (일 수) */
export function getTickInterval(totalDays: number): number {
  if (totalDays <= 14) return 1;
  if (totalDays <= 60) return 7;
  if (totalDays <= 180) return 14;
  return 30;
}
