export interface OrderTargetIds {
  exchangeId: number;
  walletId: number;
  exchangeCoinId: number;
}

const BACKEND_EXCHANGE_ID_MAP: Record<string, number> = {
  upbit: 1,
  bithumb: 2,
  binance: 3,
  jupiter: 4,
};

// Order API currently requires walletId + exchangeCoinId lookup from client.
// This table only contains IDs verified/assumed from project docs/tests.
// Add mappings here as backend seed data expands.
const ORDER_ID_MAP: Record<string, Record<string, { walletId: number; exchangeCoinId: number }>> = {
  upbit: {
    BTC: { walletId: 1, exchangeCoinId: 1 },
  },
  bithumb: {
    BTC: { walletId: 2, exchangeCoinId: 7 },
  },
  binance: {
    BTC: { walletId: 3, exchangeCoinId: 13 },
  },
};

export function getBackendExchangeId(exchangeKey: string): number | null {
  return BACKEND_EXCHANGE_ID_MAP[exchangeKey] ?? null;
}

export function resolveOrderTargetIds(
  exchangeKey: string,
  coinSymbol: string,
): OrderTargetIds | null {
  const exchangeId = getBackendExchangeId(exchangeKey);
  if (!exchangeId) return null;

  const byExchange = ORDER_ID_MAP[exchangeKey];
  if (!byExchange) return null;

  const ids = byExchange[coinSymbol.toUpperCase()];
  if (!ids) return null;

  return {
    exchangeId,
    walletId: ids.walletId,
    exchangeCoinId: ids.exchangeCoinId,
  };
}

