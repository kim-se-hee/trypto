export interface CoinData {
  symbol: string;
  name: string;
  currentPrice: number;
  changeRate: number;
  volume: number;
  marketCap: number;
  sparkline: number[];
}

export interface ExchangeData {
  id: string;
  name: string;
  type: "CEX" | "DEX";
  baseCurrency: string;
  coins: CoinData[];
}

const COIN_COLORS: Record<string, string> = {
  BTC: "#f7931a",
  ETH: "#627eea",
  XRP: "#00aae4",
  SOL: "#9945ff",
  DOGE: "#c2a633",
  ADA: "#0033ad",
  AVAX: "#e84142",
  DOT: "#e6007a",
  LINK: "#2a5ada",
  MATIC: "#8247e5",
  ATOM: "#2e3148",
  UNI: "#ff007a",
  AAVE: "#b6509e",
  SAND: "#04adef",
  MANA: "#ff2d55",
  BNB: "#f3ba2f",
  ARB: "#28a0f0",
  OP: "#ff0420",
  EOS: "#000000",
  TRX: "#ef0027",
  QTUM: "#2e9ad0",
  JUP: "#00d18c",
  BONK: "#f8a100",
  RAY: "#6c5ce7",
  ORCA: "#ffda44",
  MNGO: "#e4572e",
  PYTH: "#7b61ff",
  WIF: "#c08b5c",
  RENDER: "#1a1a2e",
  HNT: "#474dff",
  MSOL: "#9945ff",
};

export function getCoinColor(symbol: string): string {
  return COIN_COLORS[symbol] ?? "#8b949e";
}

export const EXCHANGES = [
  { id: 1, key: "upbit", name: "업비트", type: "CEX" as const, baseCurrency: "KRW" },
  { id: 2, key: "bithumb", name: "빗썸", type: "CEX" as const, baseCurrency: "KRW" },
  { id: 3, key: "binance", name: "바이낸스", type: "CEX" as const, baseCurrency: "USDT" },
  { id: 4, key: "jupiter", name: "Jupiter", type: "DEX" as const, baseCurrency: "SOL" },
];
