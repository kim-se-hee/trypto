export interface HoldingData {
  coinSymbol: string;
  coinName: string;
  quantity: number;
  avgBuyPrice: number;
  currentPrice: number;
}

export interface PortfolioSummary {
  exchangeName: string;
  exchangeId: string;
  baseCurrency: string;
  availableCash: number;
  holdings: HoldingData[];
}
