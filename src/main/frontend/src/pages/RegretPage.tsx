import { useState, useMemo, useEffect, useCallback } from "react";
import { Header } from "@/components/layout/Header";
import { BarChart3 } from "lucide-react";
import { RegretChart } from "@/components/regret/RegretChart";
import { MeVsMe } from "@/components/regret/MeVsMe";
import { ViolationTradeList } from "@/components/regret/ViolationTradeList";
import { computeSimulationLine } from "@/lib/types/regret";
import type { RuleType } from "@/lib/types/round";
import type { AssetSnapshot, RegretSummary, ViolationMarker, RuleToggleItem, BenchmarkItem, ViolationTrade } from "@/lib/types/regret";
import { useAuth } from "@/contexts/AuthContext";
import { useRound } from "@/contexts/RoundContext";
import { EXCHANGES } from "@/lib/types/coins";
import { getRegretReport, getRegretChart, type RegretReportData, type RegretChartData } from "@/lib/api/regret-api";

export function RegretPage() {
  const { user } = useAuth();
  const { activeRound } = useRound();

  const [enabledRules, setEnabledRules] = useState<Set<RuleType>>(
    new Set(["STOP_LOSS", "TAKE_PROFIT", "NO_CHASE_BUY", "AVERAGING_LIMIT", "OVERTRADE_LIMIT"]),
  );
  const [btcHoldEnabled, setBtcHoldEnabled] = useState(true);
  const [loading, setLoading] = useState(false);

  // API 데이터 상태
  const [summary, setSummary] = useState<RegretSummary | null>(null);
  const [snapshots, setSnapshots] = useState<AssetSnapshot[]>([]);
  const [markers, setMarkers] = useState<ViolationMarker[]>([]);
  const [btcHoldValues, setBtcHoldValues] = useState<number[]>([]);
  const [totalDays, setTotalDays] = useState(0);
  const [ruleToggles, setRuleToggles] = useState<RuleToggleItem[]>([]);
  const [benchmarks] = useState<BenchmarkItem[]>([
    { id: "btc-hold", label: "BTC만 홀드한 나", color: "#f7931a", profitRate: 0 },
  ]);
  const [violationTrades, setViolationTrades] = useState<ViolationTrade[]>([]);

  const loadRegretData = useCallback(async () => {
    if (!user || !activeRound) return;

    // 첫 번째 거래소의 exchangeId 사용
    const firstWallet = activeRound.wallets[0];
    if (!firstWallet) return;

    setLoading(true);
    try {
      const [reportData, chartData] = await Promise.all([
        getRegretReport(activeRound.roundId, firstWallet.exchangeId, user.userId),
        getRegretChart(activeRound.roundId, firstWallet.exchangeId, user.userId),
      ]);

      setSummary(reportData.summary);
      setRuleToggles(reportData.ruleToggles);
      setViolationTrades(reportData.violationTrades);

      setSnapshots(chartData.snapshots);
      setBtcHoldValues(chartData.btcHoldValues);
      setMarkers(chartData.markers);
      setTotalDays(chartData.totalDays);
    } catch (error) {
      console.error("Failed to load regret data", error);
    } finally {
      setLoading(false);
    }
  }, [user, activeRound]);

  useEffect(() => {
    void loadRegretData();
  }, [loadRegretData]);

  const simulationLine = useMemo(
    () => computeSimulationLine(snapshots, enabledRules),
    [snapshots, enabledRules],
  );

  const toggleRule = (ruleType: RuleType) => {
    setEnabledRules((prev) => {
      const next = new Set(prev);
      if (next.has(ruleType)) next.delete(ruleType);
      else next.add(ruleType);
      return next;
    });
  };

  return (
    <div className="min-h-screen bg-background">
      <Header />

      {/* Page header */}
      <section className="animate-enter border-b border-border/40 pb-6 pt-8">
        <div className="mx-auto max-w-6xl px-4">
          <div className="flex items-center gap-2.5">
            <BarChart3 className="h-6 w-6 text-primary" />
            <h1 className="font-display text-3xl tracking-tight">투자 복기</h1>
          </div>
          <p className="mt-2 text-sm text-muted-foreground">
            규칙만 지켰으면 얼마를 벌었을까?
          </p>
        </div>
      </section>

      <main className="mx-auto max-w-6xl px-4 py-6">
        {loading ? (
          <p className="text-sm text-muted-foreground">로딩 중...</p>
        ) : summary ? (
          <div className="space-y-6">
            <RegretChart
              summary={summary}
              snapshots={snapshots}
              markers={markers}
              simulationLine={simulationLine}
              btcHoldValues={btcHoldEnabled ? btcHoldValues : null}
              hasEnabledRules={enabledRules.size > 0}
              totalDays={totalDays}
            />

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-[380px_1fr]">
              <MeVsMe
                enabledRules={enabledRules}
                btcHoldEnabled={btcHoldEnabled}
                onToggleRule={toggleRule}
                onToggleBtcHold={() => setBtcHoldEnabled((v) => !v)}
                ruleToggles={ruleToggles}
                benchmarks={benchmarks}
              />
              <ViolationTradeList trades={violationTrades} />
            </div>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">
            {activeRound ? "복기 데이터를 불러올 수 없습니다." : "진행 중인 라운드가 없습니다."}
          </p>
        )}

        <p className="mt-3 text-[11px] text-muted-foreground/60">
          * 모의투자 데이터입니다. 규칙 준수 시 수익률은 시뮬레이션 결과입니다.
        </p>
      </main>
    </div>
  );
}
