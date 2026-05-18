import { useEffect, useMemo, useRef, useState } from "react";
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

interface MergeCacheEntry {
  base: CoinData;
  ticker: Ticker;
  merged: CoinData;
}

export function useTickers({ exchangeId, initialCoins }: UseTickersOptions): CoinData[] {
  const [tickerMap, setTickerMap] = useState<Map<string, Ticker>>(new Map());
  const mergeCache = useRef<Map<string, MergeCacheEntry>>(new Map());

  useEffect(() => {
    if (!isConnected()) {
      connect();
    }

    const pending = new Map<string, Ticker>();
    let rafId: number | null = null;

    const flush = () => {
      rafId = null;
      if (pending.size === 0) return;
      const drained = pending;
      const replacement = new Map<string, Ticker>();
      drained.forEach((value, key) => replacement.set(key, value));
      pending.clear();
      setTickerMap((prev) => {
        const next = new Map(prev);
        replacement.forEach((value, key) => next.set(key, value));
        return next;
      });
    };

    const onBatch = (tickers: Ticker[]) => {
      tickers.forEach((ticker) => pending.set(ticker.symbol, ticker));
      if (rafId === null) {
        rafId = requestAnimationFrame(flush);
      }
    };

    const unsubscribe = subscribeTickers(exchangeId, onBatch);

    return () => {
      if (rafId !== null) cancelAnimationFrame(rafId);
      unsubscribe();
      mergeCache.current.clear();
      setTickerMap(new Map());
    };
  }, [exchangeId]);

  return useMemo(() => {
    if (tickerMap.size === 0) {
      mergeCache.current.clear();
      return initialCoins;
    }
    const cache = mergeCache.current;
    return initialCoins.map((coin) => {
      const live = tickerMap.get(coin.symbol);
      if (!live) {
        cache.delete(coin.symbol);
        return coin;
      }
      const cached = cache.get(coin.symbol);
      if (cached && cached.base === coin && cached.ticker === live) {
        return cached.merged;
      }
      const merged: CoinData = {
        ...coin,
        currentPrice: live.price,
        changeRate: live.changeRate,
        volume: live.quoteTurnover,
      };
      cache.set(coin.symbol, { base: coin, ticker: live, merged });
      return merged;
    });
  }, [initialCoins, tickerMap]);
}
