import { useCallback } from "react";
import { cn } from "@/lib/utils";
import { formatPrice, formatVolume, formatMarketCap, formatChangeRate, getCurrencySymbol } from "@/lib/formatters";
import { SortIcon } from "@/components/ui/SortIcon";
import { useSort } from "@/hooks/useSort";
import type { SortDir } from "@/hooks/useSort";
import { CoinIcon } from "./CoinIcon";
import { Sparkline } from "./Sparkline";
import type { CoinData } from "@/lib/types/coins";

interface CoinTableProps {
  coins: CoinData[];
  baseCurrency: string;
  selectedSymbol?: string | null;
  onSelect?: (symbol: string) => void;
}

type SortKey = "name" | "price" | "change" | "volume" | "marketCap";

const GRID_COLS = "grid-cols-[2fr_minmax(100px,140px)_minmax(80px,100px)_80px_minmax(90px,120px)_minmax(90px,120px)]";

export function CoinTable({ coins, baseCurrency, selectedSymbol, onSelect }: CoinTableProps) {
  const comparator = useCallback((key: SortKey, dir: SortDir) => {
    return (a: CoinData, b: CoinData) => {
      let cmp = 0;
      switch (key) {
        case "name": cmp = a.symbol.localeCompare(b.symbol); break;
        case "price": cmp = a.currentPrice - b.currentPrice; break;
        case "change": cmp = a.changeRate - b.changeRate; break;
        case "volume": cmp = a.volume - b.volume; break;
        case "marketCap": cmp = a.marketCap - b.marketCap; break;
      }
      return dir === "asc" ? cmp : -cmp;
    };
  }, []);

  const { sorted: sortedCoins, sortKey, sortDir, handleSort } = useSort<CoinData, SortKey>({
    items: coins,
    comparator,
  });

  const currencySymbol = getCurrencySymbol(baseCurrency);

  const columns: { key: SortKey | "sparkline"; label: string; sortable: boolean }[] = [
    { key: "name", label: "코인명", sortable: true },
    { key: "price", label: "현재가", sortable: true },
    { key: "change", label: "전일대비", sortable: true },
    { key: "sparkline", label: "7일", sortable: false },
    { key: "marketCap", label: "시가총액", sortable: true },
    { key: "volume", label: "거래대금(24H)", sortable: true },
  ];

  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card">
      {/* Table header */}
      <div className={cn("grid items-center bg-secondary/30 px-5 py-3.5", GRID_COLS)}>
        {columns.map((col) => (
          <button
            key={col.key}
            onClick={() => col.sortable && handleSort(col.key as SortKey)}
            disabled={!col.sortable}
            className={cn(
              "flex items-center gap-1 text-xs font-medium text-muted-foreground transition-colors",
              col.sortable && "hover:text-foreground",
              !col.sortable && "cursor-default",
              col.key !== "name" && "justify-end",
            )}
          >
            {col.key !== "name" && col.sortable && <SortIcon column={col.key as SortKey} activeColumn={sortKey} direction={sortDir} />}
            {col.label}
            {col.key === "name" && <SortIcon column="name" activeColumn={sortKey} direction={sortDir} />}
          </button>
        ))}
      </div>

      {/* Table body */}
      <div>
        {sortedCoins.length === 0 ? (
          <div className="flex h-48 items-center justify-center text-sm text-muted-foreground">
            검색 결과가 없습니다.
          </div>
        ) : (
          sortedCoins.map((coin, i) => {
            const isSelected = selectedSymbol === coin.symbol;
            return (
            <div
              key={coin.symbol}
              onClick={() => onSelect?.(coin.symbol)}
              className={cn(
                "group grid cursor-pointer items-center px-5 py-[18px] transition-colors hover:bg-primary/[0.03]",
                GRID_COLS,
                i !== sortedCoins.length - 1 && "border-b border-border/30",
                isSelected && "bg-primary/[0.04]",
              )}
            >
              {/* Coin info */}
              <div className="flex items-center gap-3">
                <CoinIcon symbol={coin.symbol} size={32} />
                <div className="flex flex-col leading-tight">
                  <span className="text-[13px] font-semibold tracking-wide">{coin.symbol}</span>
                  <span className="text-[11px] text-muted-foreground">{coin.name}</span>
                </div>
              </div>

              {/* Price */}
              <div className={cn(
                "text-right font-mono text-sm font-semibold tabular-nums",
                coin.changeRate > 0 && "text-positive",
                coin.changeRate < 0 && "text-negative",
              )}>
                {currencySymbol}{formatPrice(coin.currentPrice, baseCurrency)}
              </div>

              {/* Change rate */}
              <div className="flex justify-end">
                <span
                  className={cn(
                    "inline-block rounded-full px-2 py-0.5 font-mono text-xs font-medium tabular-nums",
                    coin.changeRate > 0 && "bg-positive/15 text-positive",
                    coin.changeRate < 0 && "bg-negative/15 text-negative",
                    coin.changeRate === 0 && "text-muted-foreground",
                  )}
                >
                  {formatChangeRate(coin.changeRate)}
                </span>
              </div>

              {/* Sparkline */}
              <div className="flex justify-end">
                <Sparkline data={coin.sparkline} width={64} height={24} />
              </div>

              {/* Market cap */}
              <div className="text-right font-mono text-xs tabular-nums text-muted-foreground">
                {formatMarketCap(coin.marketCap, baseCurrency)}
              </div>

              {/* Volume */}
              <div className="text-right font-mono text-xs tabular-nums text-muted-foreground">
                {formatVolume(coin.volume, baseCurrency)}
              </div>
            </div>
          )})
        )}
      </div>
    </div>
  );
}
