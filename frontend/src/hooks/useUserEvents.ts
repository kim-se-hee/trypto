import { useEffect, useRef } from "react";
import type { StompSubscription } from "@stomp/stompjs";
import {
  connect,
  subscribeUserEvents,
  isConnected,
  type UserEvent,
} from "@/lib/api/websocket";

interface UseUserEventsOptions {
  userId: number | null;
  onOrderFilled?: (event: UserEvent) => void;
}

export function useUserEvents({ userId, onOrderFilled }: UseUserEventsOptions): void {
  const onOrderFilledRef = useRef(onOrderFilled);
  onOrderFilledRef.current = onOrderFilled;

  useEffect(() => {
    if (!userId) return;

    if (!isConnected()) {
      connect();
    }

    let cancelled = false;
    let subscription: StompSubscription | null = null;
    let retryId: number | null = null;

    const trySubscribe = () => {
      if (cancelled) return;
      subscription = subscribeUserEvents(userId, (event) => {
        if (event.eventType === "ORDER_FILLED" && onOrderFilledRef.current) {
          onOrderFilledRef.current(event);
        }
      });
      if (!subscription) {
        retryId = window.setTimeout(trySubscribe, 50);
      }
    };
    trySubscribe();

    return () => {
      cancelled = true;
      if (retryId !== null) window.clearTimeout(retryId);
      subscription?.unsubscribe();
    };
  }, [userId]);
}
