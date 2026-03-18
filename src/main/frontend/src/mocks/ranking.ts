export type { RankingPeriod } from "@/lib/types/ranking";

import type { RankingPeriod } from "@/lib/types/ranking";

export interface PortfolioItem {
  coinSymbol: string;
  coinName: string;
  ratio: number; // 비중 (0~1)
}

export interface RankingEntry {
  rank: number;
  userId: number;
  nickname: string;
  profitRate: number; // 수익률 (%)
  tradeCount: number;
  portfolioPublic: boolean;
  portfolio: PortfolioItem[];
}

const NICKNAMES = [
  "코인킹", "비트사냥꾼", "고래잡이", "흑우탈출", "무빙파도",
  "존버왕", "차트해석가", "냉정한손", "수익맨", "떡상기원",
  "솔라나킹", "이더매니아", "리플러버", "알트연구소", "매매의신",
  "신중한손절", "월급루팡", "코린이탈출", "눈치백단", "고인물",
  "불타는심장", "빗썸도사", "바낸고수", "업비트왕", "차트마스터",
  "존버킹", "떡락방어", "풀매수맨", "현명한손", "비트고래",
  "알트코인헌터", "스윙의달인", "데이트레이더", "리스크관리사", "분산투자가",
  "김프지킴이", "급등포착", "가즈아맨", "손절칼손", "물타기장인",
  "투자의정석", "냉철한판단", "이익실현가", "코인분석가", "실전리허설",
  "차트독서가", "매매일지왕", "코인마스터", "디파이러버", "스테이킹맨",
  "트레이딩봇", "선물마스터", "현물고수", "매매원칙맨", "존버의힘",
  "급등서퍼", "차트플래너", "투자습관왕", "리밸런서", "분할매수맨",
  "김치프리미엄", "체인분석가", "온체인왕", "유동성헌터", "시드불리기",
  "복리의마법", "잔고달성왕", "리스크헌터", "알파찾기", "베타추종자",
  "모멘텀킹", "가치투자가", "기술적분석", "원칙준수자", "투자멘토",
  "차트아티스트", "매매의기술", "포지션마스터", "코인수집가", "블록체인팬",
  "디센트럴", "스마트머니", "공포탐욕맨", "역발상투자", "트렌드팔로워",
  "바닥잡이", "천장팔이", "중립유지자", "시장관찰자", "데이터분석가",
  "퀀트투자가", "그리드매매맨", "DCA실천가", "장기홀더", "수익률킹",
  "투자철학가", "리스크테이커", "안전자산맨", "포트폴리오장인", "자산배분왕",
];

const COINS_POOL: { symbol: string; name: string }[] = [
  { symbol: "BTC", name: "비트코인" },
  { symbol: "ETH", name: "이더리움" },
  { symbol: "XRP", name: "리플" },
  { symbol: "SOL", name: "솔라나" },
  { symbol: "DOGE", name: "도지코인" },
  { symbol: "ADA", name: "에이다" },
  { symbol: "AVAX", name: "아발란체" },
  { symbol: "LINK", name: "체인링크" },
  { symbol: "DOT", name: "폴카닷" },
  { symbol: "ATOM", name: "코스모스" },
  { symbol: "UNI", name: "유니스왑" },
  { symbol: "AAVE", name: "에이브" },
  { symbol: "ARB", name: "아비트럼" },
  { symbol: "MATIC", name: "폴리곤" },
];

function seededRandom(seed: number): () => number {
  let s = seed;
  return () => {
    s = (s * 16807 + 0) % 2147483647;
    return s / 2147483647;
  };
}

function generatePortfolio(rand: () => number): PortfolioItem[] {
  const count = Math.floor(rand() * 5) + 2;
  const shuffled = [...COINS_POOL].sort(() => rand() - 0.5).slice(0, count);

  const rawWeights = shuffled.map(() => rand() * 10 + 1);
  const totalWeight = rawWeights.reduce((s, w) => s + w, 0);
  const ratios = rawWeights.map((w) => w / totalWeight);

  return shuffled.map((coin, i) => ({
    coinSymbol: coin.symbol,
    coinName: coin.name,
    ratio: ratios[i],
  })).sort((a, b) => b.ratio - a.ratio);
}

function generateRanking(period: string): RankingEntry[] {
  const periodSeed = period === "daily" ? 1 : period === "weekly" ? 2 : 3;
  const rand = seededRandom(42 + periodSeed * 1000);

  const entries: RankingEntry[] = [];
  const shuffledNames = [...NICKNAMES].sort(() => rand() - 0.5);

  for (let i = 0; i < 100; i++) {
    const baseProfit = 48 - i * 0.82 + (rand() - 0.5) * 8;
    const profitRate = Math.round(baseProfit * 100) / 100;

    entries.push({
      rank: i + 1,
      userId: 1000 + i,
      nickname: shuffledNames[i],
      profitRate,
      tradeCount: Math.floor(rand() * 150) + 5,
      portfolioPublic: rand() > 0.15,
      portfolio: generatePortfolio(seededRandom(periodSeed * 10000 + i)),
    });
  }

  entries.sort((a, b) => {
    if (b.profitRate !== a.profitRate) return b.profitRate - a.profitRate;
    return a.tradeCount - b.tradeCount;
  });
  entries.forEach((e, i) => (e.rank = i + 1));

  return entries;
}

export const rankingData: Record<RankingPeriod, RankingEntry[]> = {
  daily: generateRanking("daily"),
  weekly: generateRanking("weekly"),
  monthly: generateRanking("monthly"),
};

export const MY_USER_ID = 1024;
