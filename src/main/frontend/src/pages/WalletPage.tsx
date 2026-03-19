import { useState, useMemo, useEffect, useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { ExchangeTabs } from "@/components/market/ExchangeTabs";
import { WalletSummary } from "@/components/wallet/WalletSummary";
import { WalletAssetTable } from "@/components/wallet/WalletAssetTable";
import { WalletAssetDetail } from "@/components/wallet/WalletAssetDetail";
import { TransferModal, type TransferDestination } from "@/components/wallet/TransferModal";
import { DepositModal } from "@/components/wallet/DepositModal";
import { TransferHistoryPanel } from "@/components/wallet/TransferHistoryPanel";
import { useAuth } from "@/contexts/AuthContext";
import { useRound } from "@/contexts/RoundContext";
import { EXCHANGES } from "@/lib/types/coins";
import type { WalletCoinBalance, WalletData, TransferRecord } from "@/lib/types/wallet";
import { getWalletBalances } from "@/lib/api/wallet-api";
import { getExchangeCoins, type ExchangeCoinResponse } from "@/lib/api/exchange-api";
import { getTransferHistory, type TransferHistoryItem } from "@/lib/api/transfer-api";

function mapTransferItem(item: TransferHistoryItem, currentExchangeName: string): TransferRecord {
  const isWithdraw = item.type === "WITHDRAW";
  return {
    id: String(item.transferId),
    exchangeId: "",
    type: item.type,
    asset: item.coinSymbol,
    amount: Number(item.amount),
    fromExchangeName: isWithdraw ? currentExchangeName : "",
    toExchangeName: isWithdraw ? "" : currentExchangeName,
    status: item.status as TransferRecord["status"],
    requestedAt: item.createdAt,
    completedAt: item.completedAt ?? undefined,
  };
}

export function WalletPage() {
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

  const [selectedCoin, setSelectedCoin] = useState<WalletCoinBalance | null>(null);
  const [transferCoin, setTransferCoin] = useState<WalletCoinBalance | null>(null);
  const [depositCoin, setDepositCoin] = useState<WalletCoinBalance | null>(null);

  const [wallet, setWallet] = useState<WalletData | null>(null);
  const [transfers, setTransfers] = useState<TransferRecord[]>([]);
  const [walletList, setWalletList] = useState<WalletData[]>([]);
  const [loading, setLoading] = useState(false);

  const loadWalletData = useCallback(async () => {
    if (!user || !activeRound) return;

    const exchange = EXCHANGES.find((e) => e.key === selectedExchange);
    if (!exchange) return;

    const walletEntry = activeRound.wallets.find((w) => w.exchangeId === exchange.id);
    if (!walletEntry) return;

    setLoading(true);
    try {
      const [balancesData, exchangeCoins, transferData] = await Promise.all([
        getWalletBalances(user.userId, walletEntry.walletId),
        getExchangeCoins(exchange.id),
        getTransferHistory(walletEntry.walletId, user.userId, { size: 50 }),
      ]);

      const coinMap = new Map<number, ExchangeCoinResponse>();
      for (const coin of exchangeCoins) {
        coinMap.set(coin.coinId, coin);
      }

      // 기본 화폐 잔고 + 코인 잔고
      const balances: WalletCoinBalance[] = [
        {
          coinSymbol: balancesData.baseCurrencySymbol,
          coinName: balancesData.baseCurrencySymbol === "KRW" ? "원화" : balancesData.baseCurrencySymbol,
          available: Number(balancesData.baseCurrencyAvailable),
          locked: Number(balancesData.baseCurrencyLocked),
          currentPrice: 1,
        },
        ...balancesData.balances.map((b) => {
          const coinInfo = coinMap.get(b.coinId);
          return {
            coinSymbol: coinInfo?.coinSymbol ?? String(b.coinId),
            coinName: coinInfo?.coinName ?? String(b.coinId),
            available: Number(b.available),
            locked: Number(b.locked),
            currentPrice: 0, // WebSocket에서 업데이트 예정
          };
        }),
      ];

      const walletData: WalletData = {
        exchangeId: exchange.key,
        exchangeName: exchange.name,
        baseCurrency: exchange.baseCurrency,
        balances,
      };

      setWallet(walletData);
      setWalletList((prev) => {
        const filtered = prev.filter((w) => w.exchangeId !== exchange.key);
        return [...filtered, walletData];
      });
      setTransfers(transferData.content.map((item) => mapTransferItem(item, exchange.name)));
    } catch (error) {
      console.error("Failed to load wallet data", error);
    } finally {
      setLoading(false);
    }
  }, [user, activeRound, selectedExchange]);

  useEffect(() => {
    void loadWalletData();
  }, [loadWalletData]);

  const transferDestinations = useMemo<TransferDestination[]>(() => {
    return exchangeTabItems
      .filter((e) => e.id !== selectedExchange)
      .map((e) => ({ exchangeId: e.id, exchangeName: e.name }));
  }, [exchangeTabItems, selectedExchange]);

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

      {/* Page header */}
      <section className="animate-enter border-b border-border/40 pb-6 pt-8">
        <div className="mx-auto max-w-6xl px-4">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h1 className="font-display text-3xl tracking-tight">입출금</h1>
              <p className="mt-2 text-sm text-muted-foreground">
                자산 관리 · 입금/출금 내역 확인
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
        {loading ? (
          <p className="text-sm text-muted-foreground">로딩 중...</p>
        ) : wallet ? (
          <>
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
                  <TransferHistoryPanel
                    exchangeId={wallet.exchangeId}
                    exchanges={walletList}
                    records={transfers}
                    assetFilter={selectedCoin.coinSymbol}
                  />
                </div>
              )}
            </div>

            <p className="mt-3 text-[11px] text-muted-foreground/60">
              * 모의투자 데이터입니다. 실제 자산이 아닙니다.
            </p>
          </>
        ) : (
          <p className="text-sm text-muted-foreground">
            {activeRound ? "지갑 데이터를 불러올 수 없습니다." : "진행 중인 라운드가 없습니다."}
          </p>
        )}
      </main>

      {/* Mobile: bottom sheet overlay */}
      {selectedCoin && wallet && (
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
            <div className="px-4 pb-6">
              <TransferHistoryPanel
                exchangeId={wallet.exchangeId}
                exchanges={walletList}
                records={transfers}
                assetFilter={selectedCoin.coinSymbol}
              />
            </div>
          </div>
        </div>
      )}

      {/* Transfer Modal */}
      {transferCoin && wallet && (
        <TransferModal
          isOpen
          onClose={() => setTransferCoin(null)}
          coin={transferCoin}
          baseCurrency={wallet.baseCurrency}
          destinations={transferDestinations}
        />
      )}

      {/* Deposit Modal */}
      {depositCoin && wallet && (
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
