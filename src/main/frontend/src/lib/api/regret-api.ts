import { apiGet } from "./client";
import { toFrontRuleType, type BackendRuleType } from "./mappers";
import type { RuleType } from "@/lib/types/round";
import type {
  AssetSnapshot,
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
}

interface BackendViolationDetail {
  violationId: number;
  coinSymbol: string;
  ruleType: BackendRuleType;
  profitLoss: number;
  occurredAt: string;
}

interface BackendRegretReportResponse {
  missedProfit: number;
  actualProfitRate: number;
  ruleFollowedProfitRate: number;
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
  assetHistory: BackendAssetHistoryItem[];
  violationMarkers: Array<{
    date: string;
    value: number;
    type: "loss" | "gain";
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
  exchangeId: number,
  userId: number,
): Promise<RegretReportData> {
  const data = await apiGet<BackendRegretReportResponse>(
    `/api/rounds/${roundId}/regret`,
    { exchangeId, userId },
  );

  const summary: RegretSummary = {
    missedProfit: Number(data.missedProfit),
    actualProfitRate: Number(data.actualProfitRate),
    ruleFollowedProfitRate: Number(data.ruleFollowedProfitRate),
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
    };
  });

  // 동일 coinSymbol + 동일 시각의 위반을 그룹핑
  const violationMap = new Map<string, ViolationTrade>();
  for (const detail of data.violationDetails) {
    const key = `${detail.coinSymbol}-${detail.occurredAt}`;
    const existing = violationMap.get(key);
    const ruleType = toFrontRuleType(detail.ruleType);

    if (existing) {
      if (!existing.violatedRules.includes(ruleType)) {
        existing.violatedRules.push(ruleType);
      }
    } else {
      const d = new Date(detail.occurredAt);
      violationMap.set(key, {
        id: detail.violationId,
        coinSymbol: detail.coinSymbol,
        date: `${d.getMonth() + 1}/${d.getDate()}`,
        violatedRules: [ruleType],
        profitLoss: Number(detail.profitLoss),
      });
    }
  }

  return {
    summary,
    ruleToggles,
    violationTrades: Array.from(violationMap.values()),
  };
}

export async function getRegretChart(
  roundId: number,
  exchangeId: number,
  userId: number,
): Promise<RegretChartData> {
  const data = await apiGet<BackendRegretChartResponse>(
    `/api/rounds/${roundId}/regret/chart`,
    { exchangeId, userId },
  );

  const totalDays = data.assetHistory.length;

  const snapshots: AssetSnapshot[] = data.assetHistory.map((item) => ({
    date: formatDateLabel(item.snapshotDate, totalDays),
    fullDate: item.snapshotDate,
    actual: Number(item.actualAsset),
    ruleFollowed: Number(item.ruleFollowedAsset),
  }));

  const btcHoldValues = data.assetHistory.map((item) => Number(item.btcHoldAsset));

  const markers: ViolationMarker[] = (data.violationMarkers ?? []).map((m) => ({
    date: formatDateLabel(m.date, totalDays),
    value: Number(m.value),
    type: m.type,
  }));

  return { snapshots, btcHoldValues, markers, totalDays };
}
