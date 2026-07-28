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
  /** 벤치마크 수익률(%). 계산 근거가 부족하면 null 이며, 이때는 수치를 표시하지 않는다. */
  profitRate: number | null;
}

export type ViolationEmotion = "FOMO" | "감이 좋아서" | "복수 매매";

/** 매도로 확정된 위반 손실 한 조각. 실현일 이후로는 금액이 아니라 배수로 곡선에 반영된다. */
export interface Realization {
  /** yyyy-MM-dd */
  realizedOn: string;
  lossAmountKrw: number;
}

export interface ViolatedRule {
  ruleType: RuleType;
  /** 이 거래에서 해당 원칙으로 발생한 위반 손실. 거래소 기축통화 단위이며, 양수면 손해다. */
  lossAmount: number;
  /** 같은 값의 원화 환산액. 거래소가 섞이는 그래프 계산은 이쪽을 쓴다. */
  lossAmountKrw: number;
  /** 아직 매도되지 않아 금액이 현재가를 따라 움직이는 몫 (원화). */
  unrealizedLossAmountKrw: number;
  /** 매도로 확정된 몫 (원화). 실현일마다 한 조각이다. */
  realizations: Realization[];
}

/** 라운드에 새로 들어온 돈. 위반과 무관하므로 충전일의 배수를 1 쪽으로 되돌린다. */
export interface EmergencyCharge {
  /** yyyy-MM-dd */
  chargedDate: string;
  amount: number;
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
 * 켜 둔 규칙이 실제로 유발한 위반 손실만 골라 그날의 실제 자산에 반영한다.
 *
 * 아직 매도되지 않은 몫은 금액을 그대로 더하고, 매도로 확정된 몫은 실현일의 자산 대비 비율(배수)로
 * 환산해 곱한다. 실현된 돈은 지갑에 섞여 이후 투자와 함께 굴러가므로, 확정 금액을 계속 빼면 시장이
 * 이미 가져간 금액을 한 번 더 빼는 이중 차감이 되기 때문이다. 긴급 충전은 위반과 무관한 새 돈이라
 * 그날의 배수를 1 쪽으로 되돌린다.
 *
 * 서버가 전체 규칙 곡선(`ruleFollowed`)을 만드는 방식과 같은 계산이므로, 규칙을 모두 켜면 두 곡선이
 * 모든 지점에서 일치한다. 규칙 조합이 바뀌면 배수도 처음부터 다시 굴려야 한다.
 *
 * 그래프는 라운드 전체를 원화로 합친 곡선이므로 거래소 기축통화가 아니라 원화 환산액을 쓴다.
 */
export function computeSimulationLine(
  snapshots: AssetSnapshot[],
  enabledRules: Set<RuleType>,
  violationTrades: ViolationTrade[],
  emergencyCharges: EmergencyCharge[] = [],
): number[] {
  const enabledRulesOf = (trade: ViolationTrade) =>
    trade.violatedRules.filter((rule) => enabledRules.has(rule.ruleType));

  const occurredLosses = violationTrades
    .map((trade) => ({
      date: trade.occurredAt.slice(0, 10),
      amount: enabledRulesOf(trade).reduce((sum, rule) => sum + rule.lossAmountKrw, 0),
    }))
    .sort((a, b) => a.date.localeCompare(b.date));

  const realizations = violationTrades
    .flatMap((trade) =>
      enabledRulesOf(trade).flatMap((rule) =>
        rule.realizations.map((realization) => ({
          date: realization.realizedOn,
          amount: realization.lossAmountKrw,
        })),
      ),
    )
    .sort((a, b) => a.date.localeCompare(b.date));

  const charges = [...emergencyCharges].sort((a, b) => a.chargedDate.localeCompare(b.chargedDate));

  let occurred = 0;
  let realized = 0;
  let multiplier = 1;
  let nextLoss = 0;
  let nextRealization = 0;
  let nextCharge = 0;

  return snapshots.map((snapshot) => {
    // 그래프 시작일 이전에 발생한 위반과 실현도 첫 점에서 한꺼번에 반영된다.
    while (nextLoss < occurredLosses.length && occurredLosses[nextLoss].date <= snapshot.fullDate) {
      occurred += occurredLosses[nextLoss].amount;
      nextLoss += 1;
    }

    let realizedToday = 0;
    while (nextRealization < realizations.length && realizations[nextRealization].date <= snapshot.fullDate) {
      realizedToday += realizations[nextRealization].amount;
      nextRealization += 1;
    }

    let chargedToday = 0;
    while (nextCharge < charges.length && charges[nextCharge].chargedDate <= snapshot.fullDate) {
      chargedToday += charges[nextCharge].amount;
      nextCharge += 1;
    }

    realized += realizedToday;
    multiplier = nextMultiplier(multiplier, snapshot.actual, chargedToday, realizedToday);

    return Math.round((snapshot.actual + occurred - realized) * multiplier);
  });
}

/** 실현도 충전도 없는 날은 배수가 그대로다. 자산은 음수가 될 수 없으므로 배수의 하한은 0 이다. */
function nextMultiplier(
  multiplier: number,
  totalAsset: number,
  charged: number,
  realized: number,
): number {
  if (totalAsset <= 0 || (charged === 0 && realized === 0)) return multiplier;
  return Math.max(0, (multiplier * (totalAsset - charged + realized) + charged) / totalAsset);
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

/** BTC 홀드 벤치마크 수익률(%). 평가액이 2개 미만이거나 시작 평가액이 0 이면 계산할 수 없다. */
export function btcHoldProfitRate(btcHoldValues: number[]): number | null {
  if (btcHoldValues.length < 2) return null;
  const first = btcHoldValues[0];
  if (first === 0) return null;
  return (btcHoldValues[btcHoldValues.length - 1] / first - 1) * 100;
}
