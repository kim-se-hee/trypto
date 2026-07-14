import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
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
import { fetchExchangeCoinsWithCache } from "@/lib/api/id-mapping";
import { RoundContext } from "./RoundContext";

export function RoundProvider({ children }: { children: ReactNode }) {
  const { user, isAuthLoading } = useAuth();
  const [activeRound, setActiveRound] = useState<InvestmentRound | null>(null);
  const [totalRoundCount, setTotalRoundCount] = useState(0);
  const [isRoundLoading, setIsRoundLoading] = useState(true);
  // 거래소 ID -> 그 거래소에 상장된 코인 ID 집합. 상장 목록은 세션 중 바뀌지 않아 라운드마다 한 번만 받는다.
  const [listedCoinIds, setListedCoinIds] = useState<Map<number, Set<number>>>(new Map());

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

  useEffect(() => {
    if (!activeRound) {
      setListedCoinIds(new Map());
      return;
    }

    let cancelled = false;
    const exchangeIds = activeRound.wallets.map((w) => w.exchangeId);

    void Promise.all(exchangeIds.map((id) => fetchExchangeCoinsWithCache(id)))
      .then((lists) => {
        if (cancelled) return;
        setListedCoinIds(
          new Map(lists.map((coins, i) => [exchangeIds[i], new Set(coins.map((c) => c.coinId))])),
        );
      })
      .catch((error) => {
        console.error("Failed to load exchange coin listings", error);
      });

    return () => {
      cancelled = true;
    };
  }, [activeRound]);

  const isCoinListed = useCallback(
    (exchangeId: number, coinId: number): boolean => listedCoinIds.get(exchangeId)?.has(coinId) ?? false,
    [listedCoinIds],
  );

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
    async (amount: number): Promise<boolean> => {
      if (!activeRound || !user) return false;
      if (activeRound.status !== "ACTIVE") return false;
      if (activeRound.emergencyChargeCount <= 0) return false;
      if (amount <= 0 || amount > activeRound.emergencyFundingLimit) return false;

      try {
        const result = await chargeEmergencyFundingApi({
          roundId: activeRound.roundId,
          userId: user.userId,
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
      isCoinListed,
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
      isCoinListed,
    ],
  );

  return <RoundContext.Provider value={value}>{children}</RoundContext.Provider>;
}
