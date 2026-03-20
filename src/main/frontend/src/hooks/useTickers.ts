import { useCallback, useEffect, useRef, useState } from "react";
import {
  connect,
  subscribeTickers,
  isConnected,
  type Ticker,
} from "@/lib/api/websocket";
import type { CoinData } from "@/lib/types/coins";

interface UseTickersOptions {
  exchangeId: number;
  initialCoins: CoinData[];
}

export function useTickers({ exchangeId, initialCoins }: UseTickersOptions): CoinData[] {
  const [tickerMap, setTickerMap] = useState<Map<string, Ticker>>(new Map());
  const subscriptionRef = useRef<ReturnType<typeof subscribeTickers>>(null);

  const handleTicker = useCallback((ticker: Ticker) => {
    setTickerMap((prev) => {
      const next = new Map(prev);
      next.set(ticker.symbol, ticker);
      return next;
    });
  }, []);

  useEffect(() => {
    if (!isConnected()) {
      connect();
    }

    // STOMP 연결 후 구독 (약간의 지연 허용)
    const timer = setTimeout(() => {
      subscriptionRef.current = subscribeTickers(exchangeId, handleTicker);
    }, 500);

    return () => {
      clearTimeout(timer);
      subscriptionRef.current?.unsubscribe();
      subscriptionRef.current = null;
      setTickerMap(new Map());
    };
  }, [exchangeId, handleTicker]);

  // initialCoins에 실시간 티커를 머지하여 반환
  return initialCoins.map((coin) => {
    const live = tickerMap.get(coin.symbol);
    if (!live) return coin;

    return {
      ...coin,
      currentPrice: live.price,
      changeRate: live.changeRate,
      volume: live.quoteTurnover,
    };
  });
}
