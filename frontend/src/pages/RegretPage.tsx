import { useState, useMemo, useEffect, useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { ExchangeTabs } from "@/components/market/ExchangeTabs";
import { RegretChart } from "@/components/regret/RegretChart";
import { MeVsMe } from "@/components/regret/MeVsMe";
import { ViolationTradeList } from "@/components/regret/ViolationTradeList";
import { NoRoundNotice } from "@/components/round/NoRoundNotice";
import { computeSimulationLine } from "@/lib/types/regret";
import type { RuleType } from "@/lib/types/round";
import type { AssetSnapshot, RegretSummary, ViolationMarker, RuleToggleItem, BenchmarkItem, ViolationTrade } from "@/lib/types/regret";
import { useAuth } from "@/contexts/AuthContext";
import { useRound } from "@/contexts/RoundContext";
import { EXCHANGES } from "@/lib/types/coins";
import { getRegretReport, getRegretChart } from "@/lib/api/regret-api";

/** 복기 리포트는 야간 배치로 생성된다. 배치 전에는 서버가 0으로 채운 빈 리포트를 준다. */
const EMPTY_SUMMARY: RegretSummary = {
  missedProfit: 0,
  actualAsset: 0,
  ruleFollowedAsset: 0,
  totalViolations: 0,
};

export function RegretPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { user } = useAuth();
  const { activeRound } = useRound();

  // 거래소마다 기축통화가 달라 복기 지표는 합산하지 않는다. 탭으로 하나씩 본다.
  const exchangeTabItems = useMemo(() => {
    if (!activeRound) return [];
    return activeRound.wallets.map((w) => {
      const exchange = EXCHANGES.find((e) => e.id === w.exchangeId);
      return {
        id: exchange?.key ?? String(w.exchangeId),
        name: exchange?.name ?? String(w.exchangeId),
        baseCurrency: exchange?.baseCurrency ?? "",
      };
    });
  }, [activeRound]);

  const defaultExchange = exchangeTabItems[0]?.id ?? "upbit";
  const requestedExchange = searchParams.get("exchange") ?? defaultExchange;
  // 라운드에 없는 거래소가 쿼리로 들어오면 첫 번째 지갑으로 되돌린다.
  const selectedExchange = exchangeTabItems.some((e) => e.id === requestedExchange)
    ? requestedExchange
    : defaultExchange;
  const selectedExchangeItem = exchangeTabItems.find((e) => e.id === selectedExchange);
  const baseCurrency = selectedExchangeItem?.baseCurrency ?? "KRW";

  const [enabledRules, setEnabledRules] = useState<Set<RuleType>>(
    new Set(["STOP_LOSS", "TAKE_PROFIT", "NO_CHASE_BUY", "AVERAGING_LIMIT", "OVERTRADE_LIMIT"]),
  );
  const [btcHoldEnabled, setBtcHoldEnabled] = useState(true);
  const [loading, setLoading] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);

  // API 데이터 상태
  const [summary, setSummary] = useState<RegretSummary>(EMPTY_SUMMARY);
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

    const exchange = EXCHANGES.find((e) => e.key === selectedExchange);
    if (!exchange) return;

    const wallet = activeRound.wallets.find((w) => w.exchangeId === exchange.id);
    if (!wallet) return;

    setLoading(true);
    setLoadFailed(false);
    try {
      const [reportData, chartData] = await Promise.all([
        getRegretReport(activeRound.roundId, exchange.id, user.userId),
        getRegretChart(activeRound.roundId, exchange.id, user.userId),
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
      setLoadFailed(true);
    } finally {
      setLoading(false);
    }
  }, [user, activeRound, selectedExchange]);

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
          <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h1 className="font-display text-3xl tracking-tight">투자 복기</h1>
              <p className="mt-2 text-sm text-muted-foreground">
                규칙만 지켰으면 얼마를 벌었을까?
                {selectedExchangeItem && ` · ${selectedExchangeItem.name} ${baseCurrency} 기준`}
              </p>
            </div>
            <ExchangeTabs
              exchanges={exchangeTabItems}
              selected={selectedExchange}
              onSelect={(id) => setSearchParams({ exchange: id })}
            />
          </div>
        </div>
      </section>

      <main className="mx-auto max-w-6xl px-4 py-6">
        {loading ? (
          <p className="text-sm text-muted-foreground">로딩 중...</p>
        ) : !activeRound ? (
          <NoRoundNotice description="진행 중인 라운드가 없어 복기할 내역이 없습니다." />
        ) : loadFailed ? (
          <p className="text-sm text-muted-foreground">복기 데이터를 불러올 수 없습니다.</p>
        ) : (
          <div className="space-y-6">
            <RegretChart
              summary={summary}
              snapshots={snapshots}
              markers={markers}
              simulationLine={simulationLine}
              btcHoldValues={btcHoldEnabled ? btcHoldValues : null}
              hasEnabledRules={enabledRules.size > 0}
              totalDays={totalDays}
              baseCurrency={baseCurrency}
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
              <ViolationTradeList trades={violationTrades} baseCurrency={baseCurrency} />
            </div>
          </div>
        )}

        <p className="mt-3 text-[11px] text-muted-foreground/60">
          * 모의투자 데이터입니다. 규칙 준수 시 자산은 시뮬레이션 결과입니다.
        </p>
      </main>
    </div>
  );
}
