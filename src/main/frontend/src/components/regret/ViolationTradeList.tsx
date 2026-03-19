import { useState, useMemo } from "react";
import {
  TriangleAlert,
  TrendingDown,
  TrendingUp,
  Ban,
  Layers,
  Timer,
  Flame,
  Target,
  Swords,
  type LucideIcon,
} from "lucide-react";
import { CoinIcon } from "@/components/market/CoinIcon";
import type { ViolationTrade } from "@/lib/types/regret";
import {
  RULE_LABELS,
  RULE_COLORS,
  EMOTION_STYLES,
} from "@/lib/types/regret";
import type { ViolationFilter, ViolationEmotion } from "@/lib/types/regret";
import type { RuleType } from "@/lib/types/round";
import { cn } from "@/lib/utils";

const RULE_ICON_MAP: Record<RuleType, LucideIcon> = {
  STOP_LOSS: TrendingDown,
  TAKE_PROFIT: TrendingUp,
  NO_CHASE_BUY: Ban,
  AVERAGING_LIMIT: Layers,
  OVERTRADE_LIMIT: Timer,
};

const EMOTION_ICON_MAP: Record<ViolationEmotion, { icon: LucideIcon; label: string }> = {
  FOMO: { icon: Flame, label: "FOMO" },
  "감이 좋아서": { icon: Target, label: "감이 좋아서" },
  "복수 매매": { icon: Swords, label: "복수 매매" },
};

const FILTER_TABS: { key: ViolationFilter; label: string }[] = [
  { key: "ALL", label: "전체" },
  { key: "LOSS", label: "손실" },
  { key: "PROFIT", label: "수익" },
];

interface ViolationTradeListProps {
  trades: ViolationTrade[];
}

export function ViolationTradeList({ trades }: ViolationTradeListProps) {
  const [filter, setFilter] = useState<ViolationFilter>("ALL");

  const filtered = useMemo(() => {
    if (filter === "LOSS") return trades.filter((t) => t.profitLoss < 0);
    if (filter === "PROFIT") return trades.filter((t) => t.profitLoss >= 0);
    return trades;
  }, [filter, trades]);

  const counts = useMemo(() => {
    const loss = trades.filter((t) => t.profitLoss < 0).length;
    const profit = trades.filter((t) => t.profitLoss >= 0).length;
    return { all: trades.length, loss, profit };
  }, [trades]);

  return (
    <div className="rounded-xl border border-border bg-card p-5 sm:p-6">
      {/* 헤더 */}
      <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h2 className="flex items-center gap-2 text-lg font-bold">
          <TriangleAlert className="h-5 w-5 text-negative" />
          규칙 위반 거래
        </h2>

        <div className="flex gap-1.5 rounded-lg bg-secondary/60 p-1">
          {FILTER_TABS.map((tab) => {
            const count =
              tab.key === "ALL" ? counts.all : tab.key === "LOSS" ? counts.loss : counts.profit;
            return (
              <button
                key={tab.key}
                onClick={() => setFilter(tab.key)}
                className={cn(
                  "rounded-md px-3 py-1 text-xs font-semibold transition-all",
                  filter === tab.key
                    ? "bg-card text-foreground shadow-sm"
                    : "text-muted-foreground hover:text-foreground",
                )}
              >
                {tab.label} {count}
              </button>
            );
          })}
        </div>
      </div>

      {/* 거래 목록 */}
      <div className="space-y-2">
        {filtered.map((trade) => {
          const isLoss = trade.profitLoss < 0;
          const emotionStyle = trade.emotion ? EMOTION_STYLES[trade.emotion] : null;
          const emotionInfo = trade.emotion ? EMOTION_ICON_MAP[trade.emotion] : null;

          return (
            <div
              key={trade.id}
              className="flex items-center gap-3 rounded-xl border border-border/40 px-4 py-3 transition-colors hover:bg-primary/[0.02]"
            >
              <CoinIcon symbol={trade.coinSymbol} size={28} />

              <span className="text-sm font-bold">{trade.coinSymbol}</span>
              <span className="text-xs text-muted-foreground">{trade.date}</span>

              {emotionStyle && emotionInfo && (() => {
                const EmotionIcon = emotionInfo.icon;
                return (
                  <span
                    className={cn(
                      "flex shrink-0 items-center gap-1 rounded-md px-1.5 py-0.5 text-[10px] font-semibold",
                      emotionStyle.bg,
                      emotionStyle.text,
                    )}
                  >
                    <EmotionIcon className="h-3 w-3" />
                    {emotionInfo.label}
                  </span>
                );
              })()}

              {/* 위반 규칙 태그 */}
              {trade.violatedRules.map((ruleType) => {
                const Icon = RULE_ICON_MAP[ruleType];
                return (
                  <span
                    key={ruleType}
                    className="flex shrink-0 items-center gap-1 rounded-md px-2 py-0.5 text-[11px] font-semibold"
                    style={{
                      backgroundColor: `${RULE_COLORS[ruleType]}15`,
                      color: RULE_COLORS[ruleType],
                    }}
                  >
                    <Icon className="h-3 w-3" />
                    {RULE_LABELS[ruleType]}
                  </span>
                );
              })}

              <span
                className={cn(
                  "ml-auto shrink-0 font-mono text-sm font-bold tabular-nums",
                  isLoss ? "text-negative" : "text-positive",
                )}
              >
                {isLoss ? "" : "+"}
                {trade.profitLoss.toLocaleString("ko-KR")}
              </span>
            </div>
          );
        })}
      </div>

      {filtered.length === 0 && (
        <div className="py-8 text-center text-sm text-muted-foreground">
          해당 조건의 위반 거래가 없습니다.
        </div>
      )}
    </div>
  );
}
