import { useState } from "react";
import { AlertTriangle, Copy, Check } from "lucide-react";
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
import { CoinIcon } from "@/components/market/CoinIcon";
import {
  coinChains,
  tagRequiredCoins,
  depositAddresses,
} from "@/mocks/wallet";
import type { WalletCoinBalance } from "@/lib/types/wallet";

interface DepositModalProps {
  isOpen: boolean;
  onClose: () => void;
  coin: WalletCoinBalance;
  exchangeId: string;
  baseCurrency: string;
}

type CopiedField = "address" | "tag" | null;

export function DepositModal({
  isOpen,
  onClose,
  coin,
  exchangeId,
}: DepositModalProps) {
  const [network, setNetwork] = useState("");
  const [copied, setCopied] = useState<CopiedField>(null);

  const networks = coinChains[coin.coinSymbol] ?? [];
  const needsTag = tagRequiredCoins.includes(coin.coinSymbol);

  const depositInfo = network
    ? depositAddresses[exchangeId]?.[network]
    : undefined;

  async function handleCopy(text: string, field: CopiedField) {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(field);
      setTimeout(() => setCopied(null), 2000);
    } catch {
      // 클립보드 접근 실패 — 무시
    }
  }

  function handleOpenChange(open: boolean) {
    if (!open) {
      onClose();
      setNetwork("");
      setCopied(null);
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent showCloseButton className="max-w-md gap-0 p-0">
        {/* Header */}
        <DialogHeader className="border-b border-border/30 px-6 py-5">
          <div className="flex items-center gap-3">
            <CoinIcon symbol={coin.coinSymbol} size={32} />
            <div>
              <DialogTitle>{coin.coinSymbol} 입금</DialogTitle>
              <DialogDescription className="mt-0.5">
                {coin.coinName}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-5 px-6 py-5">
          {/* Network */}
          <div className="space-y-2">
            <label className="text-sm font-medium">입금 네트워크</label>
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
          </div>

          {/* Deposit Address */}
          {depositInfo && (
            <>
              <div className="space-y-2">
                <label className="text-sm font-medium">입금 주소</label>
                <div className="flex items-center gap-2 rounded-lg border border-border/50 bg-secondary/20 px-3 py-2.5">
                  <p className="min-w-0 flex-1 truncate font-mono text-sm">
                    {depositInfo.address}
                  </p>
                  <button
                    type="button"
                    onClick={() => handleCopy(depositInfo.address, "address")}
                    className="shrink-0 rounded-md p-1.5 text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground"
                    aria-label="주소 복사"
                  >
                    {copied === "address" ? (
                      <Check className="h-4 w-4 text-emerald-500" />
                    ) : (
                      <Copy className="h-4 w-4" />
                    )}
                  </button>
                </div>
              </div>

              {/* Tag / Memo */}
              {needsTag && depositInfo.tag && (
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <label className="text-sm font-medium">태그/메모</label>
                    <span className="inline-flex items-center rounded bg-destructive/10 px-1.5 py-0.5 text-[10px] font-semibold text-destructive">
                      필수
                    </span>
                  </div>
                  <div className="flex items-center gap-2 rounded-lg border border-border/50 bg-secondary/20 px-3 py-2.5">
                    <p className="min-w-0 flex-1 truncate font-mono text-sm">
                      {depositInfo.tag}
                    </p>
                    <button
                      type="button"
                      onClick={() => handleCopy(depositInfo.tag!, "tag")}
                      className="shrink-0 rounded-md p-1.5 text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground"
                      aria-label="태그 복사"
                    >
                      {copied === "tag" ? (
                        <Check className="h-4 w-4 text-emerald-500" />
                      ) : (
                        <Copy className="h-4 w-4" />
                      )}
                    </button>
                  </div>
                </div>
              )}
            </>
          )}

          {/* Warning */}
          {network && (
            <div className="flex items-start gap-2.5 rounded-lg bg-chart-4/10 px-3.5 py-3">
              <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-chart-4" />
              <p className="text-xs leading-relaxed text-chart-4">
                반드시 <span className="font-semibold">{network}</span>{" "}
                네트워크로 입금하세요. 다른 네트워크 사용 시 자산이 손실됩니다.
              </p>
            </div>
          )}

          {/* Confirm */}
          <Button className="w-full" onClick={onClose}>
            확인
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
