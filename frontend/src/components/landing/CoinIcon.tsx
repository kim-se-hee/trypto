import { useState } from "react";
import { cn } from "@/lib/utils";
import { getCoinColor } from "@/lib/types/coins";

// 폰트와 마찬가지로 CDN에서 로고를 가져온다. 없는 심볼이면 브랜드 색 원으로 대체한다(이니셜 없음).
const ICON_BASE = "https://cdn.jsdelivr.net/npm/cryptocurrency-icons@0.18.1/svg/color";

interface CoinIconProps {
  symbol: string;
  size?: number;
  className?: string;
}

export function CoinIcon({ symbol, size = 28, className }: CoinIconProps) {
  const [failed, setFailed] = useState(false);
  const dimension = { width: size, height: size };

  if (failed) {
    return (
      <span
        aria-hidden="true"
        className={cn("inline-block shrink-0 rounded-full", className)}
        style={{ ...dimension, backgroundColor: getCoinColor(symbol) }}
      />
    );
  }

  return (
    <img
      src={`${ICON_BASE}/${symbol.toLowerCase()}.svg`}
      onError={() => setFailed(true)}
      style={dimension}
      className={cn("shrink-0 rounded-full", className)}
      loading="lazy"
      alt=""
    />
  );
}
