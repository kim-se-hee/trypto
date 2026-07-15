import { cn } from "@/lib/utils";
import { formatChangeRate, formatPrice } from "@/lib/formatters";
import { getCoinColor, type CoinData } from "@/lib/types/coins";

const MARQUEE_COUNT = 12;

function MarqueeItem({ coin }: { coin: CoinData }) {
  const isUp = coin.changeRate > 0;
  const isDown = coin.changeRate < 0;

  return (
    <span className="flex items-center gap-2 px-5">
      <span
        className="inline-block h-2 w-2 rounded-full"
        style={{ backgroundColor: getCoinColor(coin.symbol) }}
      />
      <span className="text-xs font-extrabold tracking-wide">{coin.symbol}</span>
      <span className="font-mono text-xs font-semibold tabular-nums text-muted-foreground">
        ₩{formatPrice(coin.currentPrice, "KRW")}
      </span>
      <span
        className={cn(
          "text-xs font-bold tabular-nums",
          isUp && "text-positive",
          isDown && "text-negative",
          !isUp && !isDown && "text-muted-foreground",
        )}
      >
        {formatChangeRate(coin.changeRate)}
      </span>
    </span>
  );
}

interface TickerMarqueeProps {
  coins: CoinData[];
}

/** 실시간 시세가 흐르는 띠. 데이터가 아직 없으면 아무것도 그리지 않는다. */
export function TickerMarquee({ coins }: TickerMarqueeProps) {
  if (coins.length === 0) return null;

  const top = [...coins].sort((a, b) => b.volume - a.volume).slice(0, MARQUEE_COUNT);

  return (
    <div className="marquee-mask overflow-hidden border-y border-border/60 bg-card/70 py-3">
      <div className="animate-marquee flex w-max">
        <div className="flex items-center">
          {top.map((coin) => (
            <MarqueeItem key={coin.symbol} coin={coin} />
          ))}
        </div>
        {/* 이음새 없는 순환을 위한 복제 — 스크린 리더에는 숨긴다 */}
        <div className="flex items-center" aria-hidden="true">
          {top.map((coin) => (
            <MarqueeItem key={`dup-${coin.symbol}`} coin={coin} />
          ))}
        </div>
      </div>
    </div>
  );
}
