import { InvestmentRuleCard } from "./InvestmentRuleCard";
import { RULE_CONFIGS, type RulesMap, type SelectableRuleType } from "./rules";

interface InvestmentRulesSectionProps {
  rules: RulesMap;
  onRuleToggle: (type: SelectableRuleType, enabled: boolean) => void;
  onRuleValueChange: (type: SelectableRuleType, value: number) => void;
}

export function InvestmentRulesSection({
  rules,
  onRuleToggle,
  onRuleValueChange,
}: InvestmentRulesSectionProps) {
  const enabledCount = Object.values(rules).filter((r) => r.enabled).length;

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold tracking-tight">투자 원칙</h2>
          <p className="mt-0.5 text-xs font-medium text-muted-foreground">
            나만의 투자 규칙을 설정하세요
          </p>
        </div>
        {enabledCount > 0 && (
          <span className="rounded-full bg-primary/10 px-2.5 py-1 text-xs font-bold text-primary">
            {enabledCount}개 활성
          </span>
        )}
      </div>

      <div className="flex flex-col gap-2.5">
        {RULE_CONFIGS.map((cfg) => (
          <InvestmentRuleCard
            key={cfg.type}
            label={cfg.label}
            description={cfg.description}
            enabled={rules[cfg.type].enabled}
            onToggle={(v) => onRuleToggle(cfg.type, v)}
            value={rules[cfg.type].value}
            onChange={(v) => onRuleValueChange(cfg.type, v)}
            min={cfg.min}
            max={cfg.max}
            unit={cfg.unit}
            inputType={cfg.inputType}
          />
        ))}
      </div>

      {enabledCount === 0 && (
        <p className="mt-3 text-center text-xs font-medium text-muted-foreground">
          최소 1개 이상의 원칙을 활성화해주세요
        </p>
      )}
    </div>
  );
}
