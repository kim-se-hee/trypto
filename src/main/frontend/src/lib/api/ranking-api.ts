import type { RankingPeriod } from "@/lib/types/ranking";
import { apiGet } from "./client";
import { toBackendRankingPeriod } from "./mappers";
import type { CursorPageResponseDto } from "./types";

export interface RankingItem {
  rank: number;
  userId: number;
  nickname: string;
  profitRate: number;
  tradeCount: number;
  portfolioPublic: boolean;
}

export interface MyRanking {
  rank: number;
  nickname: string;
  profitRate: number;
  tradeCount: number;
}

export interface RankingStats {
  totalParticipants: number;
  maxProfitRate: number;
  avgProfitRate: number;
}

export interface RankerHolding {
  coinSymbol: string;
  exchangeName: string;
  assetRatio: number;
  profitRate: number;
}

export interface RankerPortfolio {
  userId: number;
  nickname: string;
  rank: number;
  profitRate: number;
  holdings: RankerHolding[];
}

export interface GetRankingsParams {
  period: RankingPeriod;
  referenceDate?: string;
  cursorRank?: number;
  size?: number;
}

export async function getRankings(
  params: GetRankingsParams,
): Promise<CursorPageResponseDto<RankingItem>> {
  const data = await apiGet<CursorPageResponseDto<RankingItem>>("/api/rankings", {
    period: toBackendRankingPeriod(params.period),
    referenceDate: params.referenceDate,
    cursorRank: params.cursorRank,
    size: params.size,
  });

  return {
    content: data.content.map((item) => ({
      ...item,
      profitRate: Number(item.profitRate),
    })),
    nextCursor: data.nextCursor,
    hasNext: data.hasNext,
  };
}

export async function getMyRanking(
  userId: number,
  period: RankingPeriod,
): Promise<MyRanking | null> {
  const data = await apiGet<MyRanking | null>("/api/rankings/me", {
    userId,
    period: toBackendRankingPeriod(period),
  });

  if (!data) return null;

  return {
    ...data,
    profitRate: Number(data.profitRate),
  };
}

export async function getRankingStats(period: RankingPeriod): Promise<RankingStats> {
  const data = await apiGet<RankingStats>("/api/rankings/stats", {
    period: toBackendRankingPeriod(period),
  });

  return {
    ...data,
    maxProfitRate: Number(data.maxProfitRate),
    avgProfitRate: Number(data.avgProfitRate),
  };
}

export async function getRankerPortfolio(
  userId: number,
  period: RankingPeriod,
): Promise<RankerPortfolio> {
  const data = await apiGet<RankerPortfolio>(`/api/rankings/${userId}/portfolio`, {
    period: toBackendRankingPeriod(period),
  });

  return {
    ...data,
    profitRate: Number(data.profitRate),
    holdings: data.holdings.map((holding) => ({
      ...holding,
      assetRatio: Number(holding.assetRatio),
      profitRate: Number(holding.profitRate),
    })),
  };
}

