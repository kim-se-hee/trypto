import { cn } from "@/lib/utils";
import type { CoinData } from "@/lib/types/coins";

interface MarketOverviewCardsProps {
  coins: CoinData[];
  baseCurrency: string;
  highlightSymbols?: string[];
}

function formatCardPrice(price: number, baseCurrency: string): string {
  if (baseCurrency === "SOL") {
    if (price >= 1) return `${price.toLocaleString("en-US", { minimumFractionDigits: 4, maximumFractionDigits: 4 })} SOL`;
    return `${price.toLocaleString("en-US", { minimumFractionDigits: 4, maximumFractionDigits: 8 })} SOL`;
  }
  if (baseCurrency === "USDT") {
    return `$${price.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
  return `₩${price.toLocaleString("ko-KR")}`;
}

const DEFAULT_CEX_HIGHLIGHTS = ["BTC", "ETH", "SOL"];
const DEFAULT_DEX_HIGHLIGHTS = ["JUP", "BONK", "RAY"];

export function MarketOverviewCards({ coins, baseCurrency, highlightSymbols }: MarketOverviewCardsProps) {
  const symbols = highlightSymbols ?? (baseCurrency === "SOL" ? DEFAULT_DEX_HIGHLIGHTS : DEFAULT_CEX_HIGHLIGHTS);
  const highlighted = symbols
    .map((s) => coins.find((c) => c.symbol === s))
    .filter(Boolean) as CoinData[];

  if (highlighted.length === 0) return null;

  return (
    <div className="mb-5">
      <div className="mb-3 flex items-center gap-1.5">
        <span className="text-sm font-bold text-foreground">주요 코인</span>
        <span className="text-xs font-medium text-muted-foreground">&middot; 실시간</span>
      </div>
      {/* 좁은 화면에서 3등분하면 카드 하나가 시세 한 줄도 못 담는다. 옆으로 넘겨 보게 한다. */}
      <div className="no-scrollbar -mx-4 flex snap-x snap-mandatory gap-3 overflow-x-auto px-4 sm:mx-0 sm:grid sm:grid-cols-3 sm:gap-4 sm:px-0">
      {highlighted.map((coin) => {
        const isUp = coin.changeRate > 0;
        return (
          <div
            key={coin.symbol}
            className="w-[62%] shrink-0 snap-start rounded-xl border border-border bg-card p-4 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-card-hover sm:w-auto sm:p-5"
          >
            <div className="flex items-start justify-between gap-2">
              <div className="flex min-w-0 items-center gap-2.5">
                <div className="min-w-0">
                  <span className="text-sm font-semibold">{coin.symbol}</span>
                  <span className="ml-1.5 text-xs text-muted-foreground">{coin.name}</span>
                </div>
              </div>
              {coin.currentPrice > 0 ? (
                <span
                  className={cn(
                    "shrink-0 rounded-full px-2 py-0.5 text-xs font-medium tabular-nums",
                    isUp ? "bg-positive/15 text-positive" : "bg-negative/15 text-negative",
                    coin.changeRate === 0 && "bg-muted text-muted-foreground",
                  )}
                >
                  {isUp ? "+" : ""}{(coin.changeRate * 100).toFixed(2)}%
                </span>
              ) : (
                <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">-</span>
              )}
            </div>
            <div className="mt-3">
              <span className="block truncate font-mono text-base font-bold tabular-nums sm:text-lg">
                {coin.currentPrice > 0
                  ? formatCardPrice(coin.currentPrice, baseCurrency)
                  : <span className="text-muted-foreground">-</span>}
              </span>
            </div>
          </div>
        );
      })}
      </div>
    </div>
  );
}
