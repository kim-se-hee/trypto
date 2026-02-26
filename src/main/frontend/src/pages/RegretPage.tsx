import { useState, useMemo } from "react";
import { Header } from "@/components/layout/Header";
import { BarChart3 } from "lucide-react";
import { RegretChart } from "@/components/regret/RegretChart";
import { MeVsMe } from "@/components/regret/MeVsMe";
import { ViolationTradeList } from "@/components/regret/ViolationTradeList";
import { regretData, computeSimulationLine } from "@/mocks/regret";
import type { RuleType } from "@/mocks/round";

export function RegretPage() {
  const [enabledRules, setEnabledRules] = useState<Set<RuleType>>(
    new Set(["STOP_LOSS", "TAKE_PROFIT", "NO_CHASE_BUY", "AVERAGING_LIMIT", "OVERTRADE_LIMIT"]),
  );
  const [btcHoldEnabled, setBtcHoldEnabled] = useState(true);

  const simulationLine = useMemo(
    () => computeSimulationLine(regretData.snapshots, enabledRules),
    [enabledRules],
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

      {/* Hero */}
      <section className="bg-gradient-to-r from-primary/8 via-chart-3/6 to-primary/4 pb-8 pt-8">
        <div className="mx-auto max-w-6xl px-4">
          <div className="flex items-center gap-2.5">
            <BarChart3 className="h-7 w-7 text-primary" />
            <h1 className="text-3xl font-extrabold tracking-tight">투자 복기</h1>
          </div>
          <p className="mt-1.5 text-sm font-medium text-muted-foreground">
            규칙만 지켰으면 얼마를 벌었을까?
          </p>
        </div>
      </section>

      <main className="mx-auto max-w-6xl px-4 py-6">
        <div className="space-y-6">
          <RegretChart
            summary={regretData.summary}
            snapshots={regretData.snapshots}
            markers={regretData.markers}
            simulationLine={simulationLine}
            btcHoldValues={btcHoldEnabled ? regretData.btcHoldValues : null}
            hasEnabledRules={enabledRules.size > 0}
            totalDays={regretData.totalDays}
          />

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-[380px_1fr]">
            <MeVsMe
              enabledRules={enabledRules}
              btcHoldEnabled={btcHoldEnabled}
              onToggleRule={toggleRule}
              onToggleBtcHold={() => setBtcHoldEnabled((v) => !v)}
            />
            <ViolationTradeList />
          </div>
        </div>

        <p className="mt-3 text-[11px] text-muted-foreground/60">
          * 모의투자 데이터입니다. 규칙 준수 시 수익률은 시뮬레이션 결과입니다.
        </p>
      </main>
    </div>
  );
}
