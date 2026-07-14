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
  // 콜백이 바뀔 때마다 구독을 다시 맺지 않으려고 ref 로 들고 있는다.
  // 갱신은 렌더가 끝난 뒤에 한다. 렌더 도중 ref 를 건드리면 렌더가 순수하지 않게 된다.
  const onOrderFilledRef = useRef(onOrderFilled);
  useEffect(() => {
    onOrderFilledRef.current = onOrderFilled;
  }, [onOrderFilled]);

  useEffect(() => {
    if (!userId) return;

    if (!isConnected()) {
      connect();
    }

    const unsubscribe = subscribeUserEvents(userId, (event) => {
      if (event.eventType === "ORDER_FILLED" && onOrderFilledRef.current) {
        onOrderFilledRef.current(event);
      }
    });

    return unsubscribe;
  }, [userId]);
}
