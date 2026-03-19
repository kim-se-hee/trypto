import { apiGet, apiPost } from "./client";

export interface CreateTransferRequest {
  idempotencyKey: string;
  fromWalletId: number;
  coinId: number;
  chain: string;
  toAddress: string;
  toTag?: string;
  amount: number;
}

export interface TransferCoinResponse {
  transferId: number;
  status: string;
  fee: number;
  failureReason: string | null;
  frozenUntil: string | null;
}

export interface TransferHistoryItem {
  transferId: number;
  type: "DEPOSIT" | "WITHDRAW";
  coinId: number;
  coinSymbol: string;
  chain: string;
  toAddress: string;
  toTag: string | null;
  amount: number;
  fee: number;
  status: string;
  failureReason: string | null;
  frozenUntil: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface CursorPageResponse<T> {
  content: T[];
  nextCursor: string | null;
  hasNext: boolean;
}

export function createTransfer(params: CreateTransferRequest): Promise<TransferCoinResponse> {
  return apiPost<TransferCoinResponse>("/api/transfers", params);
}

export function getTransferHistory(
  walletId: number,
  userId: number,
  params?: {
    type?: "DEPOSIT" | "WITHDRAW";
    cursor?: string;
    size?: number;
  },
): Promise<CursorPageResponse<TransferHistoryItem>> {
  return apiGet<CursorPageResponse<TransferHistoryItem>>(
    `/api/wallets/${walletId}/transfers`,
    {
      userId,
      type: params?.type,
      cursor: params?.cursor,
      size: params?.size,
    },
  );
}
