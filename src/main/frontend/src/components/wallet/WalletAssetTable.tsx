import { useState, useMemo, useCallback } from "react";
import { Search, Lock } from "lucide-react";
import { cn } from "@/lib/utils";
import { CoinIcon } from "@/components/market/CoinIcon";
import { formatQuantity, formatCurrencyCompact, SMALL_AMOUNT_THRESHOLD } from "@/lib/formatters";
import { SortIcon } from "@/components/ui/SortIcon";
import { useSort } from "@/hooks/useSort";
import type { SortDir } from "@/hooks/useSort";
import type { WalletCoinBalance } from "@/lib/types/wallet";

interface WalletAssetTableProps {
  balances: WalletCoinBalance[];
  baseCurrency: string;
  onSelectCoin?: (coin: WalletCoinBalance | null) => void;
  selectedCoin?: string | null;
}

type SortKey = "name" | "total" | "available" | "locked";

interface ComputedBalance extends WalletCoinBalance {
  total: number;
  totalValue: number;
}

function computeBalance(b: WalletCoinBalance): ComputedBalance {
  const total = b.available + b.locked;
  const totalValue = total * b.currentPrice;
  return { ...b, total, totalValue };
}

function formatDisplayQuantity(quantity: number, symbol: string, baseCurrency: string): string {
  if (symbol === baseCurrency) return quantity.toLocaleString("ko-KR");
  return formatQuantity(quantity);
}

const GRID_COLS = "grid-cols-[1.6fr_minmax(120px,1.2fr)_minmax(100px,1fr)_minmax(90px,0.8fr)]";

export function WalletAssetTable({ balances, baseCurrency, onSelectCoin, selectedCoin }: WalletAssetTableProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [hideSmall, setHideSmall] = useState(false);

  const computed = useMemo(() => balances.map(computeBalance), [balances]);

  const filtered = useMemo(() => {
    let result = computed;

    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      result = result.filter(
        (b) => b.coinSymbol.toLowerCase().includes(q) || b.coinName.toLowerCase().includes(q),
      );
    }

    if (hideSmall) {
      const threshold = SMALL_AMOUNT_THRESHOLD[baseCurrency] ?? 1;
      result = result.filter((b) => b.totalValue >= threshold);
    }

    return result;
  }, [computed, searchQuery, hideSmall, baseCurrency]);

  const comparator = useCallback(
    (key: SortKey, dir: SortDir) => (a: ComputedBalance, b: ComputedBalance) => {
      let cmp = 0;
      switch (key) {
        case "name": cmp = a.coinSymbol.localeCompare(b.coinSymbol); break;
        case "total": cmp = a.totalValue - b.totalValue; break;
        case "available": cmp = (a.available * a.currentPrice) - (b.available * b.currentPrice); break;
        case "locked": cmp = (a.locked * a.currentPrice) - (b.locked * b.currentPrice); break;
      }
      return dir === "asc" ? cmp : -cmp;
    },
    [],
  );

  const { sorted, sortKey, sortDir, handleSort } = useSort<ComputedBalance, SortKey>({
    items: filtered,
    defaultKey: "total",
    defaultDir: "desc",
    comparator,
  });

  const columns: { key: SortKey; label: string }[] = [
    { key: "name", label: "코인" },
    { key: "total", label: "보유수량" },
    { key: "available", label: "사용가능" },
    { key: "locked", label: "잠금" },
  ];

  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border/30 px-5 py-4">
        <h3 className="text-lg font-bold">보유 자산</h3>
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground/50" />
            <input
              type="text"
              placeholder="코인 검색"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              aria-label="코인 검색"
              className="h-9 w-48 rounded-full border border-border/50 bg-secondary/30 pl-9 pr-3 text-sm outline-none transition-colors placeholder:text-muted-foreground/40 focus:border-primary/40 focus:bg-white"
            />
          </div>
          <label className="flex cursor-pointer items-center gap-1.5 text-xs font-medium text-muted-foreground select-none">
            <input
              type="checkbox"
              checked={hideSmall}
              onChange={(e) => setHideSmall(e.target.checked)}
              className="h-3.5 w-3.5 rounded border-border accent-primary"
            />
            소액 제외
          </label>
        </div>
      </div>

      <div className="overflow-x-auto">
        {/* Header */}
        <div className={cn("grid min-w-[640px] items-center bg-secondary/30 px-5 py-3.5", GRID_COLS)} role="row">
          {columns.map((col) => (
            <button
              key={col.key}
              onClick={() => handleSort(col.key)}
              aria-sort={sortKey === col.key ? (sortDir === "asc" ? "ascending" : "descending") : "none"}
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
              보유 중인 자산이 없습니다.
            </div>
          ) : (
            sorted.map((b, i) => {
              const isSelected = selectedCoin === b.coinSymbol;
              const isBase = b.coinSymbol === baseCurrency;
              const hasBalance = isBase || b.total > 0;
              return (
                <div
                  key={b.coinSymbol}
                  onClick={() => onSelectCoin?.(isSelected ? null : b)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => { if (e.key === "Enter") onSelectCoin?.(isSelected ? null : b); }}
                  className={cn(
                    "grid min-w-[640px] cursor-pointer items-center px-5 py-[18px] transition-colors",
                    GRID_COLS,
                    isSelected ? "bg-primary/[0.06]" : "hover:bg-primary/[0.03]",
                    i !== sorted.length - 1 && "border-b border-border/30",
                  )}
                >
                  {/* Coin info */}
                  <div className="flex items-center gap-3">
                    <CoinIcon symbol={b.coinSymbol} size={36} />
                    <div className="flex flex-col leading-tight">
                      <span className="text-[13px] font-semibold tracking-wide">{b.coinSymbol}</span>
                      <span className="text-[11px] text-muted-foreground">{b.coinName}</span>
                    </div>
                  </div>

                  {/* Total amount */}
                  <div className="text-right">
                    <div className={cn("font-mono text-sm tabular-nums", hasBalance ? "font-semibold" : "text-muted-foreground/40")}>
                      {hasBalance ? formatDisplayQuantity(b.total, b.coinSymbol, baseCurrency) : "—"}
                    </div>
                    {!isBase && hasBalance && (
                      <div className="mt-0.5 font-mono text-[11px] tabular-nums text-muted-foreground">
                        ≈ {formatCurrencyCompact(b.totalValue, baseCurrency)}
                      </div>
                    )}
                  </div>

                  {/* Available */}
                  <div className={cn("text-right font-mono text-sm tabular-nums", !hasBalance && "text-muted-foreground/40")}>
                    {hasBalance ? formatDisplayQuantity(b.available, b.coinSymbol, baseCurrency) : "—"}
                  </div>

                  {/* Locked */}
                  <div className={cn(
                    "flex items-center justify-end gap-1 text-right font-mono text-sm tabular-nums",
                    b.locked > 0 ? "text-chart-4" : "text-muted-foreground/40",
                  )}>
                    {b.locked > 0 && <Lock className="h-3 w-3 shrink-0" />}
                    {b.locked > 0
                      ? formatDisplayQuantity(b.locked, b.coinSymbol, baseCurrency)
                      : "—"}
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
