import { apiGet } from "./client";

export interface HoldingItem {
  coinId: number;
  coinSymbol: string;
  coinName: string;
  quantity: number;
  avgBuyPrice: number;
  currentPrice: number;
}

export interface MyHoldingsResponse {
  exchangeId: number;
  baseCurrencyBalance: number;
  baseCurrencySymbol: string;
  holdings: HoldingItem[];
}

export function getMyHoldings(walletId: number): Promise<MyHoldingsResponse> {
  return apiGet<MyHoldingsResponse>(`/api/wallets/${walletId}/portfolio`);
}
