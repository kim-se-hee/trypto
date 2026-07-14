import { apiGet } from "./client";

export interface WalletBalanceItem {
  coinId: number;
  available: number;
  locked: number;
}

export interface WalletBalancesResponse {
  exchangeId: number;
  baseCurrencySymbol: string;
  baseCurrencyAvailable: number;
  baseCurrencyLocked: number;
  balances: WalletBalanceItem[];
}

export function getWalletBalances(walletId: number): Promise<WalletBalancesResponse> {
  return apiGet<WalletBalancesResponse>(`/api/wallets/${walletId}/balances`);
}
