import { useEffect, useState } from "react";

/**
 * CSS 클래스만으로는 못 바꾸는 값(SVG 좌표계, 인라인 여백처럼 자바스크립트가 쥔 수치)을
 * 화면 폭에 따라 갈라야 할 때 쓴다. 보이고 감추는 정도는 tailwind 반응형 클래스로 충분하다.
 */
export function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() => window.matchMedia(query).matches);

  useEffect(() => {
    const mql = window.matchMedia(query);
    const onChange = () => setMatches(mql.matches);
    onChange();
    mql.addEventListener("change", onChange);
    return () => mql.removeEventListener("change", onChange);
  }, [query]);

  return matches;
}

/** tailwind 의 sm(640px) 아래를 모바일로 본다. 반응형 클래스와 기준을 맞춰야 화면이 어긋나지 않는다. */
export function useIsMobile(): boolean {
  return useMediaQuery("(max-width: 639px)");
}
