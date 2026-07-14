import { getExchangeCoins, type ExchangeCoinResponse } from "./exchange-api";

export interface OrderTargetIds {
  exchangeId: number;
  walletId: number;
  exchangeCoinId: number;
}

const BACKEND_EXCHANGE_ID_MAP: Record<string, number> = {
  upbit: 1,
  bithumb: 2,
  binance: 3,
};

// 거래소별 코인 목록 캐시
const exchangeCoinsCache = new Map<number, ExchangeCoinResponse[]>();

export function getBackendExchangeId(exchangeKey: string): number | null {
  return BACKEND_EXCHANGE_ID_MAP[exchangeKey] ?? null;
}

export function getExchangeKeyById(exchangeId: number): string | null {
  const entry = Object.entries(BACKEND_EXCHANGE_ID_MAP).find(([, id]) => id === exchangeId);
  return entry?.[0] ?? null;
}

async function fetchExchangeCoinsWithCache(exchangeId: number): Promise<ExchangeCoinResponse[]> {
  const cached = exchangeCoinsCache.get(exchangeId);
  if (cached) return cached;

  const coins = await getExchangeCoins(exchangeId);
  exchangeCoinsCache.set(exchangeId, coins);
  return coins;
}

export function clearExchangeCoinsCache(): void {
  exchangeCoinsCache.clear();
}

export async function resolveOrderTargetIds(
  exchangeKey: string,
  coinSymbol: string,
  getWalletId: (exchangeId: number) => number | null,
): Promise<OrderTargetIds | null> {
  const exchangeId = getBackendExchangeId(exchangeKey);
  if (!exchangeId) return null;

  const walletId = getWalletId(exchangeId);
  if (!walletId) return null;

  try {
    const coins = await fetchExchangeCoinsWithCache(exchangeId);
    const coin = coins.find((c) => c.coinSymbol.toUpperCase() === coinSymbol.toUpperCase());
    if (!coin) return null;

    return {
      exchangeId,
      walletId,
      exchangeCoinId: coin.exchangeCoinId,
    };
  } catch {
    return null;
  }
}
