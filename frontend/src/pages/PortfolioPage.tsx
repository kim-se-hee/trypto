import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { ExchangeTabs } from "@/components/market/ExchangeTabs";
import { AssetSummaryCard } from "@/components/portfolio/AssetSummaryCard";
import { DonutChart } from "@/components/portfolio/DonutChart";
import { HoldingsTable } from "@/components/portfolio/HoldingsTable";
import { NoRoundNotice } from "@/components/round/NoRoundNotice";
import { useAuth } from "@/contexts/AuthContext";
import { useRound } from "@/contexts/RoundContext";
import { EXCHANGES } from "@/lib/types/coins";
import type { HoldingData, PortfolioSummary } from "@/lib/types/portfolio";
import { getMyHoldings, type MyHoldingsResponse } from "@/lib/api/portfolio-api";

function toPortfolioSummary(
  exchangeId: number,
  data: MyHoldingsResponse,
): PortfolioSummary {
  const exchange = EXCHANGES.find((e) => e.id === exchangeId);
  return {
    exchangeId: exchange?.key ?? String(exchangeId),
    exchangeName: exchange?.name ?? String(exchangeId),
    baseCurrency: data.baseCurrencySymbol,
    availableCash: Number(data.baseCurrencyBalance),
    holdings: data.holdings.map((h): HoldingData => ({
      coinSymbol: h.coinSymbol,
      coinName: h.coinName,
      quantity: Number(h.quantity),
      avgBuyPrice: Number(h.avgBuyPrice),
      currentPrice: Number(h.currentPrice),
    })),
  };
}

export function PortfolioPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { user } = useAuth();
  const { activeRound } = useRound();

  const exchangeTabItems = useMemo(() => {
    if (!activeRound) return [];
    return activeRound.wallets.map((w) => {
      const exchange = EXCHANGES.find((e) => e.id === w.exchangeId);
      return {
        id: exchange?.key ?? String(w.exchangeId),
        name: exchange?.name ?? String(w.exchangeId),
        baseCurrency: exchange?.baseCurrency ?? "",
      };
    });
  }, [activeRound]);

  const defaultExchange = exchangeTabItems[0]?.id ?? "upbit";
  const selectedExchange = searchParams.get("exchange") ?? defaultExchange;

  // 부제는 지금 보고 있는 거래소 지갑을 설명하는 문장이다. 라운드가 없으면 설명할 지갑이 없고,
  // 라운드가 있으면 포트폴리오를 다 불러오기 전에도 고른 탭을 곧바로 따라가야 한다.
  const selectedExchangeItem = exchangeTabItems.find((e) => e.id === selectedExchange);

  const [portfolio, setPortfolio] = useState<PortfolioSummary | null>(null);
  const [loading, setLoading] = useState(false);

  const loadPortfolio = useCallback(async () => {
    if (!user || !activeRound) return;

    const exchange = EXCHANGES.find((e) => e.key === selectedExchange);
    if (!exchange) return;

    const wallet = activeRound.wallets.find((w) => w.exchangeId === exchange.id);
    if (!wallet) return;

    setLoading(true);
    try {
      const data = await getMyHoldings(wallet.walletId);
      setPortfolio(toPortfolioSummary(exchange.id, data));
    } catch (error) {
      console.error("Failed to load portfolio", error);
      setPortfolio(null);
    } finally {
      setLoading(false);
    }
  }, [user, activeRound, selectedExchange]);

  useEffect(() => {
    void loadPortfolio();
  }, [loadPortfolio]);

  return (
    <div className="min-h-screen bg-background">
      <Header />

      {/* Page header */}
      <section className="animate-enter border-b border-border/40 pb-6 pt-8">
        <div className="mx-auto max-w-6xl px-4">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h1 className="font-display text-3xl tracking-tight">투자내역</h1>
              {selectedExchangeItem && (
                <p className="mt-2 text-sm text-muted-foreground">
                  {selectedExchangeItem.name} 기준 · {selectedExchangeItem.baseCurrency} 마켓
                </p>
              )}
            </div>
            <ExchangeTabs
              exchanges={exchangeTabItems}
              selected={selectedExchange}
              onSelect={(id) => setSearchParams({ exchange: id })}
            />
          </div>
        </div>
      </section>

      <main className="mx-auto max-w-6xl px-4 py-6">
        {loading ? (
          <p className="text-sm text-muted-foreground">로딩 중...</p>
        ) : portfolio ? (
          <>
            {/* 2-column layout */}
            <div className="grid grid-cols-1 gap-6 lg:grid-cols-[340px_1fr]">
              {/* Left column — summary + chart */}
              <div className="flex flex-col gap-6">
                <AssetSummaryCard
                  availableCash={portfolio.availableCash}
                  holdings={portfolio.holdings}
                  baseCurrency={portfolio.baseCurrency}
                />
                <DonutChart
                  holdings={portfolio.holdings}
                  baseCurrency={portfolio.baseCurrency}
                />
              </div>

              {/* Right column — holdings table */}
              <HoldingsTable
                holdings={portfolio.holdings}
                baseCurrency={portfolio.baseCurrency}
              />
            </div>

            {/* Footer info */}
            <p className="mt-3 text-[11px] text-muted-foreground/60">
              * 모의투자 데이터입니다.
            </p>
          </>
        ) : activeRound ? (
          <p className="text-sm text-muted-foreground">
            포트폴리오 데이터를 불러올 수 없습니다.
          </p>
        ) : (
          <NoRoundNotice description="진행 중인 라운드가 없어 포트폴리오가 비어 있습니다." />
        )}
      </main>
    </div>
  );
}
