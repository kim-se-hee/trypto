import { apiGet } from "./client";
import { toFrontRuleType, type BackendRuleType } from "./mappers";
import type { RuleType } from "@/lib/types/round";
import type {
  AssetSnapshot,
  EmergencyCharge,
  RegretSummary,
  RuleToggleItem,
  ViolationMarker,
  ViolationTrade,
} from "@/lib/types/regret";
import { RULE_LABELS, RULE_COLORS } from "@/lib/types/regret";

// ── 백엔드 응답 타입 ──────────────────────────────────

interface BackendRuleImpact {
  ruleType: BackendRuleType;
  violationCount: number;
  thresholdValue: number;
  totalLossAmount: number;
}

interface BackendRealization {
  realizedOn: string;
  lossAmountKrw: number;
}

interface BackendViolatedRule {
  ruleType: BackendRuleType;
  lossAmount: number;
  lossAmountKrw: number;
  unrealizedLossAmountKrw: number;
  realizations: BackendRealization[];
}

interface BackendViolationDetail {
  violationDetailId: number;
  orderId: number | null;
  exchangeId: number;
  exchangeName: string;
  currency: string;
  coinSymbol: string;
  violatedRules: BackendViolatedRule[];
  totalLossAmount: number;
  totalLossAmountKrw: number;
  occurredAt: string;
}

interface BackendRegretReportResponse {
  roundId: number;
  totalViolationLoss: number;
  actualAsset: number;
  ruleFollowedAsset: number;
  totalViolations: number;
  ruleImpacts: BackendRuleImpact[];
  violationDetails: BackendViolationDetail[];
}

interface BackendAssetHistoryItem {
  snapshotDate: string;
  actualAsset: number;
  ruleFollowedAsset: number;
  btcHoldAsset: number;
}

interface BackendRegretChartResponse {
  roundId: number;
  totalDays: number;
  assetHistory: BackendAssetHistoryItem[];
  violationMarkers: Array<{
    snapshotDate: string;
    assetValue: number;
  }>;
  emergencyCharges: Array<{
    chargedDate: string;
    amount: number;
  }>;
}

// ── 프론트 변환 결과 타입 ──────────────────────────────

export interface RegretReportData {
  summary: RegretSummary;
  ruleToggles: RuleToggleItem[];
  violationTrades: ViolationTrade[];
}

export interface RegretChartData {
  snapshots: AssetSnapshot[];
  btcHoldValues: number[];
  markers: ViolationMarker[];
  totalDays: number;
  /** 원칙 골라 보기가 배수를 다시 굴릴 때 필요하다. */
  emergencyCharges: EmergencyCharge[];
}

// ── 단위 매핑 ──────────────────────────────────────────

const RULE_THRESHOLD_UNIT: Record<RuleType, string> = {
  STOP_LOSS: "%",
  TAKE_PROFIT: "%",
  NO_CHASE_BUY: "%",
  AVERAGING_LIMIT: "회",
  OVERTRADE_LIMIT: "회",
};

// ── 날짜 포맷 ──────────────────────────────────────────

function formatDateLabel(dateStr: string, totalDays: number): string {
  const d = new Date(dateStr);
  if (totalDays <= 180) return `${d.getMonth() + 1}/${d.getDate()}`;
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}`;
}

// ── API 호출 + 변환 ──────────────────────────────────

export async function getRegretReport(
  roundId: number,
  userId: number,
): Promise<RegretReportData> {
  const data = await apiGet<BackendRegretReportResponse>(
    `/api/rounds/${roundId}/regret`,
    { userId },
  );

  const summary: RegretSummary = {
    totalViolationLoss: Number(data.totalViolationLoss),
    actualAsset: Number(data.actualAsset),
    ruleFollowedAsset: Number(data.ruleFollowedAsset),
    totalViolations: data.totalViolations,
  };

  const ruleToggles: RuleToggleItem[] = data.ruleImpacts.map((impact) => {
    const ruleType = toFrontRuleType(impact.ruleType);
    return {
      ruleType,
      label: RULE_LABELS[ruleType],
      color: RULE_COLORS[ruleType],
      thresholdValue: Number(impact.thresholdValue),
      thresholdUnit: RULE_THRESHOLD_UNIT[ruleType],
      violationCount: impact.violationCount,
      totalLossAmount: Number(impact.totalLossAmount),
    };
  });

  const violationTrades: ViolationTrade[] = data.violationDetails.map((detail) => {
    const d = new Date(detail.occurredAt);
    return {
      id: detail.violationDetailId,
      coinSymbol: detail.coinSymbol,
      date: `${d.getMonth() + 1}/${d.getDate()}`,
      occurredAt: detail.occurredAt,
      exchangeId: detail.exchangeId,
      exchangeName: detail.exchangeName,
      currency: detail.currency,
      violatedRules: detail.violatedRules.map((rule) => ({
        ruleType: toFrontRuleType(rule.ruleType),
        lossAmount: Number(rule.lossAmount),
        lossAmountKrw: Number(rule.lossAmountKrw),
        unrealizedLossAmountKrw: Number(rule.unrealizedLossAmountKrw),
        realizations: (rule.realizations ?? []).map((realization) => ({
          realizedOn: realization.realizedOn,
          lossAmountKrw: Number(realization.lossAmountKrw),
        })),
      })),
      totalLossAmount: Number(detail.totalLossAmount),
      totalLossAmountKrw: Number(detail.totalLossAmountKrw),
    };
  });

  return { summary, ruleToggles, violationTrades };
}

export async function getRegretChart(
  roundId: number,
  userId: number,
): Promise<RegretChartData> {
  const data = await apiGet<BackendRegretChartResponse>(
    `/api/rounds/${roundId}/regret/chart`,
    { userId },
  );

  // 서버가 주는 총 일수는 첫 스냅샷과 마지막 스냅샷의 날짜 차이다. 스냅샷이 빠진 날이 있으면
  // 개수와 달라지므로, 라벨 간격을 정하는 기준은 서버 값을 그대로 쓴다.
  const totalDays = data.totalDays;

  const snapshots: AssetSnapshot[] = data.assetHistory.map((item) => ({
    date: formatDateLabel(item.snapshotDate, totalDays),
    fullDate: item.snapshotDate,
    actual: Number(item.actualAsset),
    ruleFollowed: Number(item.ruleFollowedAsset),
  }));

  const btcHoldValues = data.assetHistory.map((item) => Number(item.btcHoldAsset));

  const markers: ViolationMarker[] = (data.violationMarkers ?? []).map((m) => ({
    date: formatDateLabel(m.snapshotDate, totalDays),
    value: Number(m.assetValue),
  }));

  const emergencyCharges: EmergencyCharge[] = (data.emergencyCharges ?? []).map((charge) => ({
    chargedDate: charge.chargedDate,
    amount: Number(charge.amount),
  }));

  return { snapshots, btcHoldValues, markers, totalDays, emergencyCharges };
}
