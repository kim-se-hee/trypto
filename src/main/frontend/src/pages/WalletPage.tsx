import { useState, useMemo, useEffect, useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { ExchangeTabs } from "@/components/market/ExchangeTabs";
import { WalletSummary } from "@/components/wallet/WalletSummary";
import { WalletAssetTable } from "@/components/wallet/WalletAssetTable";
import { WalletAssetDetail } from "@/components/wallet/WalletAssetDetail";
import { TransferModal } from "@/components/wallet/TransferModal";
import { DepositModal } from "@/components/wallet/DepositModal";
import { walletData } from "@/mocks/wallet";
import type { WalletCoinBalance } from "@/mocks/wallet";

const exchangeTabItems = walletData.map((w) => ({
  id: w.exchangeId,
  name: w.exchangeName,
  baseCurrency: w.baseCurrency,
}));

export function WalletPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedExchange = searchParams.get("exchange") ?? walletData[0].exchangeId;
  const [selectedCoin, setSelectedCoin] = useState<WalletCoinBalance | null>(null);

  const wallet = useMemo(
    () => walletData.find((w) => w.exchangeId === selectedExchange) ?? walletData[0],
    [selectedExchange],
  );

  const [transferCoin, setTransferCoin] = useState<WalletCoinBalance | null>(null);
  const [depositCoin, setDepositCoin] = useState<WalletCoinBalance | null>(null);

  const handleExchangeChange = (exchangeId: string) => {
    setSearchParams({ exchange: exchangeId });
    setSelectedCoin(null);
  };

  const handleDeposit = useCallback((coin: WalletCoinBalance) => {
    setDepositCoin(coin);
  }, []);

  const handleTransfer = useCallback((coin: WalletCoinBalance) => {
    setTransferCoin(coin);
  }, []);

  // 모바일 바텀시트 열릴 때 body 스크롤 방지
  useEffect(() => {
    if (!selectedCoin) return;

    const mq = window.matchMedia("(min-width: 1024px)");
    if (mq.matches) return;

    document.body.style.overflow = "hidden";
    return () => { document.body.style.overflow = ""; };
  }, [selectedCoin]);

  return (
    <div className="min-h-screen bg-background">
      <Header />

      {/* Hero section */}
      <section className="bg-gradient-to-r from-primary/8 via-chart-2/6 to-primary/4 pb-8 pt-8">
        <div className="mx-auto max-w-6xl px-4">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h1 className="text-3xl font-extrabold tracking-tight">입출금</h1>
              <p className="mt-1.5 text-sm font-medium text-muted-foreground">
                자산 관리 &middot; 입금, 거래소 간 송금
              </p>
            </div>
            <ExchangeTabs
              exchanges={exchangeTabItems}
              selected={selectedExchange}
              onSelect={handleExchangeChange}
            />
          </div>
        </div>
      </section>

      <main className="mx-auto max-w-6xl px-4 py-6">
        {/* Balance summary */}
        <WalletSummary
          balances={wallet.balances}
          baseCurrency={wallet.baseCurrency}
          exchangeName={wallet.exchangeName}
        />

        {/* Asset table + detail panel */}
        <div className={
          selectedCoin
            ? "mt-6 grid grid-cols-1 gap-6 lg:grid-cols-[1fr_340px]"
            : "mt-6"
        }>
          <WalletAssetTable
            balances={wallet.balances}
            baseCurrency={wallet.baseCurrency}
            onSelectCoin={setSelectedCoin}
            selectedCoin={selectedCoin?.coinSymbol ?? null}
            onDeposit={handleDeposit}
            onTransfer={handleTransfer}
          />

          {/* Desktop: side panel */}
          {selectedCoin && (
            <div className="hidden lg:block">
              <div className="sticky top-24">
                <WalletAssetDetail
                  coin={selectedCoin}
                  baseCurrency={wallet.baseCurrency}
                  onClose={() => setSelectedCoin(null)}
                  onDeposit={handleDeposit}
                  onTransfer={handleTransfer}
                />
              </div>
            </div>
          )}
        </div>

        <p className="mt-3 text-[11px] text-muted-foreground/60">
          * 모의투자 데이터입니다. 실제 자산이 아닙니다.
        </p>
      </main>

      {/* Mobile: bottom sheet overlay */}
      {selectedCoin && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <div
            className="absolute inset-0 bg-foreground/30 backdrop-blur-sm"
            onClick={() => setSelectedCoin(null)}
          />
          <div className="absolute inset-x-0 bottom-0 max-h-[85vh] overflow-y-auto rounded-t-2xl bg-card shadow-card-active animate-in slide-in-from-bottom duration-300">
            <div className="mx-auto my-2 h-1 w-10 rounded-full bg-border" />
            <WalletAssetDetail
              coin={selectedCoin}
              baseCurrency={wallet.baseCurrency}
              onClose={() => setSelectedCoin(null)}
              onDeposit={handleDeposit}
              onTransfer={handleTransfer}
            />
          </div>
        </div>
      )}

      {/* Transfer Modal */}
      {transferCoin && (
        <TransferModal
          isOpen
          onClose={() => setTransferCoin(null)}
          coin={transferCoin}
          exchangeId={wallet.exchangeId}
          baseCurrency={wallet.baseCurrency}
        />
      )}

      {/* Deposit Modal */}
      {depositCoin && (
        <DepositModal
          isOpen
          onClose={() => setDepositCoin(null)}
          coin={depositCoin}
          exchangeId={wallet.exchangeId}
          baseCurrency={wallet.baseCurrency}
        />
      )}
    </div>
  );
}
