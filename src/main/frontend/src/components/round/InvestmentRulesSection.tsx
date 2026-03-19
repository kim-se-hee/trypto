import { InvestmentRuleCard } from "./InvestmentRuleCard";
import type { RuleType } from "@/lib/types/round";

export interface RuleState {
  enabled: boolean;
  value: number;
}

export type RulesMap = Record<RuleType, RuleState>;

interface InvestmentRulesSectionProps {
  rules: RulesMap;
  onRuleToggle: (type: RuleType, enabled: boolean) => void;
  onRuleValueChange: (type: RuleType, value: number) => void;
}

const RULE_CONFIGS: {
  type: RuleType;
  label: string;
  description: string;
  min: number;
  max: number;
  unit: string;
  inputType: "slider" | "number";
  defaultValue: number;
}[] = [
  {
    type: "STOP_LOSS",
    label: "손절",
    description: "설정한 손실률 도달 시 매도",
    min: 1,
    max: 50,
    unit: "%",
    inputType: "slider",
    defaultValue: 10,
  },
  {
    type: "TAKE_PROFIT",
    label: "익절",
    description: "설정한 수익률 도달 시 매도",
    min: 1,
    max: 100,
    unit: "%",
    inputType: "slider",
    defaultValue: 30,
  },
  {
    type: "NO_CHASE_BUY",
    label: "추격 매수 금지",
    description: "급등 코인 매수를 방지",
    min: 1,
    max: 50,
    unit: "%",
    inputType: "slider",
    defaultValue: 15,
  },
  {
    type: "AVERAGING_LIMIT",
    label: "물타기 제한",
    description: "같은 코인 반복 매수 제한",
    min: 1,
    max: 10,
    unit: "회",
    inputType: "number",
    defaultValue: 3,
  },
  {
    type: "OVERTRADE_LIMIT",
    label: "과매매 제한",
    description: "하루 거래 횟수 제한",
    min: 1,
    max: 50,
    unit: "회/일",
    inputType: "number",
    defaultValue: 10,
  },
];

export function getDefaultRules(): RulesMap {
  const map = {} as RulesMap;
  for (const cfg of RULE_CONFIGS) {
    map[cfg.type] = { enabled: false, value: cfg.defaultValue };
  }
  return map;
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
