import { useState, useMemo, useEffect, useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { MarketOverviewCards } from "@/components/market/MarketOverviewCards";
import { ExchangeTabs } from "@/components/market/ExchangeTabs";
import { CoinSearchInput } from "@/components/market/CoinSearchInput";
import { FilterChips } from "@/components/market/FilterChips";
import { CoinTable, type CoinSortKey } from "@/components/market/CoinTable";
import { CandleChartPanel } from "@/components/market/CandleChartPanel";
import { OrderPanel } from "@/components/market/OrderPanel";
import { EmergencyFundingCard } from "@/components/round/EmergencyFundingCard";
import { useRound } from "@/contexts/RoundContext";
import { useAuth } from "@/contexts/AuthContext";
import { useLoginPrompt } from "@/contexts/LoginPromptContext";
import { EXCHANGES } from "@/lib/types/coins";
import { isChosungQuery, toChosung, toJamo } from "@/lib/hangul";
import {
  resolveOrderTargetIds,
  type OrderTargetFailure,
  type OrderTargetResult,
} from "@/lib/api/id-mapping";
import { useExchangeCoins } from "@/hooks/useExchangeCoins";
import { useTickers } from "@/hooks/useTickers";
import { useUserEvents } from "@/hooks/useUserEvents";
import { useSort, type SortDir } from "@/hooks/useSort";
import type { UserEvent } from "@/lib/api/websocket";
import type { CoinData } from "@/lib/types/coins";
import type { FilterType } from "@/components/market/FilterChips";

export function MarketPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { user, isAuthenticated } = useAuth();
  const { promptLogin } = useLoginPrompt();
  const { activeRound, chargeEmergencyFunding, getWalletId } = useRound();

  const [orderFilledEvent, setOrderFilledEvent] = useState<UserEvent | null>(null);
  const handleOrderFilled = useCallback((event: UserEvent) => {
    setOrderFilledEvent(event);
  }, []);
  useUserEvents({ userId: user?.userId ?? null, onOrderFilled: handleOrderFilled });

  const selectedExchangeKey = searchParams.get("exchange") ?? EXCHANGES[0].key;
  const exchange = useMemo(
    () => EXCHANGES.find((e) => e.key === selectedExchangeKey) ?? EXCHANGES[0],
    [selectedExchangeKey],
  );

  const [searchQuery, setSearchQuery] = useState("");
  const [filter, setFilter] = useState<FilterType>("all");
  const [selectedSymbol, setSelectedSymbol] = useState<string | null>(null);

  // 정적 API로 코인 목록 조회
  const { coins: staticCoins, loading } = useExchangeCoins(exchange.id);

  // 실시간 티커 연동
  const coins = useTickers({
    exchangeId: exchange.id,
    initialCoins: staticCoins,
  });

  // 코인 이름의 초성·자모는 시세가 갱신돼도 변하지 않는다. 목록을 받아올 때 한 번만 풀어 둔다.
  const searchIndex = useMemo(() => {
    const index = new Map<string, { chosung: string; jamo: string }>();
    staticCoins.forEach((coin) => {
      index.set(coin.symbol, {
        chosung: toChosung(coin.name).toLowerCase(),
        jamo: toJamo(coin.name).toLowerCase(),
      });
    });
    return index;
  }, [staticCoins]);

  const filteredCoins = useMemo(() => {
    let filtered = coins;

    const query = searchQuery.trim().toLowerCase();
    if (query) {
      const chosungQuery = isChosungQuery(query);
      const jamoQuery = toJamo(query);
      filtered = filtered.filter((coin) => {
        if (coin.symbol.toLowerCase().includes(query)) return true;

        const index = searchIndex.get(coin.symbol);
        if (!index) return false;

        // 자음만 친 'ㅂㅌ' 은 이름 원문에 없는 글자다. 초성을 앞에서부터 맞춘다.
        // 그 밖의 입력은 조합 중이든 완성됐든 자모로 풀어서 부분 일치를 본다.
        return chosungQuery
          ? index.chosung.startsWith(query)
          : index.jamo.includes(jamoQuery);
      });
    }

    switch (filter) {
      case "rising":
        filtered = filtered.filter((c) => c.changeRate > 0);
        break;
      case "falling":
        filtered = filtered.filter((c) => c.changeRate < 0);
        break;
    }

    return filtered;
  }, [coins, searchIndex, searchQuery, filter]);

  const comparator = useCallback((key: CoinSortKey, dir: SortDir) => {
    return (a: CoinData, b: CoinData) => {
      let cmp = 0;
      switch (key) {
        case "name": cmp = a.symbol.localeCompare(b.symbol); break;
        case "price": cmp = a.currentPrice - b.currentPrice; break;
        case "change": cmp = a.changeRate - b.changeRate; break;
        case "volume": cmp = a.volume - b.volume; break;
      }
      return dir === "asc" ? cmp : -cmp;
    };
  }, []);

  const { sorted: sortedCoins, sortKey, sortDir, handleSort } = useSort<CoinData, CoinSortKey>({
    items: filteredCoins,
    defaultKey: "volume",
    comparator,
  });

  // 아무것도 고르지 않았으면 목록 맨 위 코인의 차트를 보여준다. 다만 검색으로 좁혀진 목록을 따라가면
  // 글자를 칠 때마다 차트가 바뀌므로, 걸러내기 전 전체 목록을 같은 기준으로 세워 그 첫 코인을 쓴다.
  const topCoin = useMemo(() => {
    if (!sortKey) return coins[0];
    return [...coins].sort(comparator(sortKey, sortDir))[0];
  }, [coins, sortKey, sortDir, comparator]);

  const selectedCoin = useMemo(() => {
    const fromSelection = coins.find((coin) => coin.symbol === selectedSymbol);
    return fromSelection ?? topCoin;
  }, [coins, selectedSymbol, topCoin]);

  // 해석 결과에 어느 코인의 것인지를 함께 담는다. 그래야 다른 코인으로 옮긴 직후
  // 아직 해석이 끝나지 않은 사이에 이전 코인의 주문 대상이 그대로 쓰이는 일이 없다.
  const [resolved, setResolved] = useState<{ target: string; result: OrderTargetResult } | null>(null);
  const selectedCoinSymbol = selectedCoin?.symbol ?? null;
  const orderTargetKey = selectedCoinSymbol ? `${exchange.key}:${selectedCoinSymbol}` : null;

  useEffect(() => {
    if (!selectedCoinSymbol || !orderTargetKey) return;

    let cancelled = false;
    void resolveOrderTargetIds(exchange.key, selectedCoinSymbol, getWalletId).then((result) => {
      if (!cancelled) setResolved({ target: orderTargetKey, result });
    });
    return () => { cancelled = true; };
  }, [exchange.key, selectedCoinSymbol, orderTargetKey, getWalletId]);

  const orderTarget = resolved?.target === orderTargetKey ? resolved.result : null;
  const orderTargetIds = orderTarget?.ok ? orderTarget.ids : null;
  // 로그인하지 않았으면 지갑이 없어 해석은 NO_ROUND 로 떨어진다. 라운드를 시작하라고 안내할 자리가
  // 아니라 로그인을 물어볼 자리이므로 여기서 가른다.
  const orderTargetFailure: OrderTargetFailure | null = !isAuthenticated
    ? "UNAUTHENTICATED"
    : orderTarget && !orderTarget.ok
      ? orderTarget.reason
      : null;

  const handleExchangeChange = (key: string) => {
    setSearchParams({ exchange: key });
    setSearchQuery("");
    setFilter("all");
    setSelectedSymbol(null);
  };

  const exchangeTabItems = EXCHANGES.map((e) => ({
    id: e.key,
    name: e.name,
    baseCurrency: e.baseCurrency,
  }));

  return (
    <div className="min-h-screen bg-background">
      <Header />

      {/* Page header */}
      <section className="animate-enter border-b border-border/40 pb-5 pt-6 sm:pb-6 sm:pt-8">
        <div className="mx-auto max-w-6xl px-4">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h1 className="font-display text-2xl tracking-tight sm:text-3xl">코인 시세</h1>
              <p className="mt-1.5 text-sm text-muted-foreground sm:mt-2">
                {exchange.name} 기준 · {exchange.baseCurrency} 마켓
              </p>
            </div>
          </div>
        </div>
      </section>

      <main className="mx-auto max-w-6xl px-4 py-6 sm:py-8">
        {/* Market overview cards */}
        <div className="animate-enter-delay-1">
          <MarketOverviewCards coins={coins} baseCurrency={exchange.baseCurrency} />
        </div>

        {/* Controls */}
        <div className="animate-enter-delay-2 mb-5 flex flex-wrap items-center gap-3 rounded-xl border border-border bg-card p-3 sm:p-4">
          <ExchangeTabs
            exchanges={exchangeTabItems}
            selected={selectedExchangeKey}
            onSelect={handleExchangeChange}
          />
          {/* 줄이 나뉘면 구분선만 홀로 남아 어색하다. 한 줄에 들어가는 폭에서만 세운다. */}
          {EXCHANGES.length > 1 && <div className="hidden h-6 w-px bg-border/60 sm:block" />}
          <FilterChips selected={filter} onSelect={setFilter} />
        </div>

        {loading ? (
          <div className="flex h-48 items-center justify-center text-sm text-muted-foreground">
            코인 목록을 불러오는 중...
          </div>
        ) : (
          <div className="animate-enter-delay-3 mt-6 grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
            <div className="space-y-5">
              {selectedCoin && (
                <CandleChartPanel
                  exchangeKey={exchange.key}
                  exchangeId={exchange.id}
                  baseCurrency={exchange.baseCurrency}
                  coin={selectedCoin}
                />
              )}

              {/* Coin table */}
              <CoinTable
                coins={sortedCoins}
                baseCurrency={exchange.baseCurrency}
                sortKey={sortKey}
                sortDir={sortDir}
                onSort={handleSort}
                selectedSymbol={selectedCoin?.symbol ?? null}
                onSelect={setSelectedSymbol}
                toolbar={<CoinSearchInput value={searchQuery} onChange={setSearchQuery} />}
              />
            </div>

            {/* Side panel — 주문 패널은 로그인이나 라운드가 없어도 자리를 지킨다.
                빈 화면보다, 값이 채워진 패널을 두고 무엇이 더 필요한지 그 자리에서 알리는 편이 낫다 */}
            <div className="space-y-5">
              {activeRound && (
                <EmergencyFundingCard
                  round={activeRound}
                  onCharge={chargeEmergencyFunding}
                />
              )}
              {selectedCoin && (
                <OrderPanel
                  baseCurrency={exchange.baseCurrency}
                  coinSymbol={selectedCoin.symbol}
                  coinName={selectedCoin.name}
                  currentPrice={selectedCoin.currentPrice}
                  feeRate={0.0005}
                  orderTargetIds={orderTargetIds}
                  orderTargetFailure={orderTargetFailure}
                  orderFilledEvent={orderFilledEvent}
                  onRequireLogin={() =>
                    promptLogin(`${selectedCoin.symbol} 주문은 로그인한 뒤에 넣을 수 있습니다.`)
                  }
                />
              )}
            </div>
          </div>
        )}

        {/* Footer info */}
        <p className="mt-4 text-[11px] text-muted-foreground/50">
          * 시세 데이터는 모의투자용이며 실제 시세와 다를 수 있습니다.
        </p>
      </main>
    </div>
  );
}
