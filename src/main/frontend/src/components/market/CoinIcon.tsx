import { getCoinColor } from "@/lib/types/coins";

interface CoinIconProps {
  symbol: string;
  size?: number;
}

export function CoinIcon({ symbol, size = 32 }: CoinIconProps) {
  const color = getCoinColor(symbol);
  const label = symbol.length >= 2 ? symbol.slice(0, 2) : symbol;

  return (
    <div
      className="flex shrink-0 items-center justify-center rounded-full font-bold text-white shadow-sm"
      style={{
        width: size,
        height: size,
        fontSize: size * 0.34,
        letterSpacing: "-0.02em",
        background: `linear-gradient(145deg, ${color}, ${color}cc)`,
        boxShadow: `0 3px 10px ${color}30`,
        border: "2px solid white",
      }}
    >
      {label}
    </div>
  );
}
