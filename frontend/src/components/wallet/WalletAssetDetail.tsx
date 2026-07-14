import { X, ArrowUpFromLine } from "lucide-react";
import { cn } from "@/lib/utils";
import { formatQuantity, formatFiatEstimate } from "@/lib/formatters";
import type { WalletCoinBalance } from "@/lib/types/wallet";

interface WalletAssetDetailProps {
  coin: WalletCoinBalance;
  baseCurrency: string;
  onClose: () => void;
  onTransfer?: (coin: WalletCoinBalance) => void;
  // 다른 거래소 어디에도 상장되지 않은 코인은 보낼 곳이 없다.
  canTransfer?: boolean;
}

function formatDisplay(quantity: number, coinSymbol: string, baseCurrency: string): string {
  if (coinSymbol === baseCurrency) return quantity.toLocaleString("ko-KR");
  return formatQuantity(quantity);
}

export function WalletAssetDetail({
  coin,
  baseCurrency,
  onClose,
  onTransfer,
  canTransfer = true,
}: WalletAssetDetailProps) {
  const total = coin.available + coin.locked;
  const totalValue = total * coin.currentPrice;
  const isBase = coin.coinSymbol === baseCurrency;

  return (
    <div className="flex h-full flex-col rounded-xl border border-border bg-card">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-border/30 px-5 py-4">
        <div className="flex items-center gap-3">
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

        {/* Action buttons — 출금 for coins only */}
        {!isBase && (
          <div className="mt-4 flex flex-col gap-2">
            <button
              onClick={() => onTransfer?.(coin)}
              disabled={!canTransfer}
              className={cn(
                "flex flex-1 items-center justify-center gap-1.5 rounded-xl px-3 py-2.5 text-sm font-semibold transition-all",
                canTransfer
                  ? "bg-primary/10 text-primary hover:bg-primary/20 active:scale-[0.97]"
                  : "cursor-not-allowed bg-muted text-muted-foreground",
              )}
            >
              <ArrowUpFromLine className="h-4 w-4" />
              출금
            </button>
            {!canTransfer && (
              <p className="text-center text-xs text-muted-foreground">
                다른 거래소에 상장되지 않아 출금할 수 없습니다.
              </p>
            )}
          </div>
        )}
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
