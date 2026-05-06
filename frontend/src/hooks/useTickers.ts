import { useEffect, useMemo, useRef, useState } from "react";
import type { StompSubscription } from "@stomp/stompjs";
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

    const onTicker = (ticker: Ticker) => {
      pending.set(ticker.symbol, ticker);
      if (rafId === null) {
        rafId = requestAnimationFrame(flush);
      }
    };

    let cancelled = false;
    let subscription: StompSubscription | null = null;
    let retryId: number | null = null;

    const trySubscribe = () => {
      if (cancelled) return;
      subscription = subscribeTickers(exchangeId, onTicker);
      if (!subscription) {
        retryId = window.setTimeout(trySubscribe, 50);
      }
    };
    trySubscribe();

    return () => {
      cancelled = true;
      if (retryId !== null) window.clearTimeout(retryId);
      if (rafId !== null) cancelAnimationFrame(rafId);
      subscription?.unsubscribe();
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
