import { useEffect, useRef, useState } from "react";

/**
 * 요소가 뷰포트에 처음 들어온 순간을 알린다. 한 번 보이면 계속 true 로 유지된다 —
 * 스크롤 등장 애니메이션이 다시 감기면 오히려 산만하기 때문.
 */
export function useInView<T extends HTMLElement>(threshold = 0.15) {
  const ref = useRef<T | null>(null);
  const [inView, setInView] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setInView(true);
          observer.disconnect();
        }
      },
      { threshold },
    );

    observer.observe(el);
    return () => observer.disconnect();
  }, [threshold]);

  return { ref, inView };
}
