import { useCallback, useEffect, useRef, useState } from "react";
import {
  connect,
  subscribePrices,
  isConnected,
  type LivePrice,
} from "@/lib/api/websocket";
import type { CoinData } from "@/lib/types/coins";

interface UseLivePricesOptions {
  exchangeId: number;
  initialCoins: CoinData[];
}

export function useLivePrices({ exchangeId, initialCoins }: UseLivePricesOptions): CoinData[] {
  const [priceMap, setPriceMap] = useState<Map<string, LivePrice>>(new Map());
  const subscriptionRef = useRef<ReturnType<typeof subscribePrices>>(null);

  const handlePrice = useCallback((price: LivePrice) => {
    setPriceMap((prev) => {
      const next = new Map(prev);
      next.set(price.symbol, price);
      return next;
    });
  }, []);

  useEffect(() => {
    if (!isConnected()) {
      connect();
    }

    // STOMP 연결 후 구독 (약간의 지연 허용)
    const timer = setTimeout(() => {
      subscriptionRef.current = subscribePrices(exchangeId, handlePrice);
    }, 500);

    return () => {
      clearTimeout(timer);
      subscriptionRef.current?.unsubscribe();
      subscriptionRef.current = null;
      setPriceMap(new Map());
    };
  }, [exchangeId, handlePrice]);

  // initialCoins에 실시간 가격을 머지하여 반환
  return initialCoins.map((coin) => {
    const live = priceMap.get(coin.symbol);
    if (!live) return coin;

    return {
      ...coin,
      currentPrice: live.price,
      changeRate: live.changeRate,
      volume: live.quoteTurnover,
    };
  });
}
