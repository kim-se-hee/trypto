import { useState, useMemo, useCallback } from "react";
import { Search } from "lucide-react";
import { cn } from "@/lib/utils";
import { formatQuantity, formatCurrencyCompact, SMALL_AMOUNT_THRESHOLD } from "@/lib/formatters";
import { SortIcon } from "@/components/ui/SortIcon";
import { useSort } from "@/hooks/useSort";
import type { SortDir } from "@/hooks/useSort";
import { useVirtualList, virtualRowStyle } from "@/hooks/useVirtualList";
import { useIsMobile } from "@/hooks/useMediaQuery";
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

// 좁은 화면에서는 네 칸을 다 세울 폭이 없다. 사용가능·잠금은 접고, 행을 눌러 여는 상세에서 본다.
const GRID_COLS =
  "grid-cols-[minmax(0,1fr)_auto] sm:grid-cols-[1.6fr_minmax(120px,1.2fr)_minmax(100px,1fr)_minmax(90px,0.8fr)]";

// 가상화는 행 높이를 미리 알아야 스크롤 높이를 계산할 수 있다. 행은 높이를 고정한다.
// 보유수량 칸은 수량과 환산액 두 줄이 들어가므로 시세 목록보다 한 행이 높다.
const ROW_HEIGHT = 72;
const VISIBLE_ROWS = 8;
const LIST_HEIGHT = ROW_HEIGHT * VISIBLE_ROWS;
// 헤더 여백은 인라인 스타일로 준다(스크롤바 폭을 더해야 본문과 열이 맞는다). 본문의 px 클래스와 같은 값이어야 한다.
const LIST_PADDING_X_MOBILE = 12; // px-3
const LIST_PADDING_X = 20; // px-5

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

  const { scrollRef, virtualizer, scrollbarWidth } = useVirtualList({
    count: sorted.length,
    rowHeight: ROW_HEIGHT,
  });
  const isMobile = useIsMobile();

  const columns: { key: SortKey; label: string; mobileHidden?: boolean }[] = [
    { key: "name", label: "코인" },
    { key: "total", label: "보유수량" },
    { key: "available", label: "사용가능", mobileHidden: true },
    { key: "locked", label: "잠금", mobileHidden: true },
  ];

  const rowPaddingX = isMobile ? LIST_PADDING_X_MOBILE : LIST_PADDING_X;

  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border/30 px-3 py-3 sm:px-5 sm:py-4">
        <h3 className="text-base font-bold sm:text-lg">보유 자산</h3>
        <div className="flex flex-1 items-center justify-end gap-3">
          <div className="relative min-w-0 flex-1 sm:flex-none">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground/50" />
            <input
              type="text"
              placeholder="코인 검색"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              aria-label="코인 검색"
              className="h-9 w-full rounded-full border border-border/50 bg-secondary/30 pl-9 pr-3 text-sm outline-none transition-colors placeholder:text-muted-foreground/40 focus:border-primary/40 focus:bg-white sm:w-48"
            />
          </div>
          <label className="flex shrink-0 cursor-pointer items-center gap-1.5 whitespace-nowrap text-xs font-medium text-muted-foreground select-none">
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

      <div className="sm:overflow-x-auto">
        {/* Header */}
        <div
          className={cn("grid items-center gap-2 bg-secondary/30 py-3 sm:min-w-[640px] sm:gap-0 sm:py-3.5", GRID_COLS)}
          style={{ paddingLeft: rowPaddingX, paddingRight: rowPaddingX + scrollbarWidth }}
          role="row"
        >
          {columns.map((col) => (
            <button
              key={col.key}
              onClick={() => handleSort(col.key)}
              aria-sort={sortKey === col.key ? (sortDir === "asc" ? "ascending" : "descending") : "none"}
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

        {/* Body: 페이지가 아니라 이 상자 안에서 스크롤하고, 보이는 구간의 행만 그린다. */}
        {sorted.length === 0 ? (
          <div className="flex h-48 items-center justify-center text-sm text-muted-foreground">
            보유 중인 자산이 없습니다.
          </div>
        ) : (
          <div
            ref={scrollRef}
            className="overflow-y-auto [scrollbar-gutter:stable] sm:min-w-[640px]"
            style={{ height: Math.min(LIST_HEIGHT, sorted.length * ROW_HEIGHT) }}
          >
            <div className="relative w-full" style={{ height: virtualizer.getTotalSize() }}>
              {virtualizer.getVirtualItems().map((item) => {
                const b = sorted[item.index];
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
                    style={virtualRowStyle(item, ROW_HEIGHT)}
                    className={cn(
                      "grid cursor-pointer items-center gap-2 px-3 transition-colors sm:gap-0 sm:px-5",
                      GRID_COLS,
                      isSelected ? "bg-primary/[0.06]" : "hover:bg-primary/[0.03]",
                      item.index !== sorted.length - 1 && "border-b border-border/30",
                    )}
                  >
                    {/* Coin info */}
                    <div className="flex min-w-0 items-center gap-3">
                      <div className="flex min-w-0 flex-col leading-tight">
                        <span className="text-[13px] font-semibold tracking-wide">{b.coinSymbol}</span>
                        <span className="truncate text-[11px] text-muted-foreground">{b.coinName}</span>
                      </div>
                    </div>

                    {/* Total amount */}
                    <div className="min-w-0 text-right">
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
                    <div className={cn("hidden text-right font-mono text-sm tabular-nums sm:block", !hasBalance && "text-muted-foreground/40")}>
                      {hasBalance ? formatDisplayQuantity(b.available, b.coinSymbol, baseCurrency) : "—"}
                    </div>

                    {/* Locked */}
                    <div className={cn(
                      "hidden text-right font-mono text-sm tabular-nums sm:block",
                      b.locked > 0 ? "text-chart-4" : "text-muted-foreground/40",
                    )}>
                      {b.locked > 0
                        ? formatDisplayQuantity(b.locked, b.coinSymbol, baseCurrency)
                        : "—"}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
