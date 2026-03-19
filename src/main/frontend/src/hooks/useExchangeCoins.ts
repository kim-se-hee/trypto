import { useEffect, useState } from "react";
import { getExchangeCoins } from "@/lib/api/exchange-api";
import type { CoinData } from "@/lib/types/coins";

/**
 * 거래소 상장 코인 목록을 API에서 조회한다.
 * 초기 시세 스냅샷(가격/변동률/거래대금)이 포함되며, 이후 WebSocket으로 실시간 갱신된다.
 */
export function useExchangeCoins(exchangeId: number): { coins: CoinData[]; loading: boolean } {
  const [coins, setCoins] = useState<CoinData[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    getExchangeCoins(exchangeId)
      .then((list) => {
        if (cancelled) return;
        setCoins(
          list.map((item) => ({
            symbol: item.coinSymbol,
            name: item.coinName,
            currentPrice: item.price,
            changeRate: item.changeRate,
            volume: item.volume,
          })),
        );
      })
      .catch(() => {
        if (!cancelled) setCoins([]);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [exchangeId]);

  return { coins, loading };
}
