import { Client, type StompSubscription } from "@stomp/stompjs";

export interface Ticker {
  coinId: number;
  symbol: string;
  price: number;
  changeRate: number;
  quoteTurnover: number;
  timestamp: number;
}

export interface UserEvent {
  eventType: string;
  walletId: number;
  orderId: number;
  coinId: number;
  side: string;
  quantity: number;
  price: number;
  fee: number;
}

const WS_BASE_URL = (import.meta.env.VITE_WS_BASE_URL as string | undefined) ?? "";

function resolveWsUrl(): string {
  if (WS_BASE_URL) return WS_BASE_URL;

  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}/ws`;
}

let client: Client | null = null;

export function connect(): void {
  if (client?.active) return;

  client = new Client({
    brokerURL: resolveWsUrl(),
    reconnectDelay: 1000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    connectionTimeout: 5000,
  });

  // 지수 백오프 (1s → 30s)
  let reconnectAttempts = 0;
  const originalReconnectDelay = client.reconnectDelay;

  client.onWebSocketClose = () => {
    reconnectAttempts++;
    const delay = Math.min(originalReconnectDelay * Math.pow(2, reconnectAttempts), 30000);
    if (client) {
      client.reconnectDelay = delay;
    }
  };

  client.onConnect = () => {
    reconnectAttempts = 0;
    if (client) {
      client.reconnectDelay = originalReconnectDelay;
    }
  };

  client.onStompError = (frame) => {
    console.error("STOMP error:", frame.headers.message);
  };

  client.activate();
}

export function disconnect(): void {
  if (client?.active) {
    void client.deactivate();
  }
  client = null;
}

export function subscribeTickers(
  exchangeId: number,
  callback: (ticker: Ticker) => void,
): StompSubscription | null {
  if (!client?.active) return null;

  return client.subscribe(`/topic/tickers.${exchangeId}`, (message) => {
    try {
      const data = JSON.parse(message.body) as Ticker;
      callback(data);
    } catch {
      // ignore parse errors
    }
  });
}

export function subscribeUserEvents(
  userId: number,
  callback: (event: UserEvent) => void,
): StompSubscription | null {
  if (!client?.active) return null;

  return client.subscribe(`/user/${userId}/queue/events`, (message) => {
    try {
      const data = JSON.parse(message.body) as UserEvent;
      callback(data);
    } catch {
      // ignore parse errors
    }
  });
}

export function isConnected(): boolean {
  return client?.active ?? false;
}
