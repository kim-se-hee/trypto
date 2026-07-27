import type { RuleType } from "./round";

// ── 타입 정의 ──────────────────────────────────────────

export interface AssetSnapshot {
  date: string;       // 표시용 라벨
  fullDate: string;   // yyyy-MM-dd (hover 상세용)
  actual: number;
  ruleFollowed: number; // 전체 규칙 준수
}

/** 라운드 전체를 합친 요약. 금액은 모두 원화다. */
export interface RegretSummary {
  /** 위반 손실 합계. 양수면 원칙을 지켰다면 더 벌었을 금액, 음수면 어긴 쪽이 이득이었던 금액이다. */
  totalViolationLoss: number;
  actualAsset: number;
  ruleFollowedAsset: number;
  totalViolations: number;
}

export interface RuleToggleItem {
  ruleType: RuleType;
  label: string;
  color: string;
  thresholdValue: number;
  thresholdUnit: string;
  violationCount: number;
  /** 이 원칙만 지켰다면 자산에 남았을 금액 (원화). */
  totalLossAmount: number;
}

export interface BenchmarkItem {
  id: string;
  label: string;
  color: string;
  profitRate: number;
}

export type ViolationEmotion = "FOMO" | "감이 좋아서" | "복수 매매";

export interface ViolatedRule {
  ruleType: RuleType;
  /** 이 거래에서 해당 원칙으로 발생한 위반 손실. 거래소 기축통화 단위이며, 양수면 손해다. */
  lossAmount: number;
  /** 같은 값의 원화 환산액. 거래소가 섞이는 그래프 계산은 이쪽을 쓴다. */
  lossAmountKrw: number;
}

/** 한 행은 주문 하나다. 한 주문이 여러 원칙을 어겼으면 행을 나누지 않고 violatedRules 에 나열한다. */
export interface ViolationTrade {
  id: number;
  coinSymbol: string;
  date: string;
  /** yyyy-MM-dd 로 잘라 그래프 날짜와 맞춘다. 표시용 date 는 연도가 없어 쓸 수 없다. */
  occurredAt: string;
  exchangeId: number;
  exchangeName: string;
  /** 발생 거래소의 기축통화 (KRW/USDT). 목록은 이 통화로 금액을 보여준다. */
  currency: string;
  emotion?: ViolationEmotion;
  violatedRules: ViolatedRule[];
  totalLossAmount: number;
  totalLossAmountKrw: number;
}

/** 손익 축. 위반 손실은 양수가 손해이므로 손실은 0 초과다. */
export type ViolationFilter = "ALL" | "LOSS" | "PROFIT";

/** 거래소 축. "ALL" 이면 전 거래소, 그 외에는 거래소 ID 다. */
export type ExchangeFilter = "ALL" | number;

export interface ViolationMarker {
  date: string;
  value: number;
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
 * 켜 둔 규칙이 실제로 유발한 위반 손실을 발생일 순으로 누적해 실제 자산에 더한다.
 *
 * 서버가 전체 규칙 곡선(`ruleFollowed`)을 만드는 방식과 같은 계산이므로, 규칙을 모두 켜면
 * 두 곡선이 모든 지점에서 일치한다. 위반이 없는 날은 직전 누적값을 유지해 계단 모양이 된다.
 *
 * 그래프는 라운드 전체를 원화로 합친 곡선이므로 거래소 기축통화가 아니라 원화 환산액을 쓴다.
 */
export function computeSimulationLine(
  snapshots: AssetSnapshot[],
  enabledRules: Set<RuleType>,
  violationTrades: ViolationTrade[],
): number[] {
  const dailyLosses = violationTrades
    .map((trade) => ({
      date: trade.occurredAt.slice(0, 10),
      amount: trade.violatedRules
        .filter((rule) => enabledRules.has(rule.ruleType))
        .reduce((sum, rule) => sum + rule.lossAmountKrw, 0),
    }))
    .sort((a, b) => a.date.localeCompare(b.date));

  let cumulative = 0;
  let next = 0;

  return snapshots.map((snapshot) => {
    // 그래프 시작일 이전에 발생한 위반은 첫 점에서 한꺼번에 반영된다.
    while (next < dailyLosses.length && dailyLosses[next].date <= snapshot.fullDate) {
      cumulative += dailyLosses[next].amount;
      next += 1;
    }
    return Math.round(snapshot.actual + cumulative);
  });
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
