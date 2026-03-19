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

export interface CoinChainResponse {
  exchangeCoinChainId: number;
  chain: string;
  tagRequired: boolean;
}

export interface WithdrawalFeeResponse {
  fee: number;
  minWithdrawal: number;
}

export function getExchangeCoins(exchangeId: number): Promise<ExchangeCoinResponse[]> {
  return apiGet<ExchangeCoinResponse[]>(`/api/exchanges/${exchangeId}/coins`);
}

export function getCoinChains(exchangeId: number, coinId: number): Promise<CoinChainResponse[]> {
  return apiGet<CoinChainResponse[]>(`/api/exchanges/${exchangeId}/coins/${coinId}/chains`);
}

export function getWithdrawalFee(
  exchangeId: number,
  coinId: number,
  chain: string,
): Promise<WithdrawalFeeResponse> {
  return apiGet<WithdrawalFeeResponse>("/api/withdrawal-fees", {
    exchangeId,
    coinId,
    chain,
  });
}
