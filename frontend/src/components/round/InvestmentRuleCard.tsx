import { Slider } from "@/components/ui/slider";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

interface InvestmentRuleCardProps {
  label: string;
  description: string;
  enabled: boolean;
  onToggle: (v: boolean) => void;
  value: number;
  onChange: (v: number) => void;
  min: number;
  max: number;
  unit: string;
  inputType: "slider" | "number";
}

export function InvestmentRuleCard({
  label,
  description,
  enabled,
  onToggle,
  value,
  onChange,
  min,
  max,
  unit,
  inputType,
}: InvestmentRuleCardProps) {
  return (
    <div
      className={cn(
        "rounded-xl border transition-all duration-200",
        enabled ? "border-primary/50 bg-card" : "border-transparent bg-secondary/30",
      )}
    >
      <div
        role="switch"
        aria-checked={enabled}
        aria-label={label}
        tabIndex={0}
        onClick={() => onToggle(!enabled)}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            onToggle(!enabled);
          }
        }}
        className="cursor-pointer select-none rounded-xl px-4 py-3.5 outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
      >
        <p className={cn(
          "text-sm font-bold transition-colors",
          !enabled && "text-muted-foreground",
        )}>
          {label}
        </p>
        <p className={cn(
          "text-[11px] transition-colors",
          enabled ? "text-muted-foreground" : "text-muted-foreground/60",
        )}>
          {description}
        </p>
      </div>

      {enabled && (
        <div className="flex items-center gap-3 px-4 pb-3.5">
          {inputType === "slider" ? (
            <>
              <Slider
                min={min}
                max={max}
                step={1}
                value={[value]}
                onValueChange={([v]) => onChange(v)}
                className="flex-1"
              />
              <span className="min-w-[44px] text-right font-mono text-sm font-bold tabular-nums text-primary">
                {value}{unit}
              </span>
            </>
          ) : (
            <div className="flex items-center gap-2">
              <Input
                type="number"
                min={min}
                max={max}
                value={value}
                onChange={(e) => {
                  const n = parseInt(e.target.value, 10);
                  if (!isNaN(n)) onChange(Math.max(min, Math.min(max, n)));
                }}
                className="h-8 w-16 rounded-lg bg-white text-center text-sm font-bold"
              />
              <span className="text-xs font-medium text-muted-foreground">{unit}</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
