import { cn } from "@/lib/utils";
import { formatCurrency } from "@/lib/formatters";
import type { HoldingData } from "@/lib/types/portfolio";

interface AssetSummaryCardProps {
  availableCash: number;
  holdings: HoldingData[];
  baseCurrency: string;
}

export function AssetSummaryCard({ availableCash, holdings, baseCurrency }: AssetSummaryCardProps) {
  const totalBuy = holdings.reduce((sum, h) => sum + h.avgBuyPrice * h.quantity, 0);
  const totalEval = holdings.reduce((sum, h) => sum + h.currentPrice * h.quantity, 0);
  const totalAsset = availableCash + totalEval;
  const profitLoss = totalEval - totalBuy;
  const profitRate = totalBuy > 0 ? (profitLoss / totalBuy) * 100 : 0;
  const isPositive = profitLoss > 0;
  const isNegative = profitLoss < 0;

  const items = [
    { label: "총매수", value: formatCurrency(totalBuy, baseCurrency) },
    { label: "총평가", value: formatCurrency(totalEval, baseCurrency) },
    {
      label: "평가손익",
      value: `${isPositive ? "+" : ""}${formatCurrency(profitLoss, baseCurrency)}`,
      color: isPositive ? "text-positive" : isNegative ? "text-negative" : undefined,
    },
    {
      label: "수익률",
      value: `${isPositive ? "+" : ""}${profitRate.toFixed(2)}%`,
      color: isPositive ? "text-positive" : isNegative ? "text-negative" : undefined,
    },
  ];

  return (
    <div className="rounded-xl border border-border bg-card p-5">
      <div className="mb-4">
        <p className="text-xs font-medium text-muted-foreground">
          보유 {baseCurrency}
        </p>
        <p className="mt-0.5 whitespace-nowrap font-mono text-sm font-semibold tabular-nums">
          {formatCurrency(availableCash, baseCurrency)}
        </p>
      </div>

      <div className="mb-5">
        <p className="text-xs font-medium text-muted-foreground">총 보유자산</p>
        <p className="mt-0.5 whitespace-nowrap font-mono text-2xl font-bold tabular-nums tracking-tight">
          {formatCurrency(totalAsset, baseCurrency)}
        </p>
        <p className="mt-0.5 text-[10px] text-muted-foreground">
          보유 {baseCurrency} + 코인 평가 합계
        </p>
      </div>

      <div className="grid grid-cols-2 gap-3">
        {items.map((item) => (
          <div key={item.label} className="rounded-xl bg-secondary/40 px-3 py-2.5">
            <p className="text-[11px] font-medium text-muted-foreground">{item.label}</p>
            <p className={cn(
              "mt-0.5 whitespace-nowrap font-mono text-sm font-semibold tabular-nums",
              item.color,
            )}>
              {item.value}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}
