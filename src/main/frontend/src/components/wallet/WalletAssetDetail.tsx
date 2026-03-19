import { X, ArrowDownToLine, ArrowUpFromLine } from "lucide-react";
import { cn } from "@/lib/utils";
import { CoinIcon } from "@/components/market/CoinIcon";
import { formatQuantity, formatFiatEstimate } from "@/lib/formatters";
import type { WalletCoinBalance } from "@/lib/types/wallet";

interface WalletAssetDetailProps {
  coin: WalletCoinBalance;
  baseCurrency: string;
  onClose: () => void;
  onDeposit?: (coin: WalletCoinBalance) => void;
  onTransfer?: (coin: WalletCoinBalance) => void;
}

function formatDisplay(quantity: number, coinSymbol: string, baseCurrency: string): string {
  if (coinSymbol === baseCurrency) return quantity.toLocaleString("ko-KR");
  return formatQuantity(quantity);
}

export function WalletAssetDetail({ coin, baseCurrency, onClose, onDeposit, onTransfer }: WalletAssetDetailProps) {
  const total = coin.available + coin.locked;
  const totalValue = total * coin.currentPrice;
  const isBase = coin.coinSymbol === baseCurrency;

  return (
    <div className="flex h-full flex-col rounded-xl border border-border bg-card">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-border/30 px-5 py-4">
        <div className="flex items-center gap-3">
          <CoinIcon symbol={coin.coinSymbol} size={36} />
          <div>
            <p className="text-sm font-bold">{coin.coinSymbol}</p>
            <p className="text-xs text-muted-foreground">{coin.coinName}</p>
          </div>
        </div>
        <button
          onClick={onClose}
          aria-label="닫기"
          className="rounded-lg p-1.5 text-muted-foreground transition-colors hover:bg-secondary/60 hover:text-foreground"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Balance */}
      <div className="border-b border-border/30 px-5 py-5">
        <p className="font-mono text-2xl font-bold tabular-nums tracking-tight">
          {formatDisplay(total, coin.coinSymbol, baseCurrency)}
        </p>
        {!isBase && (
          <p className="mt-0.5 text-sm text-muted-foreground">
            {formatFiatEstimate(totalValue, baseCurrency)}
          </p>
        )}

        {/* Action buttons — 입금/출금 for coins, 입금 for base currency */}
        <div className="mt-4 flex gap-2">
          {isBase ? (
            <button
              onClick={() => onDeposit?.(coin)}
              className="flex flex-1 items-center justify-center gap-1.5 rounded-xl bg-primary/10 px-3 py-2.5 text-sm font-semibold text-primary transition-all hover:bg-primary/20 active:scale-[0.97]"
            >
              <ArrowDownToLine className="h-4 w-4" />
              입금
            </button>
          ) : (
            <>
              <button
                onClick={() => onDeposit?.(coin)}
                className="flex flex-1 items-center justify-center gap-1.5 rounded-xl bg-primary/10 px-3 py-2.5 text-sm font-semibold text-primary transition-all hover:bg-primary/20 active:scale-[0.97]"
              >
                <ArrowDownToLine className="h-4 w-4" />
                입금
              </button>
              <button
                onClick={() => onTransfer?.(coin)}
                className="flex flex-1 items-center justify-center gap-1.5 rounded-xl bg-primary/10 px-3 py-2.5 text-sm font-semibold text-primary transition-all hover:bg-primary/20 active:scale-[0.97]"
              >
                <ArrowUpFromLine className="h-4 w-4" />
                출금
              </button>
            </>
          )}
        </div>
      </div>

      {/* Balance breakdown */}
      <div className="px-5 py-4">
        <p className="mb-3 text-sm font-bold">잔고 상세</p>
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">사용 가능</span>
            <span className="font-mono text-sm font-semibold tabular-nums">
              {formatDisplay(coin.available, coin.coinSymbol, baseCurrency)}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1.5">
              <span className="text-sm text-muted-foreground">잠금</span>
              {coin.locked > 0 && (
                <span className="inline-flex items-center rounded-md bg-chart-4/15 px-1.5 py-0.5 text-[10px] font-medium text-chart-4">
                  주문 대기
                </span>
              )}
            </div>
            <span className={cn(
              "font-mono text-sm font-semibold tabular-nums",
              coin.locked > 0 ? "text-chart-4" : "text-muted-foreground/40",
            )}>
              {coin.locked > 0
                ? formatDisplay(coin.locked, coin.coinSymbol, baseCurrency)
                : "—"}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
