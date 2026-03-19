import { useMemo, useCallback } from "react";
import { cn } from "@/lib/utils";
import { formatPrice, formatQuantity, formatCurrencyCompact } from "@/lib/formatters";
import { SortIcon } from "@/components/ui/SortIcon";
import { useSort } from "@/hooks/useSort";
import type { SortDir } from "@/hooks/useSort";
import { CoinIcon } from "@/components/market/CoinIcon";
import type { HoldingData } from "@/lib/types/portfolio";

interface HoldingsTableProps {
  holdings: HoldingData[];
  baseCurrency: string;
}

type SortKey = "name" | "quantity" | "avgBuyPrice" | "currentPrice" | "evalAmount" | "profitLoss" | "profitRate";

interface ComputedHolding extends HoldingData {
  evalAmount: number;
  profitLoss: number;
  profitRate: number;
}

function computeHolding(h: HoldingData): ComputedHolding {
  const evalAmount = h.currentPrice * h.quantity;
  const buyAmount = h.avgBuyPrice * h.quantity;
  const profitLoss = evalAmount - buyAmount;
  const profitRate = buyAmount > 0 ? (profitLoss / buyAmount) * 100 : 0;
  return { ...h, evalAmount, profitLoss, profitRate };
}

const GRID_COLS = "grid-cols-[1.4fr_minmax(80px,1fr)_minmax(80px,1fr)_minmax(80px,1fr)_minmax(80px,1fr)_minmax(80px,1fr)_minmax(75px,0.8fr)]";

export function HoldingsTable({ holdings, baseCurrency }: HoldingsTableProps) {
  const computed = useMemo(() => holdings.map(computeHolding), [holdings]);

  const comparator = useCallback((key: SortKey, dir: SortDir) => {
    return (a: ComputedHolding, b: ComputedHolding) => {
      let cmp = 0;
      switch (key) {
        case "name": cmp = a.coinSymbol.localeCompare(b.coinSymbol); break;
        case "quantity": cmp = a.quantity - b.quantity; break;
        case "avgBuyPrice": cmp = a.avgBuyPrice - b.avgBuyPrice; break;
        case "currentPrice": cmp = a.currentPrice - b.currentPrice; break;
        case "evalAmount": cmp = a.evalAmount - b.evalAmount; break;
        case "profitLoss": cmp = a.profitLoss - b.profitLoss; break;
        case "profitRate": cmp = a.profitRate - b.profitRate; break;
      }
      return dir === "asc" ? cmp : -cmp;
    };
  }, []);

  const { sorted, sortKey, sortDir, handleSort } = useSort<ComputedHolding, SortKey>({
    items: computed,
    comparator,
  });

  const columns: { key: SortKey; label: string }[] = [
    { key: "name", label: "코인명" },
    { key: "quantity", label: "보유수량" },
    { key: "avgBuyPrice", label: "평균매수가" },
    { key: "currentPrice", label: "현재가" },
    { key: "evalAmount", label: "평가금액" },
    { key: "profitLoss", label: "평가손익" },
    { key: "profitRate", label: "수익률" },
  ];

  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card">
      <div className="overflow-x-auto">
        {/* Header */}
        <div className={cn("grid min-w-[700px] items-center bg-secondary/30 px-5 py-3.5", GRID_COLS)}>
          {columns.map((col) => (
            <button
              key={col.key}
              onClick={() => handleSort(col.key)}
              className={cn(
                "flex items-center gap-1 whitespace-nowrap text-xs font-medium text-muted-foreground transition-colors hover:text-foreground",
                col.key !== "name" && "justify-end",
              )}
            >
              {col.key !== "name" && <SortIcon column={col.key} activeColumn={sortKey} direction={sortDir} />}
              {col.label}
              {col.key === "name" && <SortIcon column="name" activeColumn={sortKey} direction={sortDir} />}
            </button>
          ))}
        </div>

        {/* Body */}
        <div>
          {sorted.length === 0 ? (
            <div className="flex h-48 items-center justify-center text-sm text-muted-foreground">
              보유 중인 코인이 없습니다.
            </div>
          ) : (
            sorted.map((h, i) => {
              const isPositive = h.profitLoss > 0;
              const isNegative = h.profitLoss < 0;
              return (
                <div
                  key={h.coinSymbol}
                  className={cn(
                    "grid min-w-[700px] items-center px-5 py-[18px] transition-colors hover:bg-primary/[0.03]",
                    GRID_COLS,
                    i !== sorted.length - 1 && "border-b border-border/30",
                  )}
                >
                  {/* Coin info */}
                  <div className="flex items-center gap-3">
                    <CoinIcon symbol={h.coinSymbol} size={32} />
                    <div className="flex flex-col leading-tight">
                      <span className="text-[13px] font-semibold tracking-wide">{h.coinSymbol}</span>
                      <span className="text-[11px] text-muted-foreground">{h.coinName}</span>
                    </div>
                  </div>

                  {/* Quantity */}
                  <div className="whitespace-nowrap text-right font-mono text-sm tabular-nums">
                    {formatQuantity(h.quantity)}
                  </div>

                  {/* Avg buy price */}
                  <div className="whitespace-nowrap text-right font-mono text-sm tabular-nums text-muted-foreground">
                    {formatPrice(h.avgBuyPrice, baseCurrency)}
                  </div>

                  {/* Current price */}
                  <div className="whitespace-nowrap text-right font-mono text-sm font-semibold tabular-nums">
                    {formatPrice(h.currentPrice, baseCurrency)}
                  </div>

                  {/* Eval amount */}
                  <div className="whitespace-nowrap text-right font-mono text-sm tabular-nums">
                    {formatCurrencyCompact(h.evalAmount, baseCurrency)}
                  </div>

                  {/* Profit/Loss */}
                  <div className={cn(
                    "whitespace-nowrap text-right font-mono text-sm font-semibold tabular-nums",
                    isPositive && "text-positive",
                    isNegative && "text-negative",
                  )}>
                    {isPositive ? "+" : ""}{formatCurrencyCompact(h.profitLoss, baseCurrency)}
                  </div>

                  {/* Profit rate */}
                  <div className="flex justify-end">
                    <span className={cn(
                      "inline-block whitespace-nowrap rounded-full px-2 py-0.5 font-mono text-xs font-medium tabular-nums",
                      isPositive && "bg-positive/15 text-positive",
                      isNegative && "bg-negative/20 text-negative",
                      !isPositive && !isNegative && "text-muted-foreground",
                    )}>
                      {isPositive ? "+" : ""}{h.profitRate.toFixed(2)}%
                    </span>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}
