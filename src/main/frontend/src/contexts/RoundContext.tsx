import { createContext, useContext, useState, useCallback, type ReactNode } from "react";
import {
  type InvestmentRound,
  type RuleType,
  createMockRound,
  clearMockRound,
  mockActiveRound,
  setMockActiveRound,
} from "@/mocks/round";

interface CreateRoundParams {
  userId: number;
  initialSeed: number;
  emergencyFundingLimit: number;
  rules: { ruleType: RuleType; thresholdValue: number }[];
}

interface RoundContextValue {
  activeRound: InvestmentRound | null;
  hasActiveRound: boolean;
  createRound: (params: CreateRoundParams) => InvestmentRound;
  clearRound: () => void;
  chargeEmergencyFunding: (amount: number) => boolean;
}

const RoundContext = createContext<RoundContextValue | null>(null);

export function RoundProvider({ children }: { children: ReactNode }) {
  const [activeRound, setActiveRound] = useState<InvestmentRound | null>(mockActiveRound);

  const createRound = useCallback((params: CreateRoundParams): InvestmentRound => {
    const round = createMockRound(
      params.userId,
      params.initialSeed,
      params.emergencyFundingLimit,
      params.rules,
    );
    setActiveRound(round);
    return round;
  }, []);

  const clearRound = useCallback(() => {
    clearMockRound();
    setActiveRound(null);
  }, []);

  const chargeEmergencyFunding = useCallback((amount: number): boolean => {
    if (!activeRound) return false;
    if (activeRound.status !== "ACTIVE") return false;
    if (activeRound.emergencyChargeCount <= 0) return false;
    if (amount <= 0 || amount > activeRound.emergencyFundingLimit) return false;

    const updated: InvestmentRound = {
      ...activeRound,
      emergencyChargeCount: Math.max(0, activeRound.emergencyChargeCount - 1),
    };
    setMockActiveRound(updated);
    setActiveRound(updated);
    return true;
  }, [activeRound]);

  return (
    <RoundContext.Provider
      value={{
        activeRound,
        hasActiveRound: activeRound !== null,
        createRound,
        clearRound,
        chargeEmergencyFunding,
      }}
    >
      {children}
    </RoundContext.Provider>
  );
}

export function useRound(): RoundContextValue {
  const ctx = useContext(RoundContext);
  if (!ctx) throw new Error("useRound must be used within RoundProvider");
  return ctx;
}
