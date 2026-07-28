import { Input } from "@/components/ui/input";
import { PresetButtons } from "./PresetButtons";
import { formatKRW } from "@/lib/formatters";
import { EXCHANGES } from "@/lib/types/coins";
import { cn } from "@/lib/utils";

const KRW_SEED_PRESETS = [
  { label: "100만", value: 1_000_000 },
  { label: "500만", value: 5_000_000 },
  { label: "1,000만", value: 10_000_000 },
  { label: "5,000만", value: 50_000_000 },
];

const USDT_SEED_PRESETS = [
  { label: "1,000", value: 1_000 },
  { label: "5,000", value: 5_000 },
  { label: "10,000", value: 10_000 },
  { label: "30,000", value: 30_000 },
];

const EMERGENCY_PRESETS = [
  { label: "10만", value: 100_000 },
  { label: "50만", value: 500_000 },
  { label: "100만", value: 1_000_000 },
];

export const SEED_LIMITS: Record<string, { min: number; max: number }> = {
  KRW: { min: 1_000_000, max: 50_000_000 },
  USDT: { min: 1_000, max: 30_000 },
};

// 0원은 "이 거래소엔 시드를 넣지 않음"이라 유효하다. 넣는 경우에만 통화별 한도를 검사한다.
export function seedAmountError(baseCurrency: string, amount: number): string | null {
  if (amount === 0) return null;
  const limit = SEED_LIMITS[baseCurrency];
  if (amount >= limit.min && amount <= limit.max) return null;
  return baseCurrency === "USDT"
    ? "1,000 ~ 30,000 USDT 범위로 입력해주세요"
    : "100만원 ~ 5,000만원 범위로 입력해주세요";
}

interface SeedMoneyCardProps {
  seeds: Record<number, number>;
  onSeedChange: (exchangeId: number, v: number) => void;
  emergencyLimit: number;
  onEmergencyLimitChange: (v: number) => void;
}

export function SeedMoneyCard({
  seeds,
  onSeedChange,
  emergencyLimit,
  onEmergencyLimitChange,
}: SeedMoneyCardProps) {
  return (
    <div className="flex flex-col gap-5">
      {/* 시드머니 */}
      <div className="rounded-xl border border-border bg-card p-5">
        <div>
          <div className="mb-4">
            <p className="text-sm font-bold">시작 자금</p>
            <p className="text-[11px] text-muted-foreground">
              거래소별로 기축통화 시드머니를 설정합니다. 1개 거래소 이상 입력이 필요합니다.
            </p>
          </div>

          <div className="flex flex-col gap-4">
            {EXCHANGES.map((exchange) => {
              const amount = seeds[exchange.id] ?? 0;
              const isUsdt = exchange.baseCurrency === "USDT";
              const error = seedAmountError(exchange.baseCurrency, amount);

              return (
                <div key={exchange.id} className="rounded-xl bg-secondary/40 p-4">
                  <div className="mb-2 flex items-center justify-between">
                    <p className="text-xs font-bold">{exchange.name}</p>
                    <span className="rounded-md bg-secondary px-2 py-0.5 text-[10px] font-semibold text-muted-foreground">
                      {exchange.baseCurrency}
                    </span>
                  </div>

                  <p className={cn(
                    "mb-2 font-mono text-xl font-bold tabular-nums tracking-tight",
                    amount > 0 ? "text-foreground" : "text-muted-foreground/40",
                  )}>
                    {isUsdt
                      ? `${amount.toLocaleString("en-US")} USDT`
                      : `₩${amount.toLocaleString("ko-KR")}`}
                  </p>
                  {!isUsdt && amount > 0 && (
                    <p className="mb-2 mt-0.5 text-xs font-medium text-primary">{formatKRW(amount)}</p>
                  )}

                  <Input
                    type="text"
                    inputMode="numeric"
                    value={amount > 0 ? amount.toLocaleString("ko-KR") : ""}
                    onChange={(e) => {
                      const raw = e.target.value.replace(/[^0-9]/g, "");
                      onSeedChange(exchange.id, raw ? Number(raw) : 0);
                    }}
                    placeholder="직접 입력 (0 = 미입력)"
                    className="mb-3 h-10 rounded-xl bg-white text-sm"
                  />

                  <PresetButtons
                    presets={isUsdt ? USDT_SEED_PRESETS : KRW_SEED_PRESETS}
                    onSelect={(v) => onSeedChange(exchange.id, v)}
                    activeValue={amount}
                  />

                  {error && (
                    <p className="mt-2 text-[11px] font-medium text-destructive">{error}</p>
                  )}
                </div>
              );
            })}
          </div>

          <div className="mt-3 flex items-center gap-1.5 rounded-lg bg-primary/8 px-3 py-2">
            <p className="text-[11px] font-medium text-primary">
              바이낸스 시드는 라운드 시작 시점 시세로 원화 환산되어 시드 총액에 합산됩니다
            </p>
          </div>
        </div>
      </div>

      {/* 긴급 자금 */}
      <div className="rounded-xl border border-border bg-card p-5">
        <div>
          <div className="mb-4">
            <p className="text-sm font-bold">긴급 자금 투입 상한</p>
            <p className="text-[11px] text-muted-foreground">1회당 최대 투입 금액 (원화 기준)</p>
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
