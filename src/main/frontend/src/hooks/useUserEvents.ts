import { useEffect, useRef } from "react";
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
  const subscriptionRef = useRef<ReturnType<typeof subscribeUserEvents>>(null);
  const onOrderFilledRef = useRef(onOrderFilled);
  onOrderFilledRef.current = onOrderFilled;

  useEffect(() => {
    if (!userId) return;

    if (!isConnected()) {
      connect();
    }

    const timer = setTimeout(() => {
      subscriptionRef.current = subscribeUserEvents(userId, (event) => {
        if (event.eventType === "ORDER_FILLED" && onOrderFilledRef.current) {
          onOrderFilledRef.current(event);
        }
      });
    }, 500);

    return () => {
      clearTimeout(timer);
      subscriptionRef.current?.unsubscribe();
      subscriptionRef.current = null;
    };
  }, [userId]);
}
