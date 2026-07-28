import { useMemo, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { PresetButtons } from "@/components/round/PresetButtons";
import { formatKRW } from "@/lib/formatters";
import { EXCHANGES } from "@/lib/types/coins";
import { cn } from "@/lib/utils";
import type { InvestmentRound } from "@/lib/types/round";

interface EmergencyFundingCardProps {
  round: InvestmentRound;
  onCharge: (exchangeId: number, amount: number) => Promise<boolean>;
}

export function EmergencyFundingCard({ round, onCharge }: EmergencyFundingCardProps) {
  const [open, setOpen] = useState(false);
  const [exchangeId, setExchangeId] = useState(EXCHANGES[0].id);
  const [amount, setAmount] = useState(0);

  const exchange = EXCHANGES.find((e) => e.id === exchangeId) ?? EXCHANGES[0];
  const isUsdt = exchange.baseCurrency === "USDT";

  const canCharge = round.status === "ACTIVE" && round.emergencyChargeCount > 0;
  // 상한은 원화 기준. USDT 는 투입 시점 시세로 서버가 환산 검증하므로 여기서는 양수만 확인한다.
  const isValidAmount = isUsdt
    ? amount > 0
    : amount > 0 && amount <= round.emergencyFundingLimit;

  const presets = useMemo(() => {
    const limit = round.emergencyFundingLimit;
    const values = [0.25, 0.5, 1].map((ratio) => Math.floor(limit * ratio));
    return [
      { label: "25%", value: values[0] },
      { label: "50%", value: values[1] },
      { label: "100%", value: values[2] },
    ];
  }, [round.emergencyFundingLimit]);

  function handleOpenChange(nextOpen: boolean) {
    setOpen(nextOpen);
    if (!nextOpen) {
      setExchangeId(EXCHANGES[0].id);
      setAmount(0);
    }
  }

  function handleExchangeSelect(nextExchangeId: number) {
    setExchangeId(nextExchangeId);
    setAmount(0);
  }

  async function handleConfirm() {
    if (!canCharge || !isValidAmount) return;
    const ok = await onCharge(exchangeId, amount);
    if (ok) {
      setOpen(false);
      setAmount(0);
    }
  }

  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-start gap-2.5">
          <div>
            <p className="text-sm font-semibold">긴급 자금 투입</p>
            <p className="text-xs text-muted-foreground">
              선택한 거래소의 기축통화 지갑에 들어갑니다. 라운드 진행 중 최대 3회까지 가능합니다.
            </p>
          </div>
        </div>
        <Badge variant={canCharge ? "default" : "secondary"}>
          {canCharge ? "사용 가능" : "사용 불가"}
        </Badge>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-3">
        <div className="rounded-xl bg-secondary/40 px-3 py-2">
          <p className="text-[11px] text-muted-foreground">1회 상한 (원화 기준)</p>
          <p className="mt-1 text-sm font-bold">{formatKRW(round.emergencyFundingLimit)}</p>
        </div>
        <div className="rounded-xl bg-secondary/40 px-3 py-2">
          <p className="text-[11px] text-muted-foreground">남은 횟수</p>
          <p className="mt-1 text-sm font-bold">{round.emergencyChargeCount}회</p>
        </div>
      </div>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogTrigger asChild>
          <Button className="mt-4 w-full" disabled={!canCharge}>
            긴급 자금 투입하기
          </Button>
        </DialogTrigger>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>긴급 자금 투입</DialogTitle>
            <DialogDescription>
              투입할 거래소를 선택하세요. 해당 거래소의 기축통화 지갑에 들어갑니다. 1회 상한은 원화
              기준 {formatKRW(round.emergencyFundingLimit)} 입니다.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3">
            <div className="flex gap-2">
              {EXCHANGES.map((e) => {
                const isActive = e.id === exchangeId;
                return (
                  <button
                    key={e.id}
                    type="button"
                    onClick={() => handleExchangeSelect(e.id)}
                    className={cn(
                      "flex-1 rounded-lg px-3 py-2 text-xs font-bold transition-all duration-150",
                      isActive
                        ? "bg-primary text-white shadow-sm"
                        : "bg-secondary/60 text-muted-foreground hover:bg-secondary hover:text-foreground",
                    )}
                  >
                    {e.name}
                    <span className="ml-1 text-[10px] font-semibold opacity-70">{e.baseCurrency}</span>
                  </button>
                );
              })}
            </div>

            <div className="rounded-xl bg-secondary/40 px-4 py-3">
              <p className={cn(
                "font-mono text-2xl font-bold tabular-nums tracking-tight",
                amount > 0 ? "text-foreground" : "text-muted-foreground/40",
              )}>
                {isUsdt
                  ? `${amount.toLocaleString("en-US")} USDT`
                  : `₩${amount.toLocaleString("ko-KR")}`}
              </p>
              {!isUsdt && amount > 0 && (
                <p className="mt-0.5 text-xs font-medium text-chart-4">{formatKRW(amount)}</p>
              )}
            </div>

            <Input
              type="text"
              inputMode="numeric"
              value={amount > 0 ? amount.toLocaleString("ko-KR") : ""}
              onChange={(e) => {
                const raw = e.target.value.replace(/[^0-9]/g, "");
                setAmount(raw ? Number(raw) : 0);
              }}
              placeholder="금액 입력"
              className="h-10 rounded-xl bg-white text-sm"
            />

            {!isUsdt && (
              <PresetButtons presets={presets} onSelect={setAmount} activeValue={amount} />
            )}

            {isUsdt && (
              <div className="rounded-lg bg-chart-4/8 px-3 py-2">
                <p className="text-xs font-medium text-chart-4">
                  USDT 투입은 투입 시점 시세로 원화 환산되어 상한을 검증합니다. 환산액이 상한을
                  넘으면 거절됩니다.
                </p>
              </div>
            )}

            {!isValidAmount && amount > 0 && (
              <div className="rounded-lg bg-destructive/10 px-3 py-2">
                <p className="text-xs font-medium text-destructive">
                  상한을 초과했습니다. {formatKRW(round.emergencyFundingLimit)} 이하로 입력해주세요.
                </p>
              </div>
            )}
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button variant="outline" onClick={() => setOpen(false)}>
              취소
            </Button>
            <Button onClick={handleConfirm} disabled={!canCharge || !isValidAmount}>
              투입 확정
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
