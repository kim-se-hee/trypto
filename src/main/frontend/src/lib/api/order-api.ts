import { apiGet, apiPost } from "./client";
import type { CursorPageResponseDto } from "./types";

export type OrderSide = "BUY" | "SELL";
export type OrderType = "MARKET" | "LIMIT";
export type OrderStatus = "FILLED" | "PENDING" | "CANCELLED" | "FAILED";

export interface OrderAvailability {
  available: number;
  currentPrice: number;
}

export interface OrderHistoryItem {
  orderId: number;
  exchangeCoinId: number;
  side: OrderSide;
  orderType: OrderType;
  status: OrderStatus;
  filledPrice: number | null;
  price: number | null;
  quantity: number;
  orderAmount: number;
  fee: number;
  createdAt: string;
  filledAt: string | null;
}

export interface PlaceOrderRequest {
  clientOrderId: string;
  walletId: number;
  exchangeCoinId: number;
  side: OrderSide;
  orderType: OrderType;
  price?: number;
  amount: number;
}

export interface PlaceOrderResult {
  orderId: number;
  side: OrderSide;
  orderType: OrderType;
  orderAmount: number;
  quantity: number;
  price: number | null;
  filledPrice: number | null;
  fee: number | null;
  status: OrderStatus;
  createdAt: string;
  filledAt: string | null;
}

export interface CancelOrderResult {
  orderId: number;
  status: OrderStatus;
}

export interface FindOrderHistoryParams {
  walletId: number;
  exchangeCoinId?: number;
  side?: OrderSide;
  status?: OrderStatus;
  cursorOrderId?: number;
  size?: number;
}

export async function getOrderAvailability(
  walletId: number,
  exchangeCoinId: number,
  side: OrderSide,
): Promise<OrderAvailability> {
  const data = await apiGet<OrderAvailability>("/api/orders/available", {
    walletId,
    exchangeCoinId,
    side,
  });

  return {
    available: Number(data.available),
    currentPrice: Number(data.currentPrice),
  };
}

export async function listOrderHistory(
  params: FindOrderHistoryParams,
): Promise<CursorPageResponseDto<OrderHistoryItem>> {
  const data = await apiGet<CursorPageResponseDto<OrderHistoryItem>>("/api/orders", {
    walletId: params.walletId,
    exchangeCoinId: params.exchangeCoinId,
    side: params.side,
    status: params.status,
    cursorOrderId: params.cursorOrderId,
    size: params.size,
  });

  return {
    content: data.content.map((item) => ({
      ...item,
      status: params.status ?? "FILLED",
      filledPrice: item.filledPrice != null ? Number(item.filledPrice) : null,
      price: item.price != null ? Number(item.price) : null,
      quantity: Number(item.quantity),
      orderAmount: Number(item.orderAmount),
      fee: Number(item.fee),
    })),
    nextCursor: data.nextCursor,
    hasNext: data.hasNext,
  };
}

export async function placeOrder(request: PlaceOrderRequest): Promise<PlaceOrderResult> {
  const data = await apiPost<PlaceOrderResult>("/api/orders", request);

  return {
    ...data,
    orderAmount: Number(data.orderAmount),
    quantity: Number(data.quantity),
    price: data.price != null ? Number(data.price) : null,
    filledPrice: data.filledPrice != null ? Number(data.filledPrice) : null,
    fee: data.fee != null ? Number(data.fee) : null,
  };
}

export function cancelOrder(orderId: number): Promise<CancelOrderResult> {
  return apiPost<CancelOrderResult>(`/api/orders/${orderId}/cancel`);
}

