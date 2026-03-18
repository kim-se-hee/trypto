export type { HoldingData, PortfolioSummary } from "@/lib/types/portfolio";

import type { PortfolioSummary } from "@/lib/types/portfolio";

export const portfolioData: PortfolioSummary[] = [
  {
    exchangeName: "업비트",
    exchangeId: "upbit",
    baseCurrency: "KRW",
    availableCash: 2_450_000,
    holdings: [
      { coinSymbol: "BTC", coinName: "비트코인", quantity: 0.05234100, avgBuyPrice: 132_500_000, currentPrice: 143_250_000 },
      { coinSymbol: "ETH", coinName: "이더리움", quantity: 1.24500000, avgBuyPrice: 5_120_000, currentPrice: 4_821_000 },
      { coinSymbol: "XRP", coinName: "리플", quantity: 15_420.00000000, avgBuyPrice: 2_890, currentPrice: 3_456 },
      { coinSymbol: "SOL", coinName: "솔라나", quantity: 12.34000000, avgBuyPrice: 245_000, currentPrice: 287_400 },
      { coinSymbol: "DOGE", coinName: "도지코인", quantity: 85_000.00000000, avgBuyPrice: 620, currentPrice: 542 },
      { coinSymbol: "ADA", coinName: "에이다", quantity: 8_500.00000000, avgBuyPrice: 1_100, currentPrice: 1_234 },
      { coinSymbol: "AVAX", coinName: "아발란체", quantity: 45.60000000, avgBuyPrice: 58_000, currentPrice: 62_300 },
      { coinSymbol: "LINK", coinName: "체인링크", quantity: 120.00000000, avgBuyPrice: 25_400, currentPrice: 28_900 },
    ],
  },
  {
    exchangeName: "빗썸",
    exchangeId: "bithumb",
    baseCurrency: "KRW",
    availableCash: 1_230_000,
    holdings: [
      { coinSymbol: "BTC", coinName: "비트코인", quantity: 0.02100000, avgBuyPrice: 135_000_000, currentPrice: 143_180_000 },
      { coinSymbol: "ETH", coinName: "이더리움", quantity: 0.85000000, avgBuyPrice: 4_950_000, currentPrice: 4_815_000 },
      { coinSymbol: "ADA", coinName: "에이다", quantity: 12_000.00000000, avgBuyPrice: 1_050, currentPrice: 1_230 },
      { coinSymbol: "DOT", coinName: "폴카닷", quantity: 350.00000000, avgBuyPrice: 11_200, currentPrice: 12_420 },
    ],
  },
  {
    exchangeName: "바이낸스",
    exchangeId: "binance",
    baseCurrency: "USDT",
    availableCash: 5_420.50,
    holdings: [
      { coinSymbol: "BTC", coinName: "Bitcoin", quantity: 0.12000000, avgBuyPrice: 89_500, currentPrice: 97_842.50 },
      { coinSymbol: "ETH", coinName: "Ethereum", quantity: 3.50000000, avgBuyPrice: 3_150, currentPrice: 3_298.75 },
      { coinSymbol: "SOL", coinName: "Solana", quantity: 25.00000000, avgBuyPrice: 172.80, currentPrice: 196.45 },
      { coinSymbol: "LINK", coinName: "Chainlink", quantity: 200.00000000, avgBuyPrice: 16.50, currentPrice: 19.78 },
      { coinSymbol: "ARB", coinName: "Arbitrum", quantity: 1_500.00000000, avgBuyPrice: 1.45, currentPrice: 1.8934 },
    ],
  },
];
