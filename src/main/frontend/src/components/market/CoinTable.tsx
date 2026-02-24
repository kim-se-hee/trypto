import { useState, useMemo } from "react";
import { ArrowUpDown, ArrowUp, ArrowDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { CoinIcon } from "./CoinIcon";
import { Sparkline } from "./Sparkline";
import type { CoinData } from "@/mocks/coins";

interface CoinTableProps {
  coins: CoinData[];
  baseCurrency: string;
}

type SortKey = "name" | "price" | "change" | "volume" | "marketCap";
type SortDir = "asc" | "desc";

function formatPrice(price: number, baseCurrency: string): string {
  if (baseCurrency === "SOL") {
    if (price >= 1) return price.toLocaleString("en-US", { minimumFractionDigits: 4, maximumFractionDigits: 4 });
    if (price >= 0.0001) return price.toLocaleString("en-US", { minimumFractionDigits: 4, maximumFractionDigits: 8 });
    return price.toExponential(2);
  }
  if (baseCurrency === "USDT") {
    if (price >= 100) return price.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    if (price >= 1) return price.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 4 });
    return price.toLocaleString("en-US", { minimumFractionDigits: 4, maximumFractionDigits: 4 });
  }
  return price.toLocaleString("ko-KR");
}

function formatVolume(volume: number, baseCurrency: string): string {
  if (baseCurrency === "SOL") {
    if (volume >= 1_000_000_000) return `◎${(volume / 1_000_000_000).toFixed(1)}B`;
    if (volume >= 1_000_000) return `◎${(volume / 1_000_000).toFixed(1)}M`;
    if (volume >= 1_000) return `◎${(volume / 1_000).toFixed(0)}K`;
    return `◎${volume.toLocaleString("en-US")}`;
  }
  if (baseCurrency === "USDT") {
    if (volume >= 1_000_000_000) return `$${(volume / 1_000_000_000).toFixed(1)}B`;
    if (volume >= 1_000_000) return `$${(volume / 1_000_000).toFixed(0)}M`;
    return `$${volume.toLocaleString("en-US")}`;
  }
  if (volume >= 1_0000_0000_0000) return `${(volume / 1_0000_0000_0000).toFixed(1)}조`;
  if (volume >= 1_0000_0000) return `${Math.floor(volume / 1_0000_0000).toLocaleString("ko-KR")}억`;
  if (volume >= 1_0000) return `${Math.floor(volume / 1_0000).toLocaleString("ko-KR")}만`;
  return volume.toLocaleString("ko-KR");
}

function formatMarketCap(cap: number, baseCurrency: string): string {
  if (baseCurrency === "SOL") {
    if (cap >= 1_000_000_000_000) return `◎${(cap / 1_000_000_000_000).toFixed(1)}T`;
    if (cap >= 1_000_000_000) return `◎${(cap / 1_000_000_000).toFixed(1)}B`;
    if (cap >= 1_000_000) return `◎${(cap / 1_000_000).toFixed(0)}M`;
    return `◎${cap.toLocaleString("en-US")}`;
  }
  if (baseCurrency === "USDT") {
    if (cap >= 1_000_000_000_000) return `$${(cap / 1_000_000_000_000).toFixed(1)}T`;
    if (cap >= 1_000_000_000) return `$${(cap / 1_000_000_000).toFixed(1)}B`;
    if (cap >= 1_000_000) return `$${(cap / 1_000_000).toFixed(0)}M`;
    return `$${cap.toLocaleString("en-US")}`;
  }
  if (cap >= 1_0000_0000_0000_0000) return `${(cap / 1_0000_0000_0000).toFixed(0)}조`;
  if (cap >= 1_0000_0000_0000) return `${(cap / 1_0000_0000_0000).toFixed(1)}조`;
  if (cap >= 1_0000_0000) return `${Math.floor(cap / 1_0000_0000).toLocaleString("ko-KR")}억`;
  return cap.toLocaleString("ko-KR");
}

function formatChangeRate(rate: number): string {
  const sign = rate > 0 ? "+" : "";
  return `${sign}${rate.toFixed(2)}%`;
}

function getSortComparator(key: SortKey, dir: SortDir) {
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
}

function SortIcon({ column, activeColumn, direction }: { column: SortKey; activeColumn: SortKey | null; direction: SortDir }) {
  if (activeColumn !== column) return <ArrowUpDown className="h-3 w-3 opacity-30" />;
  return direction === "asc"
    ? <ArrowUp className="h-3 w-3 text-primary" />
    : <ArrowDown className="h-3 w-3 text-primary" />;
}

const GRID_COLS = "grid-cols-[2fr_minmax(100px,140px)_minmax(80px,100px)_80px_minmax(90px,120px)_minmax(90px,120px)]";

export function CoinTable({ coins, baseCurrency }: CoinTableProps) {
  const [sortKey, setSortKey] = useState<SortKey | null>(null);
  const [sortDir, setSortDir] = useState<SortDir>("desc");

  const sortedCoins = useMemo(() => {
    if (!sortKey) return coins;
    return [...coins].sort(getSortComparator(sortKey, sortDir));
  }, [coins, sortKey, sortDir]);

  const handleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDir((prev) => (prev === "desc" ? "asc" : "desc"));
    } else {
      setSortKey(key);
      setSortDir("desc");
    }
  };

  const currencySymbol = baseCurrency === "KRW" ? "₩" : baseCurrency === "SOL" ? "◎" : "";

  const columns: { key: SortKey | "sparkline"; label: string; sortable: boolean }[] = [
    { key: "name", label: "코인명", sortable: true },
    { key: "price", label: "현재가", sortable: true },
    { key: "change", label: "전일대비", sortable: true },
    { key: "sparkline", label: "7일", sortable: false },
    { key: "marketCap", label: "시가총액", sortable: true },
    { key: "volume", label: "거래대금(24H)", sortable: true },
  ];

  return (
    <div className="overflow-hidden rounded-2xl bg-card shadow-[0_2px_12px_rgba(40,13,95,0.06)]">
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
          sortedCoins.map((coin, i) => (
            <div
              key={coin.symbol}
              className={cn(
                "group grid cursor-pointer items-center px-5 py-[18px] transition-colors hover:bg-primary/[0.03]",
                GRID_COLS,
                i !== sortedCoins.length - 1 && "border-b border-border/30",
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
          ))
        )}
      </div>
    </div>
  );
}
