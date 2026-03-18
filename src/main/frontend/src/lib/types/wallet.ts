export interface WalletCoinBalance {
  coinSymbol: string;
  coinName: string;
  available: number;
  locked: number;
  currentPrice: number;
}

export interface WalletData {
  exchangeId: string;
  exchangeName: string;
  baseCurrency: string;
  walletAddress: string;
  chain: string;
  balances: WalletCoinBalance[];
}

export interface TransferRecord {
  id: string;
  exchangeId: string;
  type: "DEPOSIT" | "WITHDRAW";
  asset: string;
  amount: number;
  fee: number;
  network: string;
  address: string;
  tag?: string;
  txId?: string;
  status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED" | "RETURNED" | "DELAYED";
  requestedAt: string;
  completedAt?: string;
}
