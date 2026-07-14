import { useEffect, useState } from "react";
import { getExchangeCoins } from "@/lib/api/exchange-api";
import type { CoinData } from "@/lib/types/coins";

// 아직 못 받아온 상태에서는 늘 같은 빈 배열을 돌려준다. 매번 새 배열을 만들면
// 이 값을 지켜보는 쪽(시세 병합·검색 색인)이 헛되이 다시 계산한다.
const EMPTY: CoinData[] = [];

/**
 * 거래소 상장 코인 목록을 API에서 조회한다.
 * 초기 시세 스냅샷(가격/변동률/거래대금)이 포함되며, 이후 WebSocket으로 실시간 갱신된다.
 */
export function useExchangeCoins(exchangeId: number): { coins: CoinData[]; loading: boolean } {
  // 어느 거래소의 목록인지 함께 담아 둔다. 그래야 거래소를 막 바꾼 직후의 '아직 못 받은 상태'를
  // 이펙트에서 setLoading(true) 로 알릴 필요 없이 렌더에서 그대로 판단할 수 있다.
  const [loaded, setLoaded] = useState<{ exchangeId: number; coins: CoinData[] } | null>(null);

  useEffect(() => {
    let cancelled = false;

    getExchangeCoins(exchangeId)
      .then((list) => {
        if (cancelled) return;
        setLoaded({
          exchangeId,
          coins: list.map((item) => ({
            symbol: item.coinSymbol,
            name: item.coinName,
            currentPrice: item.price,
            changeRate: item.changeRate,
            volume: item.volume,
          })),
        });
      })
      .catch(() => {
        if (!cancelled) setLoaded({ exchangeId, coins: [] });
      });

    return () => {
      cancelled = true;
    };
  }, [exchangeId]);

  const current = loaded?.exchangeId === exchangeId ? loaded : null;

  return { coins: current?.coins ?? EMPTY, loading: current === null };
}
