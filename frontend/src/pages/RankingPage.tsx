import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { AlertCircle, BarChart3, Trophy, Users } from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
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

function RankingErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center gap-4 rounded-2xl bg-card px-4 py-16 text-center shadow-card">
      <AlertCircle className="h-10 w-10 text-destructive/60" />
      <div>
        <p className="text-base font-bold">랭킹을 불러오지 못했습니다</p>
        <p className="mt-1.5 text-sm text-muted-foreground">
          일시적인 문제일 수 있습니다. 잠시 후 다시 시도해주세요.
        </p>
      </div>
      <Button variant="outline" onClick={onRetry}>
        다시 시도
      </Button>
    </div>
  );
}

function RankingEmptyState() {
  const navigate = useNavigate();

  return (
    <>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        {[1, 2, 3].map((rank) => (
          <div
            key={rank}
            className="rounded-2xl border border-dashed border-border/60 bg-card/40 px-4 py-4"
          >
            <div className="mb-2 flex items-center gap-2">
              <span className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-secondary/40 text-xs font-bold text-muted-foreground">
                {rank}
              </span>
              <span className="text-sm font-semibold text-muted-foreground">—</span>
            </div>
            <p className="font-mono text-lg font-extrabold tabular-nums text-muted-foreground">--%</p>
            <p className="mt-1 text-xs text-muted-foreground">집계 대기</p>
          </div>
        ))}
      </div>

      <div className="flex flex-col items-center gap-4 rounded-2xl bg-card px-4 py-12 text-center shadow-card">
        <Trophy className="h-10 w-10 text-primary/40" />
        <div>
          <p className="text-base font-bold">아직 집계된 랭킹이 없습니다</p>
          <p className="mt-1.5 text-sm leading-relaxed text-muted-foreground">
            랭킹은 매일 밤 자정에 집계됩니다.
            <br />
            지금 거래를 시작하면 내일 첫 순위에 오릅니다.
          </p>
        </div>
        <Button variant="outline" onClick={() => navigate("/market")}>
          거래하러 가기
        </Button>
      </div>
    </>
  );
}

function StatsPlaceholder() {
  return (
    <div className="space-y-3 opacity-60">
      <div className="flex items-center gap-2">
        <Users className="h-4 w-4 text-muted-foreground" />
        <span className="text-sm text-muted-foreground">참여자 0명</span>
      </div>
      <div className="flex items-center justify-between text-sm">
        <span className="text-muted-foreground">최고 수익률</span>
        <span className="font-mono font-bold text-muted-foreground">--</span>
      </div>
      <div className="flex items-center justify-between text-sm">
        <span className="text-muted-foreground">평균 수익률</span>
        <span className="font-mono font-bold text-muted-foreground">--</span>
      </div>
    </div>
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
  const [hasError, setHasError] = useState(false);
  const [loadMoreError, setLoadMoreError] = useState("");
  const [retryCount, setRetryCount] = useState(0);
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
      setHasError(false);
      setLoadMoreError("");
      setExpandedUserId(null);
      setPortfolioByUser({});
      setPortfolioLoadingByUser({});
      setPortfolioErrorByUser({});

      const [rankingPage, rankingStats, my] = await Promise.allSettled([
        getRankings({ period, size: 20 }),
        getRankingStats(period),
        user ? getMyRanking(user.userId, period) : Promise.resolve(null),
      ]);

      if (canceled) return;

      if (rankingPage.status === "rejected") {
        console.error(rankingPage.reason);
        setEntries([]);
        setStats(null);
        setMyRanking(null);
        setHasNext(false);
        setNextCursor(null);
        setHasError(true);
        setIsLoading(false);
        return;
      }

      setEntries(rankingPage.value.content);
      setHasNext(rankingPage.value.hasNext);
      setNextCursor(rankingPage.value.nextCursor);
      setStats(rankingStats.status === "fulfilled" ? rankingStats.value : null);
      setMyRanking(my.status === "fulfilled" ? my.value : null);
      setIsLoading(false);
    }

    void loadInitialData();

    return () => {
      canceled = true;
    };
  }, [period, user, retryCount]);

  async function handleLoadMore() {
    if (!hasNext || nextCursor == null || isLoadingMore) return;

    setIsLoadingMore(true);
    setLoadMoreError("");
    try {
      const page = await getRankings({ period, cursorRank: nextCursor, size: 20 });
      setEntries((prev) => [...prev, ...page.content]);
      setHasNext(page.hasNext);
      setNextCursor(page.nextCursor);
    } catch (fetchError) {
      console.error(fetchError);
      setLoadMoreError("추가 랭킹을 불러오지 못했습니다.");
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

    if (nextExpanded) {
      void loadPortfolio(entry.userId);
    }
  }

  const topThree = entries.slice(0, 3);
  const restEntries = entries.slice(3);
  const isEmpty = entries.length === 0;

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <section className="animate-enter border-b border-border/40 pb-6 pt-8">
        <div className="mx-auto max-w-6xl px-4">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <div className="mb-1 flex items-center gap-2.5">
                <Trophy className="h-6 w-6 text-primary" />
                <h1 className="font-display text-3xl tracking-tight">랭킹</h1>
              </div>
              <p className="mt-2 text-sm text-muted-foreground">
                {periodLabel} 수익률 기준 순위
              </p>
            </div>

            <div className="flex gap-1.5 rounded-lg border border-border bg-card p-1">
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
        {isLoading && (
          <div className="rounded-2xl bg-card px-4 py-6 text-sm text-muted-foreground shadow-card">
            랭킹 데이터를 불러오는 중입니다...
          </div>
        )}

        {!isLoading && hasError && (
          <RankingErrorState onRetry={() => setRetryCount((prev) => prev + 1)} />
        )}

        {!isLoading && !hasError && (
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
                        <p className="text-[11px] text-muted-foreground">
                          {myRanking.tradeCount}회 거래
                        </p>
                      </div>
                    </div>
                    <p className="mt-3 rounded-xl bg-secondary/50 px-3 py-2.5 text-center font-mono text-lg font-extrabold tabular-nums">
                      {formatProfitRate(myRanking.profitRate)}
                    </p>
                  </div>
                ) : (
                  <p className="text-sm leading-relaxed text-muted-foreground">
                    아직 순위가 없습니다.
                    <br />
                    거래를 시작하면 다음 집계에 반영됩니다.
                  </p>
                )}
              </div>

              <div className="rounded-2xl bg-card p-4 shadow-card">
                <p className="mb-3 text-xs font-semibold text-muted-foreground">
                  {periodLabel} 통계
                </p>
                {isEmpty ? (
                  <StatsPlaceholder />
                ) : stats ? (
                  <div className="space-y-3">
                    <div className="flex items-center gap-2">
                      <Users className="h-4 w-4 text-primary" />
                      <span className="text-sm">
                        참여자 {stats.totalParticipants.toLocaleString("ko-KR")}명
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-muted-foreground">최고 수익률</span>
                      <span className="font-mono font-bold text-positive">
                        {formatProfitRate(stats.maxProfitRate)}
                      </span>
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
              {isEmpty && <RankingEmptyState />}

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

              {restEntries.map((entry) => {
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
                      <span className={cn(
                        "font-mono text-sm font-bold tabular-nums",
                        entry.profitRate >= 0 ? "text-positive" : "text-negative",
                      )}>
                        {formatProfitRate(entry.profitRate)}
                      </span>
                    </button>

                    {isExpanded && (
                      <div className="border-t border-border/50 px-4 py-3">
                        {loadingPortfolio && (
                          <p className="text-xs text-muted-foreground">포트폴리오를 불러오는 중입니다...</p>
                        )}

                        {portfolioError && (
                          <p className="text-xs text-destructive">{portfolioError}</p>
                        )}

                        {portfolio && (
                          <div className="space-y-2">
                            {portfolio.holdings.length === 0 && (
                              <p className="text-xs text-muted-foreground">보유 자산 정보가 없습니다.</p>
                            )}

                            {portfolio.holdings.map((holding) => {
                              const ratio = asRatio(holding.assetRatio);
                              return (
                                <div key={`${entry.userId}-${holding.coinSymbol}`} className="flex items-center gap-3">
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

              {loadMoreError && (
                <p className="text-xs font-medium text-destructive">{loadMoreError}</p>
              )}
            </section>
          </div>
        )}
      </main>
    </div>
  );
}
