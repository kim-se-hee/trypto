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

function spark(base: number, rate: number): number[] {
  const trend = rate > 0 ? 1 : -1;
  const pts: number[] = [];
  let v = base * (1 - Math.abs(rate) / 100 * 1.5);
  for (let i = 0; i < 24; i++) {
    v += v * (Math.random() * 0.03 - 0.015 + trend * 0.002);
    pts.push(Math.round(v * 100) / 100);
  }
  pts.push(base);
  return pts;
}

export const exchanges: ExchangeData[] = [
  {
    id: "upbit",
    name: "업비트",
    type: "CEX",
    baseCurrency: "KRW",
    coins: [
      { symbol: "BTC", name: "비트코인", currentPrice: 143_250_000, changeRate: 2.34, volume: 892_400_000_000, marketCap: 2_834_000_000_000_000, sparkline: spark(143_250_000, 2.34) },
      { symbol: "ETH", name: "이더리움", currentPrice: 4_821_000, changeRate: -1.12, volume: 431_200_000_000, marketCap: 580_000_000_000_000, sparkline: spark(4_821_000, -1.12) },
      { symbol: "XRP", name: "리플", currentPrice: 3_456, changeRate: 5.67, volume: 287_600_000_000, marketCap: 197_000_000_000_000, sparkline: spark(3_456, 5.67) },
      { symbol: "SOL", name: "솔라나", currentPrice: 287_400, changeRate: 3.89, volume: 198_300_000_000, marketCap: 134_000_000_000_000, sparkline: spark(287_400, 3.89) },
      { symbol: "DOGE", name: "도지코인", currentPrice: 542, changeRate: -3.21, volume: 156_700_000_000, marketCap: 78_000_000_000_000, sparkline: spark(542, -3.21) },
      { symbol: "ADA", name: "에이다", currentPrice: 1_234, changeRate: 1.45, volume: 98_400_000_000, marketCap: 44_000_000_000_000, sparkline: spark(1_234, 1.45) },
      { symbol: "AVAX", name: "아발란체", currentPrice: 62_300, changeRate: -0.87, volume: 76_500_000_000, marketCap: 25_000_000_000_000, sparkline: spark(62_300, -0.87) },
      { symbol: "DOT", name: "폴카닷", currentPrice: 12_450, changeRate: 2.11, volume: 54_300_000_000, marketCap: 18_000_000_000_000, sparkline: spark(12_450, 2.11) },
      { symbol: "LINK", name: "체인링크", currentPrice: 28_900, changeRate: 4.56, volume: 45_600_000_000, marketCap: 18_500_000_000_000, sparkline: spark(28_900, 4.56) },
      { symbol: "MATIC", name: "폴리곤", currentPrice: 1_567, changeRate: -2.34, volume: 34_500_000_000, marketCap: 14_800_000_000_000, sparkline: spark(1_567, -2.34) },
      { symbol: "ATOM", name: "코스모스", currentPrice: 15_670, changeRate: 0.98, volume: 28_700_000_000, marketCap: 5_900_000_000_000, sparkline: spark(15_670, 0.98) },
      { symbol: "UNI", name: "유니스왑", currentPrice: 19_800, changeRate: -1.56, volume: 21_300_000_000, marketCap: 12_000_000_000_000, sparkline: spark(19_800, -1.56) },
      { symbol: "AAVE", name: "에이브", currentPrice: 456_000, changeRate: 3.12, volume: 18_900_000_000, marketCap: 6_800_000_000_000, sparkline: spark(456_000, 3.12) },
      { symbol: "SAND", name: "샌드박스", currentPrice: 876, changeRate: -4.23, volume: 12_400_000_000, marketCap: 2_000_000_000_000, sparkline: spark(876, -4.23) },
      { symbol: "MANA", name: "디센트럴랜드", currentPrice: 987, changeRate: 1.23, volume: 9_800_000_000, marketCap: 1_800_000_000_000, sparkline: spark(987, 1.23) },
    ],
  },
  {
    id: "bithumb",
    name: "빗썸",
    type: "CEX",
    baseCurrency: "KRW",
    coins: [
      { symbol: "BTC", name: "비트코인", currentPrice: 143_180_000, changeRate: 2.31, volume: 567_800_000_000, marketCap: 2_832_000_000_000_000, sparkline: spark(143_180_000, 2.31) },
      { symbol: "ETH", name: "이더리움", currentPrice: 4_815_000, changeRate: -1.18, volume: 312_400_000_000, marketCap: 579_000_000_000_000, sparkline: spark(4_815_000, -1.18) },
      { symbol: "XRP", name: "리플", currentPrice: 3_448, changeRate: 5.52, volume: 198_700_000_000, marketCap: 196_000_000_000_000, sparkline: spark(3_448, 5.52) },
      { symbol: "SOL", name: "솔라나", currentPrice: 287_100, changeRate: 3.76, volume: 143_200_000_000, marketCap: 133_500_000_000_000, sparkline: spark(287_100, 3.76) },
      { symbol: "DOGE", name: "도지코인", currentPrice: 540, changeRate: -3.15, volume: 112_300_000_000, marketCap: 77_500_000_000_000, sparkline: spark(540, -3.15) },
      { symbol: "ADA", name: "에이다", currentPrice: 1_230, changeRate: 1.38, volume: 67_800_000_000, marketCap: 43_500_000_000_000, sparkline: spark(1_230, 1.38) },
      { symbol: "AVAX", name: "아발란체", currentPrice: 62_100, changeRate: -0.92, volume: 54_300_000_000, marketCap: 24_800_000_000_000, sparkline: spark(62_100, -0.92) },
      { symbol: "DOT", name: "폴카닷", currentPrice: 12_420, changeRate: 2.05, volume: 38_900_000_000, marketCap: 17_800_000_000_000, sparkline: spark(12_420, 2.05) },
      { symbol: "LINK", name: "체인링크", currentPrice: 28_850, changeRate: 4.48, volume: 32_100_000_000, marketCap: 18_200_000_000_000, sparkline: spark(28_850, 4.48) },
      { symbol: "EOS", name: "이오스", currentPrice: 1_890, changeRate: -0.45, volume: 15_600_000_000, marketCap: 2_900_000_000_000, sparkline: spark(1_890, -0.45) },
      { symbol: "TRX", name: "트론", currentPrice: 234, changeRate: 0.67, volume: 12_300_000_000, marketCap: 20_400_000_000_000, sparkline: spark(234, 0.67) },
      { symbol: "QTUM", name: "퀀텀", currentPrice: 5_670, changeRate: -1.89, volume: 8_700_000_000, marketCap: 630_000_000_000, sparkline: spark(5_670, -1.89) },
    ],
  },
  {
    id: "binance",
    name: "바이낸스",
    type: "CEX",
    baseCurrency: "USDT",
    coins: [
      { symbol: "BTC", name: "Bitcoin", currentPrice: 97_842.50, changeRate: 2.41, volume: 28_450_000_000, marketCap: 1_938_000_000_000, sparkline: spark(97_842.50, 2.41) },
      { symbol: "ETH", name: "Ethereum", currentPrice: 3_298.75, changeRate: -1.05, volume: 15_670_000_000, marketCap: 396_000_000_000, sparkline: spark(3_298.75, -1.05) },
      { symbol: "BNB", name: "BNB", currentPrice: 712.30, changeRate: 0.89, volume: 2_340_000_000, marketCap: 106_000_000_000, sparkline: spark(712.30, 0.89) },
      { symbol: "SOL", name: "Solana", currentPrice: 196.45, changeRate: 3.92, volume: 4_560_000_000, marketCap: 91_500_000_000, sparkline: spark(196.45, 3.92) },
      { symbol: "XRP", name: "Ripple", currentPrice: 2.3648, changeRate: 5.78, volume: 3_890_000_000, marketCap: 134_500_000_000, sparkline: spark(2.3648, 5.78) },
      { symbol: "DOGE", name: "Dogecoin", currentPrice: 0.3712, changeRate: -3.18, volume: 2_780_000_000, marketCap: 53_200_000_000, sparkline: spark(0.3712, -3.18) },
      { symbol: "ADA", name: "Cardano", currentPrice: 0.8456, changeRate: 1.42, volume: 1_230_000_000, marketCap: 30_100_000_000, sparkline: spark(0.8456, 1.42) },
      { symbol: "AVAX", name: "Avalanche", currentPrice: 42.67, changeRate: -0.84, volume: 987_000_000, marketCap: 17_200_000_000, sparkline: spark(42.67, -0.84) },
      { symbol: "DOT", name: "Polkadot", currentPrice: 8.523, changeRate: 2.08, volume: 876_000_000, marketCap: 12_300_000_000, sparkline: spark(8.523, 2.08) },
      { symbol: "LINK", name: "Chainlink", currentPrice: 19.78, changeRate: 4.61, volume: 765_000_000, marketCap: 12_600_000_000, sparkline: spark(19.78, 4.61) },
      { symbol: "MATIC", name: "Polygon", currentPrice: 1.0723, changeRate: -2.41, volume: 654_000_000, marketCap: 10_100_000_000, sparkline: spark(1.0723, -2.41) },
      { symbol: "UNI", name: "Uniswap", currentPrice: 13.56, changeRate: -1.52, volume: 543_000_000, marketCap: 8_200_000_000, sparkline: spark(13.56, -1.52) },
      { symbol: "ATOM", name: "Cosmos", currentPrice: 10.72, changeRate: 0.95, volume: 432_000_000, marketCap: 4_000_000_000, sparkline: spark(10.72, 0.95) },
      { symbol: "ARB", name: "Arbitrum", currentPrice: 1.8934, changeRate: 6.23, volume: 387_000_000, marketCap: 7_600_000_000, sparkline: spark(1.8934, 6.23) },
      { symbol: "OP", name: "Optimism", currentPrice: 3.456, changeRate: -0.67, volume: 298_000_000, marketCap: 4_300_000_000, sparkline: spark(3.456, -0.67) },
    ],
  },
  {
    id: "jupiter",
    name: "Jupiter",
    type: "DEX",
    baseCurrency: "SOL",
    coins: [
      { symbol: "JUP", name: "Jupiter", currentPrice: 0.0052, changeRate: 8.34, volume: 12_400_000, marketCap: 7_100_000_000, sparkline: spark(0.0052, 8.34) },
      { symbol: "BONK", name: "Bonk", currentPrice: 0.00000012, changeRate: 15.23, volume: 8_900_000, marketCap: 1_200_000_000, sparkline: spark(0.00000012, 15.23) },
      { symbol: "RAY", name: "Raydium", currentPrice: 0.0321, changeRate: -2.45, volume: 5_600_000, marketCap: 890_000_000, sparkline: spark(0.0321, -2.45) },
      { symbol: "ORCA", name: "Orca", currentPrice: 0.0187, changeRate: 4.12, volume: 3_200_000, marketCap: 540_000_000, sparkline: spark(0.0187, 4.12) },
      { symbol: "MNGO", name: "Mango", currentPrice: 0.00089, changeRate: -6.78, volume: 1_800_000, marketCap: 89_000_000, sparkline: spark(0.00089, -6.78) },
      { symbol: "PYTH", name: "Pyth Network", currentPrice: 0.0024, changeRate: 3.56, volume: 4_300_000, marketCap: 2_100_000_000, sparkline: spark(0.0024, 3.56) },
      { symbol: "WIF", name: "dogwifhat", currentPrice: 0.0098, changeRate: 12.45, volume: 7_800_000, marketCap: 980_000_000, sparkline: spark(0.0098, 12.45) },
      { symbol: "RENDER", name: "Render", currentPrice: 0.0412, changeRate: -1.23, volume: 2_900_000, marketCap: 1_500_000_000, sparkline: spark(0.0412, -1.23) },
      { symbol: "HNT", name: "Helium", currentPrice: 0.0356, changeRate: 2.89, volume: 1_500_000, marketCap: 620_000_000, sparkline: spark(0.0356, 2.89) },
      { symbol: "MSOL", name: "Marinade SOL", currentPrice: 1.0842, changeRate: 0.15, volume: 980_000, marketCap: 450_000_000, sparkline: spark(1.0842, 0.15) },
    ],
  },
];

export const cexExchanges = exchanges.filter((e) => e.type === "CEX");
export const dexExchanges = exchanges.filter((e) => e.type === "DEX");
