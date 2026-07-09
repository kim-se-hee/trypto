import { apiGet } from "./client";

export interface ExchangeCoinResponse {
  exchangeCoinId: number;
  coinId: number;
  coinSymbol: string;
  coinName: string;
  price: number;
  changeRate: number;
  volume: number;
}

export function getExchangeCoins(exchangeId: number): Promise<ExchangeCoinResponse[]> {
  return apiGet<ExchangeCoinResponse[]>(`/api/exchanges/${exchangeId}/coins`);
}
