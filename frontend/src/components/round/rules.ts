import type { RuleType } from "@/lib/types/round";

export interface RuleState {
  enabled: boolean;
  value: number;
}

/**
 * 손절(STOP_LOSS)·익절(TAKE_PROFIT)은 백엔드에 위반 판정 로직이 없어 설정해도 아무 위반도 기록되지 않는다.
 * 지키지 않아도 복기에 0건으로 나와 사용자를 오도하므로, 판정이 구현될 때까지 설정 대상에서 제외한다.
 */
export type SelectableRuleType = Exclude<RuleType, "STOP_LOSS" | "TAKE_PROFIT">;

export type RulesMap = Record<SelectableRuleType, RuleState>;

export const RULE_CONFIGS: {
  type: SelectableRuleType;
  label: string;
  description: string;
  min: number;
  max: number;
  unit: string;
  inputType: "slider" | "number";
  defaultValue: number;
}[] = [
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
    description: "손실 중인 코인의 추가 매수 횟수 제한",
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
