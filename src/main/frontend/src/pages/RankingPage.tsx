import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { BarChart3, Lock, Trophy, Users } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { CoinIcon } from "@/components/market/CoinIcon";
import { useAuth } from "@/contexts/AuthContext";
import {
  getMyRanking,
  getRankerPortfolio,
  getRankings,
  getRankingStats,
  type MyRanking,
  type RankerPortfolio,
  type RankingItem,
  type RankingStats,
} from "@/lib/api/ranking-api";
import { cn } from "@/lib/utils";
import type { RankingPeriod } from "@/lib/types/ranking";

const PERIOD_TABS: { key: RankingPeriod; label: string }[] = [
  { key: "daily", label: "일간" },
  { key: "weekly", label: "주간" },
  { key: "monthly", label: "월간" },
];

function asRatio(value: number): number {
  return value > 1 ? value / 100 : value;
}

function formatProfitRate(value: number): string {
  return `${value >= 0 ? "+" : ""}${value.toFixed(2)}%`;
}

function RankBadge({ rank }: { rank: number }) {
  return (
    <span className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-secondary/60 text-xs font-bold text-foreground">
      {rank}
    </span>
  );
}

export function RankingPage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const periodParam = searchParams.get("period");
  const period: RankingPeriod =
    periodParam === "weekly" || periodParam === "monthly" ? periodParam : "daily";

  const [entries, setEntries] = useState<RankingItem[]>([]);
  const [myRanking, setMyRanking] = useState<MyRanking | null>(null);
  const [stats, setStats] = useState<RankingStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [hasNext, setHasNext] = useState(false);
  const [nextCursor, setNextCursor] = useState<number | null>(null);

  const [expandedUserId, setExpandedUserId] = useState<number | null>(null);
  const [portfolioByUser, setPortfolioByUser] = useState<Record<number, RankerPortfolio>>({});
  const [portfolioLoadingByUser, setPortfolioLoadingByUser] = useState<Record<number, boolean>>({});
  const [portfolioErrorByUser, setPortfolioErrorByUser] = useState<Record<number, string>>({});

  const periodLabel = useMemo(
    () => PERIOD_TABS.find((tab) => tab.key === period)?.label ?? "",
    [period],
  );

  useEffect(() => {
    let canceled = false;

    async function loadInitialData() {
      setIsLoading(true);
      setError("");
      setExpandedUserId(null);
      setPortfolioByUser({});
      setPortfolioLoadingByUser({});
      setPortfolioErrorByUser({});

      try {
        const [rankingPage, rankingStats, my] = await Promise.all([
          getRankings({ period, size: 20 }),
          getRankingStats(period),
          user ? getMyRanking(user.userId, period) : Promise.resolve(null),
        ]);

        if (canceled) return;

        setEntries(rankingPage.content);
        setHasNext(rankingPage.hasNext);
        setNextCursor(rankingPage.nextCursor);
        setStats(rankingStats);
        setMyRanking(my);
      } catch (fetchError) {
        if (canceled) return;

        console.error(fetchError);
        setEntries([]);
        setStats(null);
        setMyRanking(null);
        setHasNext(false);
        setNextCursor(null);
        setError("랭킹 데이터를 불러오지 못했습니다.");
      } finally {
        if (!canceled) {
          setIsLoading(false);
        }
      }
    }

    void loadInitialData();

    return () => {
      canceled = true;
    };
  }, [period, user]);

  async function handleLoadMore() {
    if (!hasNext || nextCursor == null || isLoadingMore) return;

    setIsLoadingMore(true);
    try {
      const page = await getRankings({ period, cursorRank: nextCursor, size: 20 });
      setEntries((prev) => [...prev, ...page.content]);
      setHasNext(page.hasNext);
      setNextCursor(page.nextCursor);
    } catch (fetchError) {
      console.error(fetchError);
      setError("추가 랭킹을 불러오지 못했습니다.");
    } finally {
      setIsLoadingMore(false);
    }
  }

  async function loadPortfolio(userId: number) {
    if (portfolioByUser[userId] || portfolioLoadingByUser[userId]) {
      return;
    }

    setPortfolioLoadingByUser((prev) => ({ ...prev, [userId]: true }));
    setPortfolioErrorByUser((prev) => ({ ...prev, [userId]: "" }));

    try {
      const portfolio = await getRankerPortfolio(userId, period);
      setPortfolioByUser((prev) => ({ ...prev, [userId]: portfolio }));
    } catch (fetchError) {
      console.error(fetchError);
      setPortfolioErrorByUser((prev) => ({
        ...prev,
        [userId]: "포트폴리오를 불러오지 못했습니다.",
      }));
    } finally {
      setPortfolioLoadingByUser((prev) => ({ ...prev, [userId]: false }));
    }
  }

  function handleToggleRow(entry: RankingItem) {
    const nextExpanded = expandedUserId === entry.userId ? null : entry.userId;
    setExpandedUserId(nextExpanded);

    if (nextExpanded && entry.portfolioPublic) {
      void loadPortfolio(entry.userId);
    }
  }

  const topThree = entries.slice(0, 3);
  const restEntries = entries.slice(3);

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <section className="bg-gradient-to-r from-primary/8 via-chart-4/6 to-primary/4 pb-8 pt-8">
        <div className="mx-auto max-w-6xl px-4">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="mb-1 flex items-center gap-2.5">
                <Trophy className="h-7 w-7 text-primary" />
                <h1 className="text-3xl font-extrabold tracking-tight">랭킹</h1>
              </div>
              <p className="mt-1.5 text-sm font-medium text-muted-foreground">
                {periodLabel} 수익률 기준 순위
              </p>
            </div>

            <div className="flex gap-1.5 rounded-xl bg-white/60 p-1 backdrop-blur-sm">
              {PERIOD_TABS.map((tab) => (
                <button
                  key={tab.key}
                  onClick={() => setSearchParams({ period: tab.key })}
                  className={cn(
                    "rounded-lg px-4 py-1.5 text-sm font-semibold transition-all",
                    period === tab.key
                      ? "bg-primary text-primary-foreground shadow-sm"
                      : "text-muted-foreground hover:text-foreground",
                  )}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      </section>

      <main className="mx-auto max-w-6xl px-4 pb-8 pt-6">
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-[280px_1fr]">
          <aside className="flex flex-col gap-4 lg:sticky lg:top-24 lg:self-start">
            <div className="rounded-2xl bg-card p-4 shadow-card">
              <p className="mb-3 text-xs font-semibold text-muted-foreground">내 랭킹</p>
              {myRanking ? (
                <div>
                  <div className="flex items-center gap-3">
                    <RankBadge rank={myRanking.rank} />
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-bold">{myRanking.nickname}</p>
                      <p className="text-[11px] text-muted-foreground">{myRanking.tradeCount}회 거래</p>
                    </div>
                  </div>
                  <p className="mt-3 rounded-xl bg-secondary/50 px-3 py-2.5 text-center font-mono text-lg font-extrabold tabular-nums">
                    {formatProfitRate(myRanking.profitRate)}
                  </p>
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">내 랭킹 데이터가 없습니다.</p>
              )}
            </div>

            <div className="rounded-2xl bg-card p-4 shadow-card">
              <p className="mb-3 text-xs font-semibold text-muted-foreground">{periodLabel} 통계</p>
              {stats ? (
                <div className="space-y-3">
                  <div className="flex items-center gap-2">
                    <Users className="h-4 w-4 text-primary" />
                    <span className="text-sm">참여자 {stats.totalParticipants.toLocaleString("ko-KR")}명</span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">최고 수익률</span>
                    <span className="font-mono font-bold text-positive">{formatProfitRate(stats.maxProfitRate)}</span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">평균 수익률</span>
                    <span className={cn(
                      "font-mono font-bold",
                      stats.avgProfitRate >= 0 ? "text-positive" : "text-negative",
                    )}>
                      {formatProfitRate(stats.avgProfitRate)}
                    </span>
                  </div>
                </div>
              ) : (
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <BarChart3 className="h-4 w-4" />
                  통계를 불러오지 못했습니다.
                </div>
              )}
            </div>
          </aside>

          <section className="space-y-3">
            {isLoading ? (
              <div className="rounded-2xl bg-card px-4 py-6 text-sm text-muted-foreground shadow-card">
                랭킹 데이터를 불러오는 중입니다...
              </div>
            ) : (
              <>
                {topThree.length > 0 && (
                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                    {topThree.map((entry) => (
                      <button
                        key={entry.userId}
                        onClick={() => handleToggleRow(entry)}
                        className={cn(
                          "rounded-2xl bg-card px-4 py-4 text-left shadow-card transition hover:shadow-card-hover",
                          expandedUserId === entry.userId && "ring-1 ring-primary/40",
                        )}
                      >
                        <div className="mb-2 flex items-center gap-2">
                          <RankBadge rank={entry.rank} />
                          <span className="truncate text-sm font-semibold">{entry.nickname}</span>
                        </div>
                        <p className={cn(
                          "font-mono text-lg font-extrabold tabular-nums",
                          entry.profitRate >= 0 ? "text-positive" : "text-negative",
                        )}>
                          {formatProfitRate(entry.profitRate)}
                        </p>
                        <p className="mt-1 text-xs text-muted-foreground">{entry.tradeCount}회 거래</p>
                      </button>
                    ))}
                  </div>
                )}

                {entries.length === 0 && (
                  <div className="rounded-2xl bg-card px-4 py-6 text-sm text-muted-foreground shadow-card">
                    랭킹 데이터가 없습니다.
                  </div>
                )}

                {[...restEntries].map((entry) => {
                  const isExpanded = expandedUserId === entry.userId;
                  const portfolio = portfolioByUser[entry.userId];
                  const loadingPortfolio = portfolioLoadingByUser[entry.userId] ?? false;
                  const portfolioError = portfolioErrorByUser[entry.userId] ?? "";

                  return (
                    <div key={entry.userId} className="overflow-hidden rounded-2xl bg-card shadow-card">
                      <button
                        onClick={() => handleToggleRow(entry)}
                        className="flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-primary/[0.03]"
                      >
                        <RankBadge rank={entry.rank} />
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-semibold">{entry.nickname}</p>
                          <p className="text-[11px] text-muted-foreground">{entry.tradeCount}회 거래</p>
                        </div>
                        {!entry.portfolioPublic && <Lock className="h-4 w-4 text-muted-foreground" />}
                        <span className={cn(
                          "font-mono text-sm font-bold tabular-nums",
                          entry.profitRate >= 0 ? "text-positive" : "text-negative",
                        )}>
                          {formatProfitRate(entry.profitRate)}
                        </span>
                      </button>

                      {isExpanded && (
                        <div className="border-t border-border/50 px-4 py-3">
                          {!entry.portfolioPublic && (
                            <p className="text-xs text-muted-foreground">비공개 포트폴리오입니다.</p>
                          )}

                          {entry.portfolioPublic && loadingPortfolio && (
                            <p className="text-xs text-muted-foreground">포트폴리오를 불러오는 중입니다...</p>
                          )}

                          {entry.portfolioPublic && portfolioError && (
                            <p className="text-xs text-destructive">{portfolioError}</p>
                          )}

                          {entry.portfolioPublic && portfolio && (
                            <div className="space-y-2">
                              {portfolio.holdings.length === 0 && (
                                <p className="text-xs text-muted-foreground">보유 자산 정보가 없습니다.</p>
                              )}

                              {portfolio.holdings.map((holding) => {
                                const ratio = asRatio(holding.assetRatio);
                                return (
                                  <div key={`${entry.userId}-${holding.coinSymbol}`} className="flex items-center gap-3">
                                    <CoinIcon symbol={holding.coinSymbol} size={24} />
                                    <div className="min-w-0 flex-1">
                                      <div className="mb-1 flex items-center justify-between gap-2">
                                        <span className="text-xs font-semibold">{holding.coinSymbol}</span>
                                        <span className="font-mono text-xs font-semibold">
                                          {(ratio * 100).toFixed(1)}%
                                        </span>
                                      </div>
                                      <div className="h-1.5 w-full overflow-hidden rounded-full bg-secondary">
                                        <div
                                          className="h-full rounded-full bg-primary"
                                          style={{ width: `${Math.max(0, Math.min(100, ratio * 100))}%` }}
                                        />
                                      </div>
                                    </div>
                                  </div>
                                );
                              })}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}

                {hasNext && (
                  <button
                    onClick={handleLoadMore}
                    disabled={isLoadingMore}
                    className="mt-2 w-full rounded-2xl border border-border/60 bg-white px-4 py-3 text-sm font-semibold text-muted-foreground transition hover:text-foreground disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {isLoadingMore ? "불러오는 중..." : "랭킹 더보기"}
                  </button>
                )}

                {error && <p className="text-xs font-medium text-destructive">{error}</p>}
              </>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}

