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

export interface DepositAddressResponse {
  depositAddressId: number;
  walletId: number;
  chain: string;
  address: string;
  tag: string | null;
}

export function getWalletBalances(
  userId: number,
  walletId: number,
): Promise<WalletBalancesResponse> {
  return apiGet<WalletBalancesResponse>(`/api/users/${userId}/wallets/${walletId}/balances`);
}

export function getDepositAddress(
  walletId: number,
  coinId: number,
  chain: string,
): Promise<DepositAddressResponse> {
  return apiGet<DepositAddressResponse>(`/api/wallets/${walletId}/deposit-address`, {
    coinId,
    chain,
  });
}
