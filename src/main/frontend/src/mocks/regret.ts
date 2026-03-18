export type {
  AssetSnapshot,
  RegretSummary,
  RuleToggleItem,
  BenchmarkItem,
  ViolationEmotion,
  ViolationTrade,
  ViolationFilter,
  ViolationMarker,
} from "@/lib/types/regret";

export {
  RULE_LABELS,
  RULE_COLORS,
  RULE_IMPACT_WEIGHTS,
  EMOTION_STYLES,
  computeSimulationLine,
  getTickInterval,
} from "@/lib/types/regret";

import type { AssetSnapshot, RegretSummary, ViolationMarker, RuleToggleItem, BenchmarkItem, ViolationTrade } from "@/lib/types/regret";
import { RULE_COLORS } from "@/lib/types/regret";

// ── Seeded random ──────────────────────────────────────

function seededRandom(seed: number): () => number {
  let s = seed;
  return () => {
    s = (s * 16807 + 0) % 2147483647;
    return s / 2147483647;
  };
}

// ── 시계열 생성 ────────────────────────────────────────

function generateTimeSeries(
  periodDays: number,
  initialSeed: number,
  seed: number,
  bias: number,
  volatility: number,
): number[] {
  const rand = seededRandom(seed);
  const values: number[] = [];
  let current = initialSeed;

  for (let i = 0; i < periodDays; i++) {
    const change = (rand() - 0.5 + bias) * volatility;
    current = current * (1 + change);
    values.push(Math.round(current));
  }
  return values;
}

function formatDateLabel(d: Date, totalDays: number): string {
  if (totalDays <= 14) return `${d.getMonth() + 1}/${d.getDate()}`;
  if (totalDays <= 60) return `${d.getMonth() + 1}/${d.getDate()}`;
  if (totalDays <= 180) return `${d.getMonth() + 1}/${d.getDate()}`;
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}`;
}

function toFullDate(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

// ── 시즌 데이터 생성 ──────────────────────────────────────

const INITIAL_SEED = 10_000_000;
const SEASON_START = new Date("2026-01-15");
const SEASON_DAYS = 42;

function buildSeasonData() {
  const startDate = new Date(SEASON_START);
  const actualValues = generateTimeSeries(SEASON_DAYS, INITIAL_SEED, 342, 0.05, 0.025);
  const ruleFollowedValues = generateTimeSeries(SEASON_DAYS, INITIAL_SEED, 377, 0.15, 0.018);

  const snapshots: AssetSnapshot[] = Array.from({ length: SEASON_DAYS }, (_, i) => {
    const d = new Date(startDate);
    d.setDate(d.getDate() + i);
    return {
      date: formatDateLabel(d, SEASON_DAYS),
      fullDate: toFullDate(d),
      actual: actualValues[i],
      ruleFollowed: ruleFollowedValues[i],
    };
  });

  const markerRand = seededRandom(1299);
  const markers: ViolationMarker[] = [];
  for (let i = 0; i < SEASON_DAYS; i++) {
    if (markerRand() > 0.82) {
      markers.push({
        date: snapshots[i].date,
        value: actualValues[i],
        type: markerRand() > 0.3 ? "loss" : "gain",
      });
    }
  }

  const lastActual = actualValues[actualValues.length - 1];
  const lastRule = ruleFollowedValues[ruleFollowedValues.length - 1];
  const summary: RegretSummary = {
    missedProfit: Math.max(0, lastRule - lastActual),
    actualProfitRate: Math.round(((lastActual - INITIAL_SEED) / INITIAL_SEED) * 1000) / 10,
    ruleFollowedProfitRate: Math.round(((lastRule - INITIAL_SEED) / INITIAL_SEED) * 1000) / 10,
    totalViolations: 8,
  };

  const btcHoldValues = generateTimeSeries(SEASON_DAYS, INITIAL_SEED, 1188, 0.12, 0.030);

  return { snapshots, markers, summary, btcHoldValues, totalDays: SEASON_DAYS };
}

export const regretData = buildSeasonData();

// ── 투자 원칙 토글 목록 ──────────────────────────────────

export const ruleToggles: RuleToggleItem[] = [
  { ruleType: "STOP_LOSS", label: "손절", color: RULE_COLORS.STOP_LOSS, thresholdValue: -10, thresholdUnit: "%", violationCount: 2 },
  { ruleType: "NO_CHASE_BUY", label: "추격 매수 금지", color: RULE_COLORS.NO_CHASE_BUY, thresholdValue: 20, thresholdUnit: "%", violationCount: 2 },
  { ruleType: "TAKE_PROFIT", label: "익절", color: RULE_COLORS.TAKE_PROFIT, thresholdValue: 30, thresholdUnit: "%", violationCount: 1 },
  { ruleType: "OVERTRADE_LIMIT", label: "과매매 제한", color: RULE_COLORS.OVERTRADE_LIMIT, thresholdValue: 10, thresholdUnit: "회", violationCount: 1 },
  { ruleType: "AVERAGING_LIMIT", label: "물타기 제한", color: RULE_COLORS.AVERAGING_LIMIT, thresholdValue: 2, thresholdUnit: "회", violationCount: 1 },
];

// ── 벤치마크 ──────────────────────────────────────────

export const benchmarks: BenchmarkItem[] = [
  { id: "btc-hold", label: "BTC만 홀드한 나", color: "#f7931a", profitRate: 11.8 },
];

// ── 규칙 위반 거래 목록 ──────────────────────────────────

export const violationTrades: ViolationTrade[] = [
  { id: 1, coinSymbol: "DOGE", date: "1/22", emotion: "FOMO", violatedRules: ["NO_CHASE_BUY"], profitLoss: -385_000 },
  { id: 2, coinSymbol: "SOL", date: "1/28", emotion: "감이 좋아서", violatedRules: ["NO_CHASE_BUY"], profitLoss: 120_000 },
  { id: 3, coinSymbol: "SHIB", date: "2/1", emotion: "복수 매매", violatedRules: ["STOP_LOSS", "AVERAGING_LIMIT"], profitLoss: -220_000 },
  { id: 4, coinSymbol: "AVAX", date: "2/5", emotion: "FOMO", violatedRules: ["STOP_LOSS"], profitLoss: -158_000 },
  { id: 5, coinSymbol: "BTC", date: "2/9", emotion: "복수 매매", violatedRules: ["OVERTRADE_LIMIT"], profitLoss: -52_000 },
];
