import { useMemo, useState } from "react";
import { AlertTriangle, PlusCircle } from "lucide-react";
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
import { cn } from "@/lib/utils";
import type { InvestmentRound } from "@/lib/types/round";

interface EmergencyFundingCardProps {
  round: InvestmentRound;
  onCharge: (amount: number) => Promise<boolean>;
}

export function EmergencyFundingCard({ round, onCharge }: EmergencyFundingCardProps) {
  const [open, setOpen] = useState(false);
  const [amount, setAmount] = useState(0);

  const canCharge = round.status === "ACTIVE" && round.emergencyChargeCount > 0;
  const isValidAmount = amount > 0 && amount <= round.emergencyFundingLimit;

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
      setAmount(0);
    }
  }

  async function handleConfirm() {
    if (!canCharge || !isValidAmount) return;
    const ok = await onCharge(amount);
    if (ok) {
      setOpen(false);
      setAmount(0);
    }
  }

  return (
    <div className="rounded-xl border border-border/40 bg-secondary/20 p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-start gap-2.5">
          <div className="mt-0.5 flex h-9 w-9 items-center justify-center rounded-lg bg-chart-4/10">
            <AlertTriangle className="h-4 w-4 text-chart-4" />
          </div>
          <div>
            <p className="text-sm font-semibold">긴급 자금 투입</p>
            <p className="text-xs text-muted-foreground">
              라운드 진행 중 최대 3회까지 가능합니다.
            </p>
          </div>
        </div>
        <Badge variant={canCharge ? "default" : "secondary"}>
          {canCharge ? "사용 가능" : "사용 불가"}
        </Badge>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-3">
        <div className="rounded-xl bg-background/60 px-3 py-2">
          <p className="text-[11px] text-muted-foreground">1회 상한</p>
          <p className="mt-1 text-sm font-bold">{formatKRW(round.emergencyFundingLimit)}</p>
        </div>
        <div className="rounded-xl bg-background/60 px-3 py-2">
          <p className="text-[11px] text-muted-foreground">남은 횟수</p>
          <p className="mt-1 text-sm font-bold">{round.emergencyChargeCount}회</p>
        </div>
      </div>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogTrigger asChild>
          <Button className="mt-4 w-full" disabled={!canCharge}>
            <PlusCircle className="mr-2 h-4 w-4" />
            긴급 자금 투입하기
          </Button>
        </DialogTrigger>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>긴급 자금 투입</DialogTitle>
            <DialogDescription>
              1회 상한은 {formatKRW(round.emergencyFundingLimit)} 입니다.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3">
            <div className="rounded-xl bg-secondary/40 px-4 py-3">
              <p className={cn(
                "font-mono text-2xl font-bold tabular-nums tracking-tight",
                amount > 0 ? "text-foreground" : "text-muted-foreground/40",
              )}>
                {amount > 0 ? `₩${amount.toLocaleString("ko-KR")}` : "₩0"}
              </p>
              {amount > 0 && (
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

            <PresetButtons presets={presets} onSelect={setAmount} activeValue={amount} />

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
