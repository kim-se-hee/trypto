import { useLayoutEffect, useRef, useState, type CSSProperties } from "react";
import { useVirtualizer, type VirtualItem } from "@tanstack/react-virtual";

interface UseVirtualListOptions {
  count: number;
  rowHeight: number;
  overscan?: number;
}

/**
 * 목록을 페이지가 아니라 자체 스크롤 상자 안에서 스크롤시키고, 그 안에서도 보이는 구간의 행만 그린다.
 * 상장 코인이 거래소당 최대 600개를 넘어 전부 그리면 DOM 이 수천 노드가 되고, 실시간 시세가 들어올
 * 때마다 React 가 그 전부를 다시 훑는다.
 *
 * 행 높이가 고정이어야 전체 스크롤 높이를 계산할 수 있으므로 rowHeight 를 받는다.
 */
export function useVirtualList({ count, rowHeight, overscan = 6 }: UseVirtualListOptions) {
  const scrollRef = useRef<HTMLDivElement>(null);

  const virtualizer = useVirtualizer({
    count,
    getScrollElement: () => scrollRef.current,
    estimateSize: () => rowHeight,
    overscan,
  });

  // 헤더는 스크롤 상자 밖에 있어 스크롤바 폭만큼 본문보다 넓다. 그만큼 헤더 오른쪽을 비워야 열이 맞는다.
  // 목록이 비면 상자가 사라지므로, 다시 생겼을 때 재측정하도록 존재 여부를 의존성에 둔다.
  const [scrollbarWidth, setScrollbarWidth] = useState(0);
  const hasRows = count > 0;
  useLayoutEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    const measure = () => setScrollbarWidth(el.offsetWidth - el.clientWidth);
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(el);
    return () => observer.disconnect();
  }, [hasRows]);

  return { scrollRef, virtualizer, scrollbarWidth };
}

/** 가상화된 행은 전체 높이를 잡아둔 상자 위에 제자리로 얹는다. */
export function virtualRowStyle(item: VirtualItem, rowHeight: number): CSSProperties {
  return {
    position: "absolute",
    top: 0,
    left: 0,
    width: "100%",
    height: rowHeight,
    transform: `translateY(${item.start}px)`,
  };
}
