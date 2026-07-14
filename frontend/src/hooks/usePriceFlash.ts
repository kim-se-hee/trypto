import { useEffect, useState } from "react";

export type FlashDirection = "up" | "down" | "same";

// 서서히 사라지게 하면 잔상이 다음 체결과 겹쳐 어느 행이 방금 바뀌었는지 오히려 흐려진다.
// 켰다가 그냥 끈다.
const FLASH_MS = 100;

interface FlashState {
  price: number;
  tickedAt?: number;
  direction: FlashDirection | null;
}

/**
 * 체결이 들어올 때마다 짧게 방향을 알린다. 가격이 오르면 up, 내리면 down,
 * 가격은 그대로인데 체결만 일어났으면 same 이다.
 *
 * 가격만 비교하면 같은 가격에 체결된 경우를 놓치므로 체결 시각으로 새 체결인지를 판정한다.
 * 시세를 다시 받아와도 체결 시각이 그대로면 반짝이지 않는다.
 */
export function usePriceFlash(price: number, tickedAt?: number): FlashDirection | null {
  const [seen, setSeen] = useState<FlashState>({ price, tickedAt, direction: null });

  // 방향은 직전 체결과 비교해야만 나오므로 화면에 그린 마지막 시세를 기억한다.
  // 목록이 가상화되어 있어 행이 다시 그려질 때는 그 시점의 시세로 시작한다 — 스크롤만으로는 반짝이지 않는다.
  if (tickedAt !== seen.tickedAt) {
    const isFirstTick = seen.tickedAt === undefined || tickedAt === undefined;
    setSeen({
      price,
      tickedAt,
      direction: isFirstTick
        ? null
        : price > seen.price ? "up"
        : price < seen.price ? "down"
        : "same",
    });
  }

  const { direction } = seen;
  useEffect(() => {
    if (direction === null) return;
    const timer = window.setTimeout(
      () => setSeen((prev) => ({ ...prev, direction: null })),
      FLASH_MS,
    );
    return () => window.clearTimeout(timer);
  }, [direction, tickedAt]);

  return direction;
}
