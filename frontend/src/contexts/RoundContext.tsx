import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type { InvestmentRound } from "@/lib/types/round";
import { useAuth } from "@/contexts/AuthContext";
import {
  chargeEmergencyFunding as chargeEmergencyFundingApi,
  createIdempotencyKey,
  createRound as createRoundApi,
  fetchActiveRound,
  fetchTotalRoundCount,
  type CreateRoundParams,
} from "@/lib/api/round-api";

interface RoundContextValue {
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

const RoundContext = createContext<RoundContextValue | null>(null);

export function RoundProvider({ children }: { children: ReactNode }) {
  const { user, isAuthLoading } = useAuth();
  const [activeRound, setActiveRound] = useState<InvestmentRound | null>(null);
  const [totalRoundCount, setTotalRoundCount] = useState(0);
  const [isRoundLoading, setIsRoundLoading] = useState(true);

  const refreshActiveRound = useCallback(async () => {
    // 세션 복구가 끝나기 전에는 로그인 여부를 알 수 없으니 판단을 미룬다.
    if (isAuthLoading) {
      setIsRoundLoading(true);
      return;
    }

    if (!user) {
      setActiveRound(null);
      setTotalRoundCount(0);
      setIsRoundLoading(false);
      return;
    }

    setIsRoundLoading(true);
    try {
      const [round, count] = await Promise.all([
        fetchActiveRound(user.userId),
        fetchTotalRoundCount(user.userId),
      ]);
      setActiveRound(round);
      setTotalRoundCount(count);
    } catch (error) {
      console.error("Failed to load round state", error);
      setActiveRound(null);
      setTotalRoundCount(0);
    } finally {
      setIsRoundLoading(false);
    }
  }, [user, isAuthLoading]);

  useEffect(() => {
    void refreshActiveRound();
  }, [refreshActiveRound]);

  const createRound = useCallback(async (params: CreateRoundParams): Promise<InvestmentRound | null> => {
    try {
      const round = await createRoundApi(params);
      setActiveRound(round);
      setTotalRoundCount((prev) => Math.max(prev + 1, round.roundNumber));
      return round;
    } catch (error) {
      console.error("Failed to create round", error);
      return null;
    }
  }, []);

  const clearRound = useCallback(() => {
    setActiveRound(null);
  }, []);

  const getWalletId = useCallback(
    (exchangeId: number): number | null => {
      if (!activeRound) return null;
      const wallet = activeRound.wallets.find((w) => w.exchangeId === exchangeId);
      return wallet?.walletId ?? null;
    },
    [activeRound],
  );

  const chargeEmergencyFunding = useCallback(
    async (amount: number, exchangeId: number): Promise<boolean> => {
      if (!activeRound || !user) return false;
      if (activeRound.status !== "ACTIVE") return false;
      if (activeRound.emergencyChargeCount <= 0) return false;
      if (amount <= 0 || amount > activeRound.emergencyFundingLimit) return false;

      try {
        const result = await chargeEmergencyFundingApi({
          roundId: activeRound.roundId,
          userId: user.userId,
          exchangeId,
          amount,
          idempotencyKey: createIdempotencyKey(),
        });

        setActiveRound((prev) => {
          if (!prev) return prev;
          return {
            ...prev,
            emergencyChargeCount: result.remainingChargeCount,
          };
        });

        return true;
      } catch (error) {
        console.error("Failed to charge emergency funding", error);
        return false;
      }
    },
    [activeRound, user],
  );

  const value = useMemo(
    () => ({
      activeRound,
      hasActiveRound: activeRound !== null,
      hasEverStartedRound: totalRoundCount > 0,
      isRoundLoading,
      createRound,
      clearRound,
      refreshActiveRound,
      chargeEmergencyFunding,
      getWalletId,
    }),
    [
      activeRound,
      totalRoundCount,
      isRoundLoading,
      createRound,
      clearRound,
      refreshActiveRound,
      chargeEmergencyFunding,
      getWalletId,
    ],
  );

  return <RoundContext.Provider value={value}>{children}</RoundContext.Provider>;
}

export function useRound(): RoundContextValue {
  const ctx = useContext(RoundContext);
  if (!ctx) throw new Error("useRound must be used within RoundProvider");
  return ctx;
}
