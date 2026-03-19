import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { cn } from "@/lib/utils";
import { formatCurrency } from "@/lib/formatters";
import type { WalletCoinBalance } from "@/lib/types/wallet";

interface WalletSummaryProps {
  balances: WalletCoinBalance[];
  baseCurrency: string;
  exchangeName: string;
}

const HIDDEN = "••••••••";

export function WalletSummary({ balances, baseCurrency, exchangeName }: WalletSummaryProps) {
  const [visible, setVisible] = useState(true);

  const totalAsset = balances.reduce(
    (sum, b) => sum + (b.available + b.locked) * b.currentPrice,
    0,
  );

  const baseCoin = balances.find((b) => b.coinSymbol === baseCurrency);
  const baseTotal = baseCoin ? baseCoin.available + baseCoin.locked : 0;
  const baseAvailable = baseCoin ? baseCoin.available : 0;
  const baseLocked = baseCoin ? baseCoin.locked : 0;

  return (
    <div className="rounded-xl border border-border bg-card p-6">
      <div>
        <div className="flex items-center gap-2">
          <p className="text-sm font-medium text-muted-foreground">
            {exchangeName} 총 자산
          </p>
          <button
            onClick={() => setVisible((v) => !v)}
            aria-label={visible ? "잔액 숨기기" : "잔액 보기"}
            className="text-muted-foreground/60 transition-colors hover:text-foreground"
          >
            {visible ? <Eye className="h-4 w-4" /> : <EyeOff className="h-4 w-4" />}
          </button>
        </div>

        <p className="mt-1 font-mono text-3xl font-bold tabular-nums tracking-tight">
          {visible ? formatCurrency(totalAsset, baseCurrency) : HIDDEN}
        </p>

        <div className="mt-1.5 flex items-center gap-3 text-sm text-muted-foreground">
          <span>
            <span className="text-xs">보유 {baseCurrency}</span>{" "}
            <span className={cn("font-mono font-semibold tabular-nums", !visible && "blur-sm select-none")}>
              {visible ? formatCurrency(baseTotal, baseCurrency) : HIDDEN}
            </span>
          </span>
          {baseLocked > 0 && visible && (
            <span className="text-xs text-muted-foreground/60">
              (사용 가능 {formatCurrency(baseAvailable, baseCurrency)} / 잠금 {formatCurrency(baseLocked, baseCurrency)})
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
