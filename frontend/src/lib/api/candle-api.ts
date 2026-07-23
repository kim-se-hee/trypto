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

// 4시간·일·주·월 경계는 서버·거래소와 같은 00:00 UTC 기준으로 자른다(업비트·바이낸스 일 경계 = 09시 KST = 00:00 UTC).
// 보는 사람 로컬시간으로 자르면 서버가 준 진행봉과 실시간 봉이 다른 자리에 떨어져 봉이 둘로 갈라진다.
// 1분·1시간은 KST 가 UTC 와 정시가 겹쳐 로컬로 잘라도 같은 순간이므로 그대로 둔다.
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
      normalized.setUTCMinutes(0, 0, 0);
      normalized.setUTCHours(Math.floor(normalized.getUTCHours() / 4) * 4);
      return normalized;
    case "1d":
      normalized.setUTCHours(0, 0, 0, 0);
      return normalized;
    case "1w": {
      normalized.setUTCHours(0, 0, 0, 0);
      const day = normalized.getUTCDay();
      const diff = day === 0 ? 6 : day - 1;
      normalized.setUTCDate(normalized.getUTCDate() - diff);
      return normalized;
    }
    case "1M":
      normalized.setUTCHours(0, 0, 0, 0);
      normalized.setUTCDate(1);
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
