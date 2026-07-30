import { useMemo, useCallback } from "react";
import { cn } from "@/lib/utils";
import { formatPrice, formatQuantity, formatCurrencyCompact } from "@/lib/formatters";
import { SortIcon } from "@/components/ui/SortIcon";
import { useSort } from "@/hooks/useSort";
import type { SortDir } from "@/hooks/useSort";
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

// 좁은 화면에서는 일곱 칸이 들어가지 않는다. 코인·평가금액·수익률만 세우고,
// 접은 칸 가운데 수량과 평가손익은 남은 칸 아래에 덧붙여 보여준다.
const GRID_COLS =
  "grid-cols-[minmax(0,1fr)_auto_76px] sm:grid-cols-[1.4fr_minmax(80px,1fr)_minmax(80px,1fr)_minmax(80px,1fr)_minmax(80px,1fr)_minmax(80px,1fr)_minmax(75px,0.8fr)]";

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

  const columns: { key: SortKey; label: string; mobileHidden?: boolean }[] = [
    { key: "name", label: "코인명" },
    { key: "quantity", label: "보유수량", mobileHidden: true },
    { key: "avgBuyPrice", label: "평균매수가", mobileHidden: true },
    { key: "currentPrice", label: "현재가", mobileHidden: true },
    { key: "evalAmount", label: "평가금액" },
    { key: "profitLoss", label: "평가손익", mobileHidden: true },
    { key: "profitRate", label: "수익률" },
  ];

  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card">
      <div className="sm:overflow-x-auto">
        {/* Header */}
        <div className={cn("grid items-center gap-2 bg-secondary/30 px-3 py-3 sm:min-w-[700px] sm:gap-0 sm:px-5 sm:py-3.5", GRID_COLS)}>
          {columns.map((col) => (
            <button
              key={col.key}
              onClick={() => handleSort(col.key)}
              className={cn(
                "flex items-center gap-1 whitespace-nowrap text-[11px] font-medium text-muted-foreground transition-colors hover:text-foreground sm:text-xs",
                col.key !== "name" && "justify-end",
                col.mobileHidden && "hidden sm:flex",
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
                    "grid items-center gap-2 px-3 py-3.5 transition-colors hover:bg-primary/[0.03] sm:min-w-[700px] sm:gap-0 sm:px-5 sm:py-[18px]",
                    GRID_COLS,
                    i !== sorted.length - 1 && "border-b border-border/30",
                  )}
                >
                  {/* Coin info */}
                  <div className="flex min-w-0 items-center gap-3">
                    <div className="flex min-w-0 flex-col leading-tight">
                      <span className="text-[13px] font-semibold tracking-wide">{h.coinSymbol}</span>
                      <span className="truncate text-[11px] text-muted-foreground">{h.coinName}</span>
                      {/* 좁은 화면에서 접은 보유수량 칸을 이름 아래에 되살린다. */}
                      <span className="mt-0.5 font-mono text-[11px] tabular-nums text-muted-foreground/80 sm:hidden">
                        {formatQuantity(h.quantity)}
                      </span>
                    </div>
                  </div>

                  {/* Quantity */}
                  <div className="hidden whitespace-nowrap text-right font-mono text-sm tabular-nums sm:block">
                    {formatQuantity(h.quantity)}
                  </div>

                  {/* Avg buy price */}
                  <div className="hidden whitespace-nowrap text-right font-mono text-sm tabular-nums text-muted-foreground sm:block">
                    {formatPrice(h.avgBuyPrice, baseCurrency)}
                  </div>

                  {/* Current price */}
                  <div className="hidden whitespace-nowrap text-right font-mono text-sm font-semibold tabular-nums sm:block">
                    {formatPrice(h.currentPrice, baseCurrency)}
                  </div>

                  {/* Eval amount */}
                  <div className="whitespace-nowrap text-right font-mono text-[13px] tabular-nums sm:text-sm">
                    {formatCurrencyCompact(h.evalAmount, baseCurrency)}
                    {/* 좁은 화면에서 접은 평가손익 칸을 평가금액 아래에 되살린다. */}
                    <span className={cn(
                      "mt-0.5 block text-[11px] font-semibold sm:hidden",
                      isPositive && "text-positive",
                      isNegative && "text-negative",
                      !isPositive && !isNegative && "text-muted-foreground",
                    )}>
                      {isPositive ? "+" : ""}{formatCurrencyCompact(h.profitLoss, baseCurrency)}
                    </span>
                  </div>

                  {/* Profit/Loss */}
                  <div className={cn(
                    "hidden whitespace-nowrap text-right font-mono text-sm font-semibold tabular-nums sm:block",
                    isPositive && "text-positive",
                    isNegative && "text-negative",
                  )}>
                    {isPositive ? "+" : ""}{formatCurrencyCompact(h.profitLoss, baseCurrency)}
                  </div>

                  {/* Profit rate */}
                  <div className="flex justify-end">
                    <span className={cn(
                      "inline-block whitespace-nowrap rounded-full px-1.5 py-0.5 font-mono text-[11px] font-medium tabular-nums sm:px-2 sm:text-xs",
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
