import { apiGet } from "./client";

export type CandleInterval = "1m" | "1h" | "4h" | "1d" | "1w" | "1M";

export interface CandleItem {
  time: string;
  open: number;
  high: number;
  low: number;
  close: number;
}

interface CandleItemResponse {
  time?: string;
  timestamp?: string;
  open: number | string;
  high: number | string;
  low: number | string;
  close: number | string;
}

interface FindCandlesParams {
  exchange: string;
  coin: string;
  interval: CandleInterval;
  limit?: number;
  cursor?: string;
}

function normalizeCandleDate(date: Date, interval: CandleInterval): Date {
  const normalized = new Date(date);
  normalized.setMilliseconds(0);

  switch (interval) {
    case "1m":
      normalized.setSeconds(0);
      return normalized;
    case "1h":
      normalized.setMinutes(0, 0, 0);
      return normalized;
    case "4h":
      normalized.setMinutes(0, 0, 0);
      normalized.setHours(Math.floor(normalized.getHours() / 4) * 4);
      return normalized;
    case "1d":
      normalized.setHours(0, 0, 0, 0);
      return normalized;
    case "1w": {
      normalized.setHours(0, 0, 0, 0);
      const day = normalized.getDay();
      const diff = day === 0 ? 6 : day - 1;
      normalized.setDate(normalized.getDate() - diff);
      return normalized;
    }
    case "1M":
      normalized.setHours(0, 0, 0, 0);
      normalized.setDate(1);
      return normalized;
  }
}

export function normalizeCandleTime(time: string, interval: CandleInterval): string {
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) return time;
  return normalizeCandleDate(date, interval).toISOString();
}

const DEFAULT_CANDLE_API_PATH =
  (import.meta.env.VITE_CANDLE_API_PATH as string | undefined) ?? "/api/candles";

const EXCHANGE_CODE_MAP: Record<string, string> = {
  upbit: "UPBIT",
  bithumb: "BITHUMB",
  binance: "BINANCE",
};

export function resolveCandleExchangeCode(exchangeKey: string): string | null {
  return EXCHANGE_CODE_MAP[exchangeKey] ?? null;
}

export async function findCandles({
  exchange,
  coin,
  interval,
  limit = 60,
  cursor,
}: FindCandlesParams): Promise<CandleItem[]> {
  const data = await apiGet<CandleItemResponse[]>(DEFAULT_CANDLE_API_PATH, {
    exchange,
    coin,
    interval,
    limit,
    cursor,
  });

  return data
    .map((item) => ({
      time: normalizeCandleTime(item.time ?? item.timestamp ?? "", interval),
      open: Number(item.open),
      high: Number(item.high),
      low: Number(item.low),
      close: Number(item.close),
    }))
    .filter(
      (item) =>
        item.time &&
        Number.isFinite(item.open) &&
        Number.isFinite(item.high) &&
        Number.isFinite(item.low) &&
        Number.isFinite(item.close),
    )
    .sort((a, b) => new Date(a.time).getTime() - new Date(b.time).getTime());
}
