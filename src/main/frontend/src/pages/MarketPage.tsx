import { useState, useMemo } from "react";
import { Header } from "@/components/layout/Header";
import { MarketOverviewCards } from "@/components/market/MarketOverviewCards";
import { MarketTypeTabs } from "@/components/market/MarketTypeTabs";
import { ExchangeTabs } from "@/components/market/ExchangeTabs";
import { CoinSearchInput } from "@/components/market/CoinSearchInput";
import { FilterChips } from "@/components/market/FilterChips";
import { CoinTable } from "@/components/market/CoinTable";
import { cexExchanges, dexExchanges } from "@/mocks/coins";
import type { MarketType } from "@/components/market/MarketTypeTabs";
import type { FilterType } from "@/components/market/FilterChips";

export function MarketPage() {
  const [marketType, setMarketType] = useState<MarketType>("cex");
  const [selectedExchange, setSelectedExchange] = useState(cexExchanges[0].id);
  const [searchQuery, setSearchQuery] = useState("");
  const [filter, setFilter] = useState<FilterType>("all");

  const isCex = marketType === "cex";
  const activeExchanges = isCex ? cexExchanges : dexExchanges;

  const exchange = useMemo(
    () => activeExchanges.find((e) => e.id === selectedExchange) ?? activeExchanges[0],
    [activeExchanges, selectedExchange],
  );

  const filteredCoins = useMemo(() => {
    let coins = exchange.coins;

    if (searchQuery.trim()) {
      const query = searchQuery.trim().toLowerCase();
      coins = coins.filter(
        (coin) =>
          coin.symbol.toLowerCase().includes(query) ||
          coin.name.toLowerCase().includes(query),
      );
    }

    switch (filter) {
      case "rising":
        coins = coins.filter((c) => c.changeRate > 0);
        break;
      case "falling":
        coins = coins.filter((c) => c.changeRate < 0);
        break;
      case "volume":
        coins = [...coins].sort((a, b) => b.volume - a.volume);
        break;
    }

    return coins;
  }, [exchange.coins, searchQuery, filter]);

  const handleMarketTypeChange = (type: MarketType) => {
    setMarketType(type);
    const newExchanges = type === "cex" ? cexExchanges : dexExchanges;
    setSelectedExchange(newExchanges[0].id);
    setSearchQuery("");
    setFilter("all");
  };

  return (
    <div className="min-h-screen bg-background">
      <Header />

      {/* Hero section */}
      <section className="bg-gradient-to-r from-primary/8 via-chart-2/6 to-primary/4 pb-8 pt-8">
        <div className="mx-auto max-w-6xl px-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-extrabold tracking-tight">
                {isCex ? "코인 시세" : "DEX 시세"}
              </h1>
              <p className="mt-1.5 text-sm font-medium text-muted-foreground">
                {exchange.name} 기준 &middot; {exchange.baseCurrency} 마켓
              </p>
            </div>
            <MarketTypeTabs selected={marketType} onSelect={handleMarketTypeChange} />
          </div>
        </div>
      </section>

      <main className="mx-auto max-w-6xl px-4 py-6">
        {/* Market overview cards */}
        <MarketOverviewCards coins={exchange.coins} baseCurrency={exchange.baseCurrency} />

        {/* Controls card */}
        <div className="mb-4 flex items-center gap-3 rounded-2xl bg-card p-4 shadow-[0_2px_12px_rgba(40,13,95,0.06)]">
          {isCex && (
            <>
              <ExchangeTabs
                exchanges={cexExchanges}
                selected={selectedExchange}
                onSelect={(id) => {
                  setSelectedExchange(id);
                  setSearchQuery("");
                  setFilter("all");
                }}
              />
              <div className="h-6 w-px bg-border/60" />
            </>
          )}
          <FilterChips selected={filter} onSelect={setFilter} />
          <div className="ml-auto">
            <CoinSearchInput value={searchQuery} onChange={setSearchQuery} />
          </div>
        </div>

        {/* Coin table */}
        <CoinTable coins={filteredCoins} baseCurrency={exchange.baseCurrency} />

        {/* Footer info */}
        <p className="mt-3 text-[11px] text-muted-foreground/60">
          * 시세 데이터는 모의투자용이며 실제 시세와 다를 수 있습니다.
        </p>
      </main>
    </div>
  );
}
