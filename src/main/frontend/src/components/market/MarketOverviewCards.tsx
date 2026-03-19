import { Star } from "lucide-react";
import { cn } from "@/lib/utils";
import { CoinIcon } from "./CoinIcon";
import { Sparkline } from "./Sparkline";
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

function getCoinGradient(symbol: string): string {
  const gradients: Record<string, string> = {
    BTC: "from-orange-500/8 to-transparent",
    ETH: "from-blue-500/8 to-transparent",
    SOL: "from-violet-500/8 to-transparent",
    JUP: "from-emerald-500/8 to-transparent",
    BONK: "from-amber-500/8 to-transparent",
    RAY: "from-cyan-500/8 to-transparent",
  };
  return gradients[symbol] ?? "from-primary/5 to-transparent";
}

export function MarketOverviewCards({ coins, baseCurrency, highlightSymbols }: MarketOverviewCardsProps) {
  const symbols = highlightSymbols ?? (baseCurrency === "SOL" ? DEFAULT_DEX_HIGHLIGHTS : DEFAULT_CEX_HIGHLIGHTS);
  const highlighted = symbols
    .map((s) => coins.find((c) => c.symbol === s))
    .filter(Boolean) as CoinData[];

  if (highlighted.length === 0) return null;

  return (
    <div className="mb-5">
      <div className="mb-3 flex items-center gap-1.5">
        <Star className="h-4 w-4 fill-chart-4 text-chart-4" />
        <span className="text-sm font-bold text-foreground">주요 코인</span>
        <span className="text-xs font-medium text-muted-foreground">&middot; 시가총액 기준</span>
      </div>
      <div className="grid grid-cols-3 gap-4">
      {highlighted.map((coin) => {
        const isUp = coin.changeRate > 0;
        return (
          <div
            key={coin.symbol}
            className="group relative overflow-hidden rounded-xl border border-border bg-card p-5 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-card-hover"
          >
            {/* Gradient overlay */}
            <div className={cn("pointer-events-none absolute inset-0 bg-gradient-to-br opacity-0 transition-opacity duration-200 group-hover:opacity-100", getCoinGradient(coin.symbol))} />

            <div className="relative flex items-start justify-between">
              <div className="flex items-center gap-2.5">
                <CoinIcon symbol={coin.symbol} size={32} />
                <div>
                  <span className="text-sm font-semibold">{coin.symbol}</span>
                  <span className="ml-1.5 text-xs text-muted-foreground">{coin.name}</span>
                </div>
              </div>
              <span
                className={cn(
                  "rounded-full px-2 py-0.5 text-xs font-medium tabular-nums",
                  isUp ? "bg-positive/15 text-positive" : "bg-negative/15 text-negative",
                  coin.changeRate === 0 && "bg-muted text-muted-foreground",
                )}
              >
                {isUp ? "+" : ""}{coin.changeRate.toFixed(2)}%
              </span>
            </div>
            <div className="relative mt-3 flex items-end justify-between">
              <span className="font-mono text-lg font-bold tabular-nums">
                {formatCardPrice(coin.currentPrice, baseCurrency)}
              </span>
              <Sparkline data={coin.sparkline} width={72} height={28} />
            </div>
          </div>
        );
      })}
      </div>
    </div>
  );
}
