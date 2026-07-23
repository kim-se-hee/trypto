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

// 봉 경계는 서버·거래소와 같은 시간대로 자른다. 거래소 기준 시간대의 자정에 맞추기 위해 offsetMinutes 만큼
// 시프트한 뒤 UTC 로 내림하고 되돌린다. 업비트·바이낸스는 오프셋 0(UTC), 빗썸은 +9시간(KST).
// 보는 사람 로컬시간으로 자르면 서버가 준 진행봉과 실시간 봉이 다른 자리에 떨어져 봉이 둘로 갈라진다.
function normalizeCandleDate(date: Date, interval: CandleInterval, offsetMinutes: number): Date {
  const offsetMs = offsetMinutes * 60_000;
  const shifted = new Date(date.getTime() + offsetMs);

  switch (interval) {
    case "1m":
      shifted.setUTCSeconds(0, 0);
      break;
    case "1h":
      shifted.setUTCMinutes(0, 0, 0);
      break;
    case "4h":
      shifted.setUTCMinutes(0, 0, 0);
      shifted.setUTCHours(Math.floor(shifted.getUTCHours() / 4) * 4);
      break;
    case "1d":
      shifted.setUTCHours(0, 0, 0, 0);
      break;
    case "1w": {
      shifted.setUTCHours(0, 0, 0, 0);
      const day = shifted.getUTCDay();
      const diff = day === 0 ? 6 : day - 1;
      shifted.setUTCDate(shifted.getUTCDate() - diff);
      break;
    }
    case "1M":
      shifted.setUTCHours(0, 0, 0, 0);
      shifted.setUTCDate(1);
      break;
  }

  return new Date(shifted.getTime() - offsetMs);
}

export function normalizeCandleTime(
  time: string,
  interval: CandleInterval,
  offsetMinutes: number,
): string {
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) return time;
  return normalizeCandleDate(date, interval, offsetMinutes).toISOString();
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

// 거래소별 캔들 일 경계 타임존 오프셋(분). 빗썸만 KST(00:00 KST), 업비트·바이낸스는 UTC(00:00 UTC).
const EXCHANGE_CANDLE_TZ_OFFSET_MINUTES: Record<string, number> = {
  UPBIT: 0,
  BINANCE: 0,
  BITHUMB: 9 * 60,
};

export function candleTzOffsetMinutes(exchange: string): number {
  return EXCHANGE_CANDLE_TZ_OFFSET_MINUTES[exchange] ?? 0;
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

  const offsetMinutes = candleTzOffsetMinutes(exchange);

  return data
    .map((item) => ({
      time: normalizeCandleTime(item.time ?? item.timestamp ?? "", interval, offsetMinutes),
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
