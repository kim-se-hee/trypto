import { memo, useCallback, type CSSProperties } from "react";
import { cn } from "@/lib/utils";
import { formatPrice, formatVolume, formatChangeRate, getCurrencySymbol } from "@/lib/formatters";
import { SortIcon } from "@/components/ui/SortIcon";
import { useSort } from "@/hooks/useSort";
import type { SortDir } from "@/hooks/useSort";
import { useVirtualList, virtualRowStyle } from "@/hooks/useVirtualList";
import { CoinIcon } from "./CoinIcon";
import type { CoinData } from "@/lib/types/coins";

interface CoinTableProps {
  coins: CoinData[];
  baseCurrency: string;
  selectedSymbol?: string | null;
  onSelect?: (symbol: string) => void;
}

type SortKey = "name" | "price" | "change" | "volume";

const GRID_COLS = "grid-cols-[2fr_minmax(100px,140px)_minmax(80px,100px)_minmax(160px,1fr)]";

// 가상화는 행 높이를 미리 알아야 스크롤 높이를 계산할 수 있다. 행은 높이를 고정한다.
const ROW_HEIGHT = 68;
const VISIBLE_ROWS = 8;
const LIST_HEIGHT = ROW_HEIGHT * VISIBLE_ROWS;
const LIST_PADDING_X = 20; // px-5

interface CoinRowProps {
  coin: CoinData;
  baseCurrency: string;
  currencySymbol: string;
  isSelected: boolean;
  isLast: boolean;
  style: CSSProperties;
  onSelect?: (symbol: string) => void;
}

const CoinRow = memo(function CoinRow({
  coin,
  baseCurrency,
  currencySymbol,
  isSelected,
  isLast,
  style,
  onSelect,
}: CoinRowProps) {
  const handleClick = () => onSelect?.(coin.symbol);

  return (
    <div
      onClick={handleClick}
      style={style}
      className={cn(
        "group grid cursor-pointer items-center px-5 transition-colors hover:bg-primary/[0.03]",
        GRID_COLS,
        !isLast && "border-b border-border/30",
        isSelected && "bg-primary/[0.04]",
      )}
    >
      <div className="flex items-center gap-3">
        <CoinIcon symbol={coin.symbol} size={32} />
        <div className="flex flex-col leading-tight">
          <span className="text-[13px] font-semibold tracking-wide">{coin.symbol}</span>
          <span className="text-[11px] text-muted-foreground">{coin.name}</span>
        </div>
      </div>

      <div className={cn(
        "text-right font-mono text-sm font-semibold tabular-nums",
        coin.changeRate > 0 && "text-positive",
        coin.changeRate < 0 && "text-negative",
      )}>
        {coin.currentPrice > 0
          ? <>{currencySymbol}{formatPrice(coin.currentPrice, baseCurrency)}</>
          : <span className="text-muted-foreground">-</span>}
      </div>

      <div className="flex justify-end">
        {coin.currentPrice > 0 ? (
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
        ) : (
          <span className="text-xs text-muted-foreground">-</span>
        )}
      </div>

      <div className="text-right font-mono text-xs tabular-nums text-muted-foreground">
        {coin.volume > 0 ? formatVolume(coin.volume, baseCurrency) : "-"}
      </div>
    </div>
  );
});

export function CoinTable({ coins, baseCurrency, selectedSymbol, onSelect }: CoinTableProps) {
  const comparator = useCallback((key: SortKey, dir: SortDir) => {
    return (a: CoinData, b: CoinData) => {
      let cmp = 0;
      switch (key) {
        case "name": cmp = a.symbol.localeCompare(b.symbol); break;
        case "price": cmp = a.currentPrice - b.currentPrice; break;
        case "change": cmp = a.changeRate - b.changeRate; break;
        case "volume": cmp = a.volume - b.volume; break;
      }
      return dir === "asc" ? cmp : -cmp;
    };
  }, []);

  const { sorted: sortedCoins, sortKey, sortDir, handleSort } = useSort<CoinData, SortKey>({
    items: coins,
    comparator,
  });

  const { scrollRef, virtualizer, scrollbarWidth } = useVirtualList({
    count: sortedCoins.length,
    rowHeight: ROW_HEIGHT,
  });

  const currencySymbol = getCurrencySymbol(baseCurrency);

  const columns: { key: SortKey; label: string; sortable: boolean }[] = [
    { key: "name", label: "코인명", sortable: true },
    { key: "price", label: "현재가", sortable: true },
    { key: "change", label: "전일대비", sortable: true },
    { key: "volume", label: "거래대금(24H)", sortable: true },
  ];

  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card">
      <div
        className={cn("grid items-center bg-secondary/30 py-3.5", GRID_COLS)}
        style={{
          paddingLeft: LIST_PADDING_X,
          paddingRight: LIST_PADDING_X + scrollbarWidth,
        }}
      >
        {columns.map((col) => (
          <button
            key={col.key}
            onClick={() => col.sortable && handleSort(col.key)}
            disabled={!col.sortable}
            className={cn(
              "flex items-center gap-1 text-xs font-medium text-muted-foreground transition-colors",
              col.sortable && "hover:text-foreground",
              col.key !== "name" && "justify-end",
            )}
          >
            {col.key !== "name" && <SortIcon column={col.key} activeColumn={sortKey} direction={sortDir} />}
            {col.label}
            {col.key === "name" && <SortIcon column="name" activeColumn={sortKey} direction={sortDir} />}
          </button>
        ))}
      </div>

      {sortedCoins.length === 0 ? (
        <div className="flex h-48 items-center justify-center text-sm text-muted-foreground">
          검색 결과가 없습니다.
        </div>
      ) : (
        // 페이지가 아니라 이 상자 안에서 스크롤한다. 스크롤바 자리는 항상 비워 두어야
        // 목록 길이가 바뀌어도 헤더와 본문의 열이 어긋나지 않는다.
        <div
          ref={scrollRef}
          className="overflow-y-auto [scrollbar-gutter:stable]"
          style={{ height: Math.min(LIST_HEIGHT, sortedCoins.length * ROW_HEIGHT) }}
        >
          <div className="relative w-full" style={{ height: virtualizer.getTotalSize() }}>
            {virtualizer.getVirtualItems().map((item) => {
              const coin = sortedCoins[item.index];
              return (
                <CoinRow
                  key={coin.symbol}
                  coin={coin}
                  baseCurrency={baseCurrency}
                  currencySymbol={currencySymbol}
                  isSelected={selectedSymbol === coin.symbol}
                  isLast={item.index === sortedCoins.length - 1}
                  style={virtualRowStyle(item, ROW_HEIGHT)}
                  onSelect={onSelect}
                />
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
