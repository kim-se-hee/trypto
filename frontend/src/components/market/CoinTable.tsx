import { memo, type CSSProperties, type ReactNode } from "react";
import { cn } from "@/lib/utils";
import { formatPrice, formatVolume, formatChangeRate, getCurrencySymbol } from "@/lib/formatters";
import { SortIcon } from "@/components/ui/SortIcon";
import type { SortDir } from "@/hooks/useSort";
import { useVirtualList, virtualRowStyle } from "@/hooks/useVirtualList";
import { useIsMobile } from "@/hooks/useMediaQuery";
import { usePriceFlash } from "@/hooks/usePriceFlash";
import type { CoinData } from "@/lib/types/coins";

export type CoinSortKey = "name" | "price" | "change" | "volume";

interface CoinTableProps {
  /** 이미 정렬을 마친 목록. 차트의 기본 코인도 같은 정렬을 따라야 하므로 정렬은 페이지가 쥔다. */
  coins: CoinData[];
  baseCurrency: string;
  sortKey: CoinSortKey | null;
  sortDir: SortDir;
  onSort: (key: CoinSortKey) => void;
  selectedSymbol?: string | null;
  onSelect?: (symbol: string) => void;
  /** 목록 바로 위에 얹을 것 (검색창). 목록을 좁히는 도구는 목록과 같은 상자 안에 둔다. */
  toolbar?: ReactNode;
}

// 좁은 화면에서는 네 칸을 다 세울 폭이 없다. 거래대금을 접고 코인·현재가·전일대비만 남긴다.
const GRID_COLS =
  "grid-cols-[minmax(0,1fr)_auto_72px] sm:grid-cols-[2fr_minmax(100px,140px)_minmax(80px,100px)_minmax(160px,1fr)]";

// 가상화는 행 높이를 미리 알아야 스크롤 높이를 계산할 수 있다. 행은 높이를 고정한다.
const ROW_HEIGHT = 68;
const VISIBLE_ROWS = 8;
const LIST_HEIGHT = ROW_HEIGHT * VISIBLE_ROWS;
// 헤더 여백은 인라인 스타일로 준다(스크롤바 폭을 더해야 본문과 열이 맞는다). 본문의 px 클래스와 같은 값이어야 한다.
const LIST_PADDING_X_MOBILE = 12; // px-3
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
  const flash = usePriceFlash(coin.currentPrice, coin.tickedAt);

  return (
    <div
      onClick={handleClick}
      style={style}
      className={cn(
        "group grid cursor-pointer items-center gap-2 px-3 transition-colors hover:bg-primary/[0.03] sm:gap-0 sm:px-5",
        GRID_COLS,
        !isLast && "border-b border-border/30",
        isSelected && "bg-primary/[0.04]",
      )}
    >
      <div className="flex min-w-0 items-center gap-3">
        <div className="flex min-w-0 flex-col leading-tight">
          <span className="text-[13px] font-semibold tracking-wide">{coin.symbol}</span>
          <span className="truncate text-[11px] text-muted-foreground">{coin.name}</span>
        </div>
      </div>

      <div className={cn(
        "relative whitespace-nowrap text-right font-mono text-[13px] font-semibold tabular-nums sm:text-sm",
        coin.changeRate > 0 && "text-positive",
        coin.changeRate < 0 && "text-negative",
      )}>
        {coin.currentPrice > 0
          ? <>{currencySymbol}{formatPrice(coin.currentPrice, baseCurrency)}</>
          : <span className="text-muted-foreground">-</span>}
        {flash && (
          // 글자색이나 배경을 건드리면 정작 읽어야 할 숫자가 흔들린다. 테두리만 잠깐 두른다.
          <span
            aria-hidden
            className={cn(
              "pointer-events-none absolute left-0 -right-2 -inset-y-2.5 rounded-md border",
              flash === "up" && "border-positive",
              flash === "down" && "border-negative",
              flash === "same" && "border-muted-foreground/40",
            )}
          />
        )}
      </div>

      <div className="flex justify-end">
        {coin.currentPrice > 0 ? (
          <span
            className={cn(
              "inline-block whitespace-nowrap rounded-full px-1.5 py-0.5 font-mono text-[11px] font-medium tabular-nums sm:px-2 sm:text-xs",
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

      <div className="hidden text-right font-mono text-xs tabular-nums text-muted-foreground sm:block">
        {coin.volume > 0 ? formatVolume(coin.volume, baseCurrency) : "-"}
      </div>
    </div>
  );
});

export function CoinTable({
  coins,
  baseCurrency,
  sortKey,
  sortDir,
  onSort,
  selectedSymbol,
  onSelect,
  toolbar,
}: CoinTableProps) {
  const { scrollRef, virtualizer, scrollbarWidth } = useVirtualList({
    count: coins.length,
    rowHeight: ROW_HEIGHT,
  });
  const isMobile = useIsMobile();

  const currencySymbol = getCurrencySymbol(baseCurrency);

  const columns: { key: CoinSortKey; label: string; sortable: boolean; mobileHidden?: boolean }[] = [
    { key: "name", label: "코인명", sortable: true },
    { key: "price", label: "현재가", sortable: true },
    { key: "change", label: "전일대비", sortable: true },
    { key: "volume", label: "거래대금(24H)", sortable: true, mobileHidden: true },
  ];

  const rowPaddingX = isMobile ? LIST_PADDING_X_MOBILE : LIST_PADDING_X;

  return (
    <div className="overflow-hidden rounded-xl border border-border bg-card">
      {toolbar && <div className="border-b border-border/40 px-3 py-3 sm:px-5">{toolbar}</div>}

      <div
        className={cn("grid items-center gap-2 bg-secondary/30 py-3 sm:gap-0 sm:py-3.5", GRID_COLS)}
        style={{
          paddingLeft: rowPaddingX,
          paddingRight: rowPaddingX + scrollbarWidth,
        }}
      >
        {columns.map((col) => (
          <button
            key={col.key}
            onClick={() => col.sortable && onSort(col.key)}
            disabled={!col.sortable}
            className={cn(
              "flex items-center gap-1 whitespace-nowrap text-[11px] font-medium text-muted-foreground transition-colors sm:text-xs",
              col.sortable && "hover:text-foreground",
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

      {coins.length === 0 ? (
        <div className="flex h-48 items-center justify-center text-sm text-muted-foreground">
          검색 결과가 없습니다.
        </div>
      ) : (
        // 페이지가 아니라 이 상자 안에서 스크롤한다. 스크롤바 자리는 항상 비워 두어야
        // 목록 길이가 바뀌어도 헤더와 본문의 열이 어긋나지 않는다.
        <div
          ref={scrollRef}
          className="overflow-y-auto [scrollbar-gutter:stable]"
          style={{ height: Math.min(LIST_HEIGHT, coins.length * ROW_HEIGHT) }}
        >
          <div className="relative w-full" style={{ height: virtualizer.getTotalSize() }}>
            {virtualizer.getVirtualItems().map((item) => {
              const coin = coins[item.index];
              return (
                <CoinRow
                  key={coin.symbol}
                  coin={coin}
                  baseCurrency={baseCurrency}
                  currencySymbol={currencySymbol}
                  isSelected={selectedSymbol === coin.symbol}
                  isLast={item.index === coins.length - 1}
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
