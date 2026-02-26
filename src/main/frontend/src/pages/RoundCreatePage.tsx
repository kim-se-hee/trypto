import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Rocket } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useRound } from "@/contexts/RoundContext";
import { RoundCreateHeader } from "@/components/round/RoundCreateHeader";
import { SeedMoneyCard } from "@/components/round/SeedMoneyCard";
import {
  InvestmentRulesSection,
  getDefaultRules,
  type RulesMap,
} from "@/components/round/InvestmentRulesSection";
import type { RuleType } from "@/mocks/round";

export function RoundCreatePage() {
  const { user } = useAuth();
  const { createRound } = useRound();
  const navigate = useNavigate();

  const [seed, setSeed] = useState(0);
  const [emergencyLimit, setEmergencyLimit] = useState(0);
  const [rules, setRules] = useState<RulesMap>(getDefaultRules);

  function handleRuleToggle(type: RuleType, enabled: boolean) {
    setRules((prev) => ({ ...prev, [type]: { ...prev[type], enabled } }));
  }

  function handleRuleValueChange(type: RuleType, value: number) {
    setRules((prev) => ({ ...prev, [type]: { ...prev[type], value } }));
  }

  const enabledRules = Object.entries(rules).filter(([, r]) => r.enabled);
  const canSubmit = seed > 0 && emergencyLimit > 0 && enabledRules.length >= 1;

  function handleSubmit() {
    if (!canSubmit || !user) return;

    createRound({
      userId: user.userId,
      initialSeed: seed,
      emergencyFundingLimit: emergencyLimit,
      rules: enabledRules.map(([type, r]) => ({
        ruleType: type as RuleType,
        thresholdValue: r.value,
      })),
    });
    navigate("/market", { replace: true });
  }

  return (
    <div className="min-h-dvh bg-background">
      <RoundCreateHeader />

      {/* Hero */}
      <section className="bg-gradient-to-r from-primary/8 via-chart-2/6 to-primary/4 pb-8 pt-8">
        <div className="mx-auto max-w-2xl px-4">
          <h1 className="text-3xl font-extrabold tracking-tight">새 라운드</h1>
          <p className="mt-1.5 text-sm font-medium text-muted-foreground">
            시드머니와 투자 원칙을 설정하고 모의투자를 시작하세요
          </p>
        </div>
      </section>

      <main className="mx-auto max-w-2xl px-4 py-6">
        <div className="flex flex-col gap-8">
          {/* 시드머니 섹션 */}
          <div>
            <h2 className="mb-4 text-lg font-extrabold tracking-tight">자금 설정</h2>
            <SeedMoneyCard
              seed={seed}
              onSeedChange={setSeed}
              emergencyLimit={emergencyLimit}
              onEmergencyLimitChange={setEmergencyLimit}
            />
          </div>

          {/* 투자 원칙 섹션 */}
          <InvestmentRulesSection
            rules={rules}
            onRuleToggle={handleRuleToggle}
            onRuleValueChange={handleRuleValueChange}
          />

          {/* 제출 */}
          <div>
            <button
              disabled={!canSubmit}
              onClick={handleSubmit}
              className="flex h-12 w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-primary to-[#9A6AFF] text-sm font-bold text-white shadow-md transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg active:translate-y-0 disabled:pointer-events-none disabled:opacity-40"
            >
              <Rocket className="h-4 w-4" />
              라운드 시작하기
            </button>

            {!canSubmit && (
              <p className="mt-2 text-center text-[11px] text-muted-foreground">
                {seed <= 0 && "시드머니"}
                {seed <= 0 && emergencyLimit <= 0 && ", "}
                {emergencyLimit <= 0 && "긴급 자금 상한"}
                {(seed <= 0 || emergencyLimit <= 0) && enabledRules.length < 1 && ", "}
                {enabledRules.length < 1 && "투자 원칙 1개 이상"}
                {" "}설정이 필요합니다
              </p>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
