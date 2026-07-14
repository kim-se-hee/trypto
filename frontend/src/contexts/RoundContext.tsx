import { createContext, useContext } from "react";
import type { InvestmentRound } from "@/lib/types/round";
import type { CreateRoundParams } from "@/lib/api/round-api";

// 컴포넌트(RoundProvider)는 같은 파일에 두지 않는다. 컴포넌트와 훅을 한 파일에서 함께 내보내면
// 개발 중 파일을 고칠 때마다 화면이 통째로 새로고침되어 상태가 날아간다.

export interface RoundContextValue {
  activeRound: InvestmentRound | null;
  hasActiveRound: boolean;
  hasEverStartedRound: boolean;
  isRoundLoading: boolean;
  createRound: (params: CreateRoundParams) => Promise<InvestmentRound | null>;
  clearRound: () => void;
  refreshActiveRound: () => Promise<void>;
  chargeEmergencyFunding: (amount: number, exchangeId: number) => Promise<boolean>;
  getWalletId: (exchangeId: number) => number | null;
}

export const RoundContext = createContext<RoundContextValue | null>(null);

export function useRound(): RoundContextValue {
  const ctx = useContext(RoundContext);
  if (!ctx) throw new Error("useRound must be used within RoundProvider");
  return ctx;
}
