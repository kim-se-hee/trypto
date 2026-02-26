import { useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { ExchangeTabs } from "@/components/market/ExchangeTabs";
import { AssetSummaryCard } from "@/components/portfolio/AssetSummaryCard";
import { DonutChart } from "@/components/portfolio/DonutChart";
import { HoldingsTable } from "@/components/portfolio/HoldingsTable";
import { portfolioData } from "@/mocks/portfolio";

const exchangeTabItems = portfolioData.map((p) => ({
  id: p.exchangeId,
  name: p.exchangeName,
  baseCurrency: p.baseCurrency,
}));

export function PortfolioPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedExchange = searchParams.get("exchange") ?? portfolioData[0].exchangeId;

  const portfolio = useMemo(
    () => portfolioData.find((p) => p.exchangeId === selectedExchange) ?? portfolioData[0],
    [selectedExchange],
  );

  return (
    <div className="min-h-screen bg-background">
      <Header />

      {/* Hero section */}
      <section className="bg-gradient-to-r from-primary/8 via-chart-2/6 to-primary/4 pb-8 pt-8">
        <div className="mx-auto max-w-6xl px-4">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h1 className="text-3xl font-extrabold tracking-tight">투자내역</h1>
              <p className="mt-1.5 text-sm font-medium text-muted-foreground">
                {portfolio.exchangeName} 기준 &middot; {portfolio.baseCurrency} 마켓
              </p>
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
      </main>
    </div>
  );
}
