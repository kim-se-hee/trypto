import { useState, useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import { Header } from "@/components/layout/Header";
import { Trophy, ChevronDown, Lock, TrendingUp, Users, BarChart3 } from "lucide-react";
import { CoinIcon } from "@/components/market/CoinIcon";
import { rankingData, MY_USER_ID } from "@/mocks/ranking";
import type { RankingPeriod, RankingEntry } from "@/mocks/ranking";
import { cn } from "@/lib/utils";

const PERIOD_TABS: { key: RankingPeriod; label: string }[] = [
  { key: "daily", label: "일간" },
  { key: "weekly", label: "주간" },
  { key: "monthly", label: "월간" },
];

const MEDAL_COLORS: Record<number, { bg: string; text: string; ring: string }> = {
  1: { bg: "bg-amber-400", text: "text-amber-900", ring: "ring-amber-300" },
  2: { bg: "bg-gray-300", text: "text-gray-700", ring: "ring-gray-200" },
  3: { bg: "bg-amber-600", text: "text-amber-100", ring: "ring-amber-500" },
};

function RankBadge({ rank, size = "sm" }: { rank: number; size?: "sm" | "lg" }) {
  const medal = MEDAL_COLORS[rank];
  const sizeClasses = size === "lg" ? "h-10 w-10 text-sm" : "h-7 w-7 text-xs";
  if (medal) {
    return (
      <span
        className={cn(
          "inline-flex items-center justify-center rounded-full font-extrabold ring-2",
          sizeClasses,
          medal.bg,
          medal.text,
          medal.ring,
        )}
      >
        {rank}
      </span>
    );
  }
  return (
    <span
      className={cn(
        "inline-flex items-center justify-center rounded-full font-bold text-muted-foreground",
        sizeClasses,
      )}
    >
      {rank}
    </span>
  );
}

function CoinStack({ coins, max = 3 }: { coins: { coinSymbol: string }[]; max?: number }) {
  const display = coins.slice(0, max);
  return (
    <div className="flex items-center">
      {display.map((coin, i) => (
        <div
          key={coin.coinSymbol}
          className={cn("relative rounded-full ring-2 ring-card", i > 0 && "-ml-1.5")}
          style={{ zIndex: max - i }}
        >
          <CoinIcon symbol={coin.coinSymbol} size={20} />
        </div>
      ))}
    </div>
  );
}

function PortfolioPanel({ entry }: { entry: RankingEntry }) {
  if (!entry.portfolioPublic) {
    return (
      <div className="flex items-center gap-2 px-4 py-5 text-xs text-muted-foreground">
        <Lock className="h-3.5 w-3.5" />
        비공개 포트폴리오입니다.
      </div>
    );
  }

  return (
    <div className="px-4 pb-4 pt-2">
      <p className="mb-2.5 text-[11px] font-medium text-muted-foreground">보유 코인 비중</p>
      <div className="space-y-2">
        {entry.portfolio.map((item) => {
          const pct = item.ratio * 100;
          return (
            <div key={item.coinSymbol} className="flex items-center gap-3">
              <CoinIcon symbol={item.coinSymbol} size={24} />
              <div className="min-w-0 flex-1">
                <div className="mb-1 flex items-center justify-between">
                  <span className="text-xs font-semibold">{item.coinSymbol}</span>
                  <span className="font-mono text-xs font-semibold tabular-nums">
                    {pct.toFixed(1)}%
                  </span>
                </div>
                <div className="h-1.5 w-full overflow-hidden rounded-full bg-secondary">
                  <div
                    className="h-full rounded-full bg-primary transition-all duration-500"
                    style={{ width: `${pct}%` }}
                  />
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function RankingRow({
  entry,
  isExpanded,
  onToggle,
}: {
  entry: RankingEntry;
  isExpanded: boolean;
  onToggle: () => void;
}) {
  const isPositive = entry.profitRate >= 0;
  const topCoins = entry.portfolioPublic ? entry.portfolio.slice(0, 3) : [];

  return (
    <div
      className={cn(
        "overflow-hidden rounded-2xl bg-card transition-shadow",
        isExpanded ? "shadow-card-active" : "shadow-card hover:shadow-card-hover",
      )}
    >
      <button
        onClick={onToggle}
        className="flex w-full items-center gap-4 px-4 py-3.5 text-left transition-colors hover:bg-primary/[0.03]"
      >
        <RankBadge rank={entry.rank} />

        <div className="min-w-0 flex-1">
          <span className="text-sm font-semibold">{entry.nickname}</span>
          <span className="ml-2 text-[11px] text-muted-foreground">
            {entry.tradeCount}회 거래
          </span>
        </div>

        {topCoins.length > 0 && (
          <div className="hidden sm:block">
            <CoinStack coins={topCoins} />
          </div>
        )}

        <span
          className={cn(
            "font-mono text-sm font-bold tabular-nums",
            isPositive ? "text-positive" : "text-negative",
          )}
        >
          {isPositive ? "+" : ""}
          {entry.profitRate.toFixed(2)}%
        </span>

        <ChevronDown
          className={cn(
            "h-4 w-4 shrink-0 text-muted-foreground/50 transition-transform duration-200",
            isExpanded && "rotate-180",
          )}
        />
      </button>

      <div
        className={cn(
          "grid transition-[grid-template-rows] duration-300 ease-in-out",
          isExpanded ? "grid-rows-[1fr]" : "grid-rows-[0fr]",
        )}
      >
        <div className="overflow-hidden">
          {isExpanded && (
            <div className="border-t border-border/60">
              <PortfolioPanel entry={entry} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

const TOP3_ACCENTS: Record<number, { border: string; glow: string }> = {
  1: { border: "border-amber-400", glow: "shadow-[0_0_0_1px_rgba(251,191,36,0.15),0_4px_24px_rgba(251,191,36,0.10)]" },
  2: { border: "border-gray-300", glow: "shadow-card" },
  3: { border: "border-amber-700/40", glow: "shadow-card" },
};

function Podium({
  entries,
  expandedId,
  onToggle,
}: {
  entries: RankingEntry[];
  expandedId: number | null;
  onToggle: (userId: number) => void;
}) {
  const top3 = entries.slice(0, 3);
  if (top3.length < 3) return null;

  return (
    <div className="mx-auto max-w-6xl px-4 pb-2 pt-8">
      <div className="grid grid-cols-3 items-end gap-3 sm:gap-4">
        {[top3[1], top3[0], top3[2]].map((entry) => {
          const isPositive = entry.profitRate >= 0;
          const topCoins = entry.portfolioPublic ? entry.portfolio.slice(0, 3) : [];
          const accent = TOP3_ACCENTS[entry.rank];
          const isFirst = entry.rank === 1;
          const isExpanded = expandedId === entry.userId;

          return (
            <div
              key={entry.userId}
              className={cn(
                "overflow-hidden rounded-2xl border-t-[3px] bg-card transition-shadow",
                accent.border,
                isExpanded ? "shadow-card-active" : accent.glow,
              )}
            >
              <button
                onClick={() => onToggle(entry.userId)}
                className={cn(
                  "flex w-full flex-col items-center px-3 pb-4 pt-5 transition-colors hover:bg-primary/[0.03] sm:px-5",
                  isFirst && "sm:pb-6 sm:pt-7",
                )}
              >
                <RankBadge rank={entry.rank} size="lg" />
                <span
                  className={cn(
                    "mt-2.5 max-w-full truncate font-bold",
                    isFirst ? "text-base" : "text-sm",
                  )}
                >
                  {entry.nickname}
                </span>
                <span
                  className={cn(
                    "mt-1 font-mono font-bold tabular-nums",
                    isFirst ? "text-base" : "text-sm",
                    isPositive ? "text-positive" : "text-negative",
                  )}
                >
                  {isPositive ? "+" : ""}
                  {entry.profitRate.toFixed(2)}%
                </span>
                <div className="mt-2.5 flex items-center gap-2">
                  {topCoins.length > 0 && <CoinStack coins={topCoins} />}
                  <span className="text-[11px] text-muted-foreground">
                    {entry.tradeCount}회 거래
                  </span>
                </div>
                <ChevronDown
                  className={cn(
                    "mt-2 h-4 w-4 text-muted-foreground/40 transition-transform duration-200",
                    isExpanded && "rotate-180",
                  )}
                />
              </button>

              <div
                className={cn(
                  "grid transition-[grid-template-rows] duration-300 ease-in-out",
                  isExpanded ? "grid-rows-[1fr]" : "grid-rows-[0fr]",
                )}
              >
                <div className="overflow-hidden">
                  {isExpanded && (
                    <div className="border-t border-border/60">
                      <PortfolioPanel entry={entry} />
                    </div>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function MyRankCard({
  entry,
  periodLabel,
}: {
  entry: RankingEntry | undefined;
  periodLabel: string;
}) {
  if (!entry) return null;
  const isPositive = entry.profitRate >= 0;

  return (
    <div className="rounded-2xl bg-card p-4 shadow-card">
      <p className="mb-3 text-xs font-semibold text-muted-foreground">내 {periodLabel} 랭킹</p>
      <div className="flex items-center gap-3">
        <RankBadge rank={entry.rank} size="lg" />
        <div className="min-w-0 flex-1">
          <p className="text-sm font-bold">{entry.nickname}</p>
          <p className="text-[11px] text-muted-foreground">{entry.tradeCount}회 거래</p>
        </div>
      </div>
      <div className="mt-3 rounded-xl bg-secondary/50 px-3 py-2.5 text-center">
        <p className="text-[11px] text-muted-foreground">수익률</p>
        <p
          className={cn(
            "mt-0.5 font-mono text-lg font-extrabold tabular-nums",
            isPositive ? "text-positive" : "text-negative",
          )}
        >
          {isPositive ? "+" : ""}
          {entry.profitRate.toFixed(2)}%
        </p>
      </div>
    </div>
  );
}

function PeriodStats({
  entries,
  periodLabel,
}: {
  entries: RankingEntry[];
  periodLabel: string;
}) {
  const stats = useMemo(() => {
    const avgProfit = entries.reduce((sum, e) => sum + e.profitRate, 0) / entries.length;
    const positiveCount = entries.filter((e) => e.profitRate >= 0).length;
    return {
      totalParticipants: entries.length,
      avgProfit,
      maxProfit: entries[0]?.profitRate ?? 0,
      positiveRate: Math.round((positiveCount / entries.length) * 100),
    };
  }, [entries]);

  const items = [
    {
      icon: Users,
      label: "참여자",
      value: `${stats.totalParticipants}명`,
    },
    {
      icon: TrendingUp,
      label: "최고 수익률",
      value: `+${stats.maxProfit.toFixed(2)}%`,
      color: "text-positive" as const,
    },
    {
      icon: BarChart3,
      label: "평균 수익률",
      value: `${stats.avgProfit >= 0 ? "+" : ""}${stats.avgProfit.toFixed(2)}%`,
      color: (stats.avgProfit >= 0 ? "text-positive" : "text-negative") as string,
    },
  ];

  return (
    <div className="rounded-2xl bg-card p-4 shadow-card">
      <p className="mb-3 text-xs font-semibold text-muted-foreground">{periodLabel} 통계</p>
      <div className="space-y-3">
        {items.map((item) => (
          <div key={item.label} className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/8">
              <item.icon className="h-4 w-4 text-primary" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-[11px] text-muted-foreground">{item.label}</p>
              <p className={cn("text-sm font-bold tabular-nums", item.color)}>{item.value}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function RankingPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const period = (searchParams.get("period") ?? "daily") as RankingPeriod;
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const entries = useMemo(() => rankingData[period] ?? rankingData.daily, [period]);
  const myEntry = useMemo(() => entries.find((e) => e.userId === MY_USER_ID), [entries]);
  const restEntries = useMemo(() => entries.slice(3), [entries]);

  const handleToggle = (userId: number) => {
    setExpandedId((prev) => (prev === userId ? null : userId));
  };

  const periodLabel = PERIOD_TABS.find((t) => t.key === period)?.label ?? "";

  return (
    <div className="min-h-screen bg-background">
      <Header />

      {/* Hero */}
      <section className="bg-gradient-to-r from-primary/8 via-chart-4/6 to-primary/4 pb-8 pt-8">
        <div className="mx-auto max-w-6xl px-4">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="mb-1 flex items-center gap-2.5">
                <Trophy className="h-7 w-7 text-primary" />
                <h1 className="text-3xl font-extrabold tracking-tight">랭킹</h1>
              </div>
              <p className="mt-1.5 text-sm font-medium text-muted-foreground">
                {periodLabel} 수익률 기준 · 상위 100명
              </p>
            </div>

            <div className="flex gap-1.5 rounded-xl bg-white/60 p-1 backdrop-blur-sm">
              {PERIOD_TABS.map((tab) => (
                <button
                  key={tab.key}
                  onClick={() => {
                    setSearchParams({ period: tab.key });
                    setExpandedId(null);
                  }}
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

      {/* TOP 3 Podium */}
      <Podium entries={entries} expandedId={expandedId} onToggle={handleToggle} />

      {/* Main: sidebar + ranking list */}
      <main className="mx-auto max-w-6xl px-4 pb-8">
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-[280px_1fr]">
          <aside className="flex flex-col gap-4 lg:sticky lg:top-24 lg:self-start">
            <MyRankCard entry={myEntry} periodLabel={periodLabel} />
            <PeriodStats entries={entries} periodLabel={periodLabel} />
          </aside>

          <div>
            <div className="space-y-2">
              {restEntries.map((entry) => (
                <RankingRow
                  key={entry.userId}
                  entry={entry}
                  isExpanded={expandedId === entry.userId}
                  onToggle={() => handleToggle(entry.userId)}
                />
              ))}
            </div>

            <p className="mt-3 text-[11px] text-muted-foreground/60">
              * 모의투자 데이터입니다. 배치 집계 기준.
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
