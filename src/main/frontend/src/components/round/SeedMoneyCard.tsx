import { Wallet, ShieldPlus } from "lucide-react";
import { Input } from "@/components/ui/input";
import { PresetButtons } from "./PresetButtons";
import { formatKRW } from "@/lib/formatters";
import { cn } from "@/lib/utils";

const SEED_PRESETS = [
  { label: "100만", value: 1_000_000 },
  { label: "500만", value: 5_000_000 },
  { label: "1,000만", value: 10_000_000 },
  { label: "5,000만", value: 50_000_000 },
];

const EMERGENCY_PRESETS = [
  { label: "10만", value: 100_000 },
  { label: "50만", value: 500_000 },
  { label: "100만", value: 1_000_000 },
];

interface SeedMoneyCardProps {
  seed: number;
  onSeedChange: (v: number) => void;
  emergencyLimit: number;
  onEmergencyLimitChange: (v: number) => void;
}

export function SeedMoneyCard({
  seed,
  onSeedChange,
  emergencyLimit,
  onEmergencyLimitChange,
}: SeedMoneyCardProps) {
  return (
    <div className="flex flex-col gap-5">
      {/* 시드머니 */}
      <div className="group relative overflow-hidden rounded-xl border border-border bg-card p-5">
        <div className={cn(
          "pointer-events-none absolute inset-0 bg-gradient-to-br from-primary/5 to-transparent",
          "opacity-0 transition-opacity duration-200 group-hover:opacity-100",
        )} />

        <div className="relative">
          <div className="mb-4 flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10">
              <Wallet className="h-4 w-4 text-primary" />
            </div>
            <div>
              <p className="text-sm font-bold">시작 자금</p>
              <p className="text-[11px] text-muted-foreground">모의투자에 사용할 초기 자본금</p>
            </div>
          </div>

          {/* 금액 표시 */}
          <div className="mb-3 rounded-xl bg-secondary/40 px-4 py-3">
            <p className={cn(
              "font-mono text-2xl font-bold tabular-nums tracking-tight",
              seed > 0 ? "text-foreground" : "text-muted-foreground/40",
            )}>
              {seed > 0 ? `₩${seed.toLocaleString("ko-KR")}` : "₩0"}
            </p>
            {seed > 0 && (
              <p className="mt-0.5 text-xs font-medium text-primary">{formatKRW(seed)}</p>
            )}
          </div>

          {/* 직접 입력 */}
          <Input
            type="text"
            inputMode="numeric"
            value={seed > 0 ? seed.toLocaleString("ko-KR") : ""}
            onChange={(e) => {
              const raw = e.target.value.replace(/[^0-9]/g, "");
              onSeedChange(raw ? Number(raw) : 0);
            }}
            placeholder="직접 입력"
            className="mb-3 h-10 rounded-xl bg-white text-sm"
          />

          <PresetButtons presets={SEED_PRESETS} onSelect={onSeedChange} activeValue={seed} />
        </div>
      </div>

      {/* 긴급 자금 */}
      <div className="group relative overflow-hidden rounded-xl border border-border bg-card p-5">
        <div className={cn(
          "pointer-events-none absolute inset-0 bg-gradient-to-br from-chart-4/5 to-transparent",
          "opacity-0 transition-opacity duration-200 group-hover:opacity-100",
        )} />

        <div className="relative">
          <div className="mb-4 flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-chart-4/10">
              <ShieldPlus className="h-4 w-4 text-chart-4" />
            </div>
            <div>
              <p className="text-sm font-bold">긴급 자금 투입 상한</p>
              <p className="text-[11px] text-muted-foreground">1회당 최대 투입 금액</p>
            </div>
          </div>

          <div className="mb-3 rounded-xl bg-secondary/40 px-4 py-3">
            <p className={cn(
              "font-mono text-2xl font-bold tabular-nums tracking-tight",
              emergencyLimit > 0 ? "text-foreground" : "text-muted-foreground/40",
            )}>
              {emergencyLimit > 0 ? `₩${emergencyLimit.toLocaleString("ko-KR")}` : "₩0"}
            </p>
            {emergencyLimit > 0 && (
              <p className="mt-0.5 text-xs font-medium text-chart-4">{formatKRW(emergencyLimit)}</p>
            )}
          </div>

          <Input
            type="text"
            inputMode="numeric"
            value={emergencyLimit > 0 ? emergencyLimit.toLocaleString("ko-KR") : ""}
            onChange={(e) => {
              const raw = e.target.value.replace(/[^0-9]/g, "");
              onEmergencyLimitChange(raw ? Number(raw) : 0);
            }}
            placeholder="직접 입력"
            className="mb-3 h-10 rounded-xl bg-white text-sm"
          />

          <PresetButtons
            presets={EMERGENCY_PRESETS}
            onSelect={onEmergencyLimitChange}
            activeValue={emergencyLimit}
          />

          <div className="mt-3 flex items-center gap-1.5 rounded-lg bg-chart-4/8 px-3 py-2">
            <p className="text-[11px] font-medium text-chart-4">
              라운드 진행 중 최대 3회까지 긴급 자금을 투입할 수 있습니다
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
