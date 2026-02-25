import { useState, useMemo } from "react";
import { AlertTriangle, ClipboardPaste } from "lucide-react";
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
import { Separator } from "@/components/ui/separator";
import { Button } from "@/components/ui/button";
import { CoinIcon } from "@/components/market/CoinIcon";
import { formatQuantity } from "@/lib/formatters";
import {
  coinChains,
  tagRequiredCoins,
  withdrawalFees,
} from "@/mocks/wallet";
import type { WalletCoinBalance } from "@/mocks/wallet";

interface TransferModalProps {
  isOpen: boolean;
  onClose: () => void;
  coin: WalletCoinBalance;
  exchangeId: string;
  baseCurrency: string;
}

export function TransferModal({
  isOpen,
  onClose,
  coin,
  exchangeId,
  baseCurrency,
}: TransferModalProps) {
  const [network, setNetwork] = useState("");
  const [address, setAddress] = useState("");
  const [tag, setTag] = useState("");
  const [amountStr, setAmountStr] = useState("");
  const [submitted, setSubmitted] = useState(false);

  const networks = coinChains[coin.coinSymbol] ?? [];
  const needsTag = tagRequiredCoins.includes(coin.coinSymbol);

  const fee = useMemo(() => {
    if (!network) return 0;
    return withdrawalFees[exchangeId]?.[coin.coinSymbol]?.[network] ?? 0;
  }, [exchangeId, coin.coinSymbol, network]);

  const amount = parseFloat(amountStr) || 0;
  const received = Math.max(amount - fee, 0);

  const errors = useMemo(() => {
    if (!submitted) return {};
    const e: Record<string, string> = {};
    if (!network) e.network = "네트워크를 선택해주세요.";
    if (!address.trim()) e.address = "도착 주소를 입력해주세요.";
    if (needsTag && !tag.trim()) e.tag = "태그/메모를 입력해주세요.";
    if (amount <= 0) e.amount = "수량을 입력해주세요.";
    else if (amount > coin.available) e.amount = "가용 잔고를 초과합니다.";
    else if (amount <= fee) e.amount = "수수료보다 많은 수량을 입력해주세요.";
    return e;
  }, [submitted, network, address, tag, needsTag, amount, coin.available, fee]);

  function handleMaxClick() {
    setAmountStr(coin.available.toString());
  }

  async function handlePaste() {
    try {
      const text = await navigator.clipboard.readText();
      setAddress(text);
    } catch {
      // 클립보드 접근 실패 — 무시
    }
  }

  function handleSubmit() {
    setSubmitted(true);
    const hasError =
      !network ||
      !address.trim() ||
      (needsTag && !tag.trim()) ||
      amount <= 0 ||
      amount > coin.available ||
      amount <= fee;
    if (hasError) return;

    // 실제 API 연동 시 여기서 호출
    onClose();
  }

  function handleOpenChange(open: boolean) {
    if (!open) {
      onClose();
      setNetwork("");
      setAddress("");
      setTag("");
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
            <CoinIcon symbol={coin.coinSymbol} size={32} />
            <div>
              <DialogTitle>{coin.coinSymbol} 송금</DialogTitle>
              <DialogDescription className="mt-0.5">
                {coin.coinName}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-5 px-6 py-5">
          {/* Network */}
          <div className="space-y-2">
            <label className="text-sm font-medium">출금 네트워크</label>
            <Select value={network} onValueChange={setNetwork}>
              <SelectTrigger className="w-full">
                <SelectValue placeholder="네트워크 선택" />
              </SelectTrigger>
              <SelectContent>
                {networks.map((n) => (
                  <SelectItem key={n} value={n}>
                    {n}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {errors.network && (
              <p className="text-xs text-destructive">{errors.network}</p>
            )}
          </div>

          {/* Address */}
          <div className="space-y-2">
            <label className="text-sm font-medium">도착 주소</label>
            <div className="flex gap-2">
              <input
                type="text"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                placeholder="주소를 입력하세요"
                className="h-9 flex-1 rounded-md border border-input bg-transparent px-3 text-sm outline-none transition-colors placeholder:text-muted-foreground/40 focus:border-primary/40 focus:ring-1 focus:ring-ring/50"
              />
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handlePaste}
                className="shrink-0"
              >
                <ClipboardPaste className="mr-1 h-3.5 w-3.5" />
                붙여넣기
              </Button>
            </div>
            {errors.address && (
              <p className="text-xs text-destructive">{errors.address}</p>
            )}
          </div>

          {/* Tag / Memo */}
          {needsTag && (
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <label className="text-sm font-medium">태그/메모</label>
                <span className="inline-flex items-center rounded bg-destructive/10 px-1.5 py-0.5 text-[10px] font-semibold text-destructive">
                  필수
                </span>
              </div>
              <input
                type="text"
                value={tag}
                onChange={(e) => setTag(e.target.value)}
                placeholder="태그(메모)를 입력하세요"
                className="h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm outline-none transition-colors placeholder:text-muted-foreground/40 focus:border-primary/40 focus:ring-1 focus:ring-ring/50"
              />
              {errors.tag && (
                <p className="text-xs text-destructive">{errors.tag}</p>
              )}
            </div>
          )}

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

          {/* Summary */}
          <Separator />
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">출금 수수료</span>
              <span className="font-mono font-medium tabular-nums">
                {network ? `${formatDisplay(fee)} ${coin.coinSymbol}` : "—"}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">실수령량</span>
              <span className="font-mono font-semibold tabular-nums">
                {amount > 0 ? `${formatDisplay(received)} ${coin.coinSymbol}` : "—"}
              </span>
            </div>
          </div>

          {/* Warning */}
          <div className="flex items-start gap-2.5 rounded-lg bg-chart-4/10 px-3.5 py-3">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-chart-4" />
            <p className="text-xs leading-relaxed text-chart-4">
              잘못된 네트워크 또는 주소 선택 시 24시간 자금이 동결됩니다.
            </p>
          </div>

          {/* Submit */}
          <Button className="w-full" onClick={handleSubmit}>
            송금하기
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
