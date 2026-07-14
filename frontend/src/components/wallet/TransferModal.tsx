import { useState, useMemo } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { formatQuantity } from "@/lib/formatters";
import { createTransfer } from "@/lib/api/transfer-api";
import { isApiClientError } from "@/lib/api/types";
import type { WalletCoinBalance } from "@/lib/types/wallet";

export interface TransferDestination {
  walletId: number;
  exchangeId: string;
  exchangeName: string;
  // 해당 거래소가 이 코인을 취급하는지. 취급하지 않으면 선택할 수 없다.
  listed: boolean;
}

interface TransferModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
  coin: WalletCoinBalance;
  baseCurrency: string;
  fromWalletId: number;
  destinations: TransferDestination[];
}

export function TransferModal({
  isOpen,
  onClose,
  onSuccess,
  coin,
  baseCurrency,
  fromWalletId,
  destinations,
}: TransferModalProps) {
  const [selectedDestination, setSelectedDestination] = useState("");
  const [amountStr, setAmountStr] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const amount = parseFloat(amountStr) || 0;

  const errors = useMemo(() => {
    if (!submitted) return {};
    const e: Record<string, string> = {};
    if (!selectedDestination) e.destination = "도착 거래소를 선택해주세요.";
    if (amount <= 0) e.amount = "수량을 입력해주세요.";
    else if (amount > coin.available) e.amount = "가용 잔고를 초과합니다.";
    return e;
  }, [submitted, selectedDestination, amount, coin.available]);

  const hasReachableDestination = destinations.some((d) => d.listed);

  function handleMaxClick() {
    setAmountStr(coin.available.toString());
  }

  async function handleSubmit() {
    setSubmitted(true);
    setError(null);
    if (submitting) return;
    if (!selectedDestination || amount <= 0 || amount > coin.available) return;
    if (!coin.coinId) return;

    const dest = destinations.find((d) => d.exchangeId === selectedDestination);
    if (!dest || !dest.listed) return;

    setSubmitting(true);
    try {
      await createTransfer({
        idempotencyKey: crypto.randomUUID(),
        fromWalletId,
        toWalletId: dest.walletId,
        coinId: coin.coinId,
        amount,
      });
      onSuccess?.();
      onClose();
    } catch (e) {
      if (isApiClientError(e) && e.code === "COIN_NOT_LISTED_ON_EXCHANGE") {
        setError(`${dest.exchangeName}에는 ${coin.coinSymbol}이(가) 상장되어 있지 않습니다.`);
      } else {
        setError("송금에 실패했습니다.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  function handleOpenChange(open: boolean) {
    if (!open) {
      onClose();
      setSelectedDestination("");
      setAmountStr("");
      setSubmitted(false);
    }
  }

  function formatDisplay(qty: number): string {
    if (coin.coinSymbol === baseCurrency) return qty.toLocaleString("ko-KR");
    return formatQuantity(qty);
  }

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent showCloseButton className="max-w-md gap-0 p-0">
        {/* Header */}
        <DialogHeader className="border-b border-border/30 px-6 py-5">
          <div className="flex items-center gap-3">
            <div>
              <DialogTitle>{coin.coinSymbol} 출금</DialogTitle>
              <DialogDescription className="mt-0.5">
                {coin.coinName}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-5 px-6 py-5">
          {/* Destination Exchange */}
          <div className="space-y-2">
            <label className="text-sm font-medium">도착 거래소</label>
            <Select value={selectedDestination} onValueChange={setSelectedDestination}>
              <SelectTrigger className="w-full">
                <SelectValue placeholder="거래소 선택" />
              </SelectTrigger>
              <SelectContent>
                {destinations.map((dest) => (
                  <SelectItem key={dest.exchangeId} value={dest.exchangeId} disabled={!dest.listed}>
                    {dest.exchangeName}
                    {!dest.listed && (
                      <span className="ml-2 text-xs text-muted-foreground">
                        {coin.coinSymbol} 미상장
                      </span>
                    )}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {errors.destination && (
              <p className="text-xs text-destructive">{errors.destination}</p>
            )}
          </div>

          {/* Amount */}
          <div className="space-y-2">
            <label className="text-sm font-medium">출금 수량</label>
            <div className="flex gap-2">
              <input
                type="number"
                value={amountStr}
                onChange={(e) => setAmountStr(e.target.value)}
                placeholder="0"
                min={0}
                step="any"
                className="h-9 flex-1 rounded-md border border-input bg-transparent px-3 font-mono text-sm outline-none transition-colors placeholder:text-muted-foreground/40 focus:border-primary/40 focus:ring-1 focus:ring-ring/50 [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none"
              />
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleMaxClick}
                className="shrink-0"
              >
                최대
              </Button>
            </div>
            <p className="text-xs text-muted-foreground">
              가용:{" "}
              <span className="font-mono font-medium tabular-nums">
                {formatDisplay(coin.available)}
              </span>{" "}
              {coin.coinSymbol}
            </p>
            {errors.amount && (
              <p className="text-xs text-destructive">{errors.amount}</p>
            )}
          </div>

          {!hasReachableDestination && (
            <p className="text-center text-sm text-muted-foreground">
              {coin.coinSymbol}을(를) 취급하는 다른 거래소가 없어 출금할 수 없습니다.
            </p>
          )}

          {error && (
            <p className="text-center text-sm text-destructive">{error}</p>
          )}

          {/* Submit */}
          <Button
            className="w-full"
            onClick={handleSubmit}
            disabled={submitting || !hasReachableDestination}
          >
            {submitting ? "출금 중..." : "출금하기"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
