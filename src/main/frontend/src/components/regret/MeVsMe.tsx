import {
  TrendingDown,
  TrendingUp,
  Ban,
  Layers,
  Timer,
  Crosshair,
  Check,
  type LucideIcon,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { RuleToggleItem, BenchmarkItem } from "@/lib/types/regret";
import type { RuleType } from "@/lib/types/round";

const RULE_ICON_MAP: Record<RuleType, LucideIcon> = {
  STOP_LOSS: TrendingDown,
  TAKE_PROFIT: TrendingUp,
  NO_CHASE_BUY: Ban,
  AVERAGING_LIMIT: Layers,
  OVERTRADE_LIMIT: Timer,
};

interface MeVsMeProps {
  enabledRules: Set<RuleType>;
  btcHoldEnabled: boolean;
  onToggleRule: (ruleType: RuleType) => void;
  onToggleBtcHold: () => void;
  ruleToggles: RuleToggleItem[];
  benchmarks: BenchmarkItem[];
}

export function MeVsMe({
  enabledRules,
  btcHoldEnabled,
  onToggleRule,
  onToggleBtcHold,
  ruleToggles,
  benchmarks,
}: MeVsMeProps) {
  return (
    <div className="rounded-xl border border-border bg-card p-5 sm:p-6">
      {/* 헤더 */}
      <div className="mb-5">
        <h2 className="flex items-center gap-2 text-lg font-bold">
          <Crosshair className="h-5 w-5 text-primary" />
          나 vs 나
        </h2>
        <p className="mt-1 text-xs font-medium text-muted-foreground">
          규칙을 켜고 끄면서 "어떤 규칙이 가장 돈이 됐는지" 확인
        </p>
      </div>

      {/* 규칙 토글 */}
      <div className="space-y-2">
        {ruleToggles.map((rule) => {
          const isEnabled = enabledRules.has(rule.ruleType);
          const Icon = RULE_ICON_MAP[rule.ruleType];
          return (
            <button
              key={rule.ruleType}
              onClick={() => onToggleRule(rule.ruleType)}
              className={cn(
                "flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left transition-all",
                isEnabled ? "bg-secondary/60" : "bg-transparent opacity-40",
              )}
            >
              <div
                className={cn(
                  "flex h-5 w-5 shrink-0 items-center justify-center rounded-md border-2 transition-colors",
                  isEnabled
                    ? "border-positive bg-positive text-white"
                    : "border-muted-foreground/30 bg-transparent",
                )}
              >
                {isEnabled && <Check className="h-3 w-3" strokeWidth={3} />}
              </div>

              <div
                className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg"
                style={{ backgroundColor: `${rule.color}18` }}
              >
                <Icon className="h-3.5 w-3.5" style={{ color: rule.color }} />
              </div>

              <span className="flex-1 text-sm font-semibold">{rule.label}</span>

              <span
                className="font-mono text-sm font-bold tabular-nums"
                style={{ color: rule.color }}
              >
                {rule.thresholdValue > 0 ? "+" : ""}
                {rule.thresholdValue}
                {rule.thresholdUnit}
              </span>

              <span className="rounded-md bg-negative/12 px-2 py-0.5 text-xs font-bold text-negative">
                위반 {rule.violationCount}
              </span>
            </button>
          );
        })}
      </div>

      {/* 벤치마크 */}
      <div className="mt-5 border-t border-border/60 pt-4">
        <p className="mb-3 text-[11px] font-medium text-muted-foreground">벤치마크</p>
        <div className="space-y-2">
          {benchmarks.map((bm) => {
            const isEnabled = bm.id === "btc-hold" ? btcHoldEnabled : false;
            const isPositive = bm.profitRate > 0;
            return (
              <button
                key={bm.id}
                onClick={bm.id === "btc-hold" ? onToggleBtcHold : undefined}
                className={cn(
                  "flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-all",
                  isEnabled ? "bg-secondary/40" : "opacity-40",
                )}
              >
                <div
                  className={cn(
                    "flex h-5 w-5 shrink-0 items-center justify-center rounded-md border-2 transition-colors",
                    isEnabled
                      ? "border-muted-foreground/50 bg-muted-foreground/50 text-white"
                      : "border-muted-foreground/30 bg-transparent",
                  )}
                >
                  {isEnabled && <Check className="h-3 w-3" strokeWidth={3} />}
                </div>

                <span
                  className="h-3 w-3 shrink-0 rounded-full"
                  style={{ backgroundColor: bm.color }}
                />
                <span className="flex-1 text-sm font-medium">{bm.label}</span>

                <span
                  className={cn(
                    "font-mono text-sm font-bold tabular-nums",
                    isPositive ? "text-positive" : "text-negative",
                  )}
                >
                  {isPositive ? "+" : ""}{bm.profitRate}%
                </span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
