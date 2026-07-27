import { useState, useMemo } from "react";
import type { ViolationTrade } from "@/lib/types/regret";
import {
  RULE_LABELS,
  RULE_COLORS,
  EMOTION_STYLES,
} from "@/lib/types/regret";
import type { ExchangeFilter, ViolationFilter } from "@/lib/types/regret";
import { cn } from "@/lib/utils";
import { formatCurrencyCompact } from "@/lib/formatters";

const FILTER_TABS: { key: ViolationFilter; label: string }[] = [
  { key: "ALL", label: "전체" },
  { key: "LOSS", label: "손실" },
  { key: "PROFIT", label: "수익" },
];

/** 위반 손실은 양수가 손해다. 0 이하는 어긴 쪽이 오히려 이득이었던 거래다. */
function isLoss(trade: ViolationTrade): boolean {
  return trade.totalLossAmount > 0;
}

interface ViolationTradeListProps {
  trades: ViolationTrade[];
}

export function ViolationTradeList({ trades }: ViolationTradeListProps) {
  const [filter, setFilter] = useState<ViolationFilter>("ALL");
  const [exchangeFilter, setExchangeFilter] = useState<ExchangeFilter>("ALL");

  // 라운드에 위반이 있는 거래소만 축으로 세운다. 하나뿐이면 고를 것이 없어 감춘다.
  const exchanges = useMemo(() => {
    const byId = new Map<number, string>();
    for (const trade of trades) byId.set(trade.exchangeId, trade.exchangeName);
    return [...byId].map(([id, name]) => ({ id, name }));
  }, [trades]);

  const byExchange = useMemo(
    () => (exchangeFilter === "ALL" ? trades : trades.filter((t) => t.exchangeId === exchangeFilter)),
    [trades, exchangeFilter],
  );

  const filtered = useMemo(() => {
    if (filter === "LOSS") return byExchange.filter(isLoss);
    if (filter === "PROFIT") return byExchange.filter((t) => !isLoss(t));
    return byExchange;
  }, [filter, byExchange]);

  const counts = useMemo(() => {
    const loss = byExchange.filter(isLoss).length;
    return { all: byExchange.length, loss, profit: byExchange.length - loss };
  }, [byExchange]);

  return (
    <div className="rounded-xl border border-border bg-card p-5 sm:p-6">
      {/* 헤더 */}
      <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h2 className="text-lg font-bold">규칙 위반 거래</h2>

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

      {/* 거래소 필터 — 손익 필터와 조합해서 쓴다 */}
      {exchanges.length > 1 && (
        <div className="mb-4 flex flex-wrap gap-1.5">
          {[{ id: "ALL" as const, name: "전 거래소" }, ...exchanges].map((item) => (
            <button
              key={item.id}
              onClick={() => setExchangeFilter(item.id)}
              className={cn(
                "rounded-full border px-2.5 py-1 text-[11px] font-semibold transition-colors",
                exchangeFilter === item.id
                  ? "border-primary/40 bg-primary/10 text-primary"
                  : "border-border text-muted-foreground hover:text-foreground",
              )}
            >
              {item.name}
            </button>
          ))}
        </div>
      )}

      {/* 거래 목록 — 한 행은 주문 하나다 */}
      <div className="space-y-2">
        {filtered.map((trade) => {
          const loss = isLoss(trade);
          const emotionStyle = trade.emotion ? EMOTION_STYLES[trade.emotion] : null;

          return (
            <div
              key={trade.id}
              className="flex flex-col gap-2 rounded-xl border border-border/40 px-4 py-3 transition-colors hover:bg-primary/[0.02]"
            >
              <div className="flex items-center gap-3">
                <span className="text-sm font-bold">{trade.coinSymbol}</span>
                <span className="text-xs text-muted-foreground">{trade.date}</span>

                <span className="shrink-0 rounded-md bg-secondary/70 px-1.5 py-0.5 text-[10px] font-semibold text-muted-foreground">
                  {trade.exchangeName}
                </span>

                {emotionStyle && trade.emotion && (
                  <span
                    className={cn(
                      "shrink-0 rounded-md px-1.5 py-0.5 text-[10px] font-semibold",
                      emotionStyle.bg,
                      emotionStyle.text,
                    )}
                  >
                    {trade.emotion}
                  </span>
                )}

                <span
                  className={cn(
                    "ml-auto shrink-0 font-mono text-sm font-bold tabular-nums",
                    loss ? "text-negative" : "text-positive",
                  )}
                >
                  {trade.totalLossAmount < 0 ? "-" : ""}
                  {formatCurrencyCompact(Math.abs(trade.totalLossAmount), trade.currency)}
                </span>
              </div>

              {/* 어긴 원칙과 원칙마다의 금액 */}
              <div className="flex flex-wrap gap-1.5">
                {trade.violatedRules.map((rule) => (
                  <span
                    key={rule.ruleType}
                    className="shrink-0 rounded-md px-2 py-0.5 text-[11px] font-semibold"
                    style={{
                      backgroundColor: `${RULE_COLORS[rule.ruleType]}15`,
                      color: RULE_COLORS[rule.ruleType],
                    }}
                  >
                    {RULE_LABELS[rule.ruleType]}{" "}
                    <span className="font-mono tabular-nums">
                      {rule.lossAmount < 0 ? "-" : ""}
                      {formatCurrencyCompact(Math.abs(rule.lossAmount), trade.currency)}
                    </span>
                  </span>
                ))}
              </div>
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
