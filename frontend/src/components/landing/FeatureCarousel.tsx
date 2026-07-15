import type { ReactNode } from "react";
import { cn } from "@/lib/utils";
import { CoinIcon } from "./CoinIcon";

/** 팬텀식 가로 스와이프 카드. 아이콘 대신 실제 화면을 본뜬 미니 목업을 넣는다. */

/* ── 직접 그린 거래소/런치패드 브랜드 마크 ── */

/** 바이낸스: 다이아몬드 클러스터 */
function BinanceMark() {
  const diamond = (cx: number, cy: number, r: number) =>
    `M${cx} ${cy - r}L${cx + r} ${cy}L${cx} ${cy + r}L${cx - r} ${cy}Z`;
  return (
    <svg viewBox="0 0 32 32" className="h-6 w-6" aria-hidden="true">
      <g fill="#F0B90B">
        <path d={diamond(16, 9, 3.4)} />
        <path d={diamond(9, 16, 3.4)} />
        <path d={diamond(23, 16, 3.4)} />
        <path d={diamond(16, 23, 3.4)} />
        <path d={diamond(16, 16, 3.4)} />
      </g>
    </svg>
  );
}

/** 업비트: 위로 향하는 겹 셰브론(up) */
function UpbitMark() {
  return (
    <svg
      viewBox="0 0 32 32"
      className="h-6 w-6"
      fill="none"
      stroke="#4DA3FF"
      strokeWidth="3.2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M8 17 L16 9 L24 17" />
      <path d="M8 24 L16 16 L24 24" />
    </svg>
  );
}

/** 빗썸: 주황 b + 뒤쪽 붉은 그림자 (흰 타일용) */
function BithumbMark() {
  return (
    <svg viewBox="0 0 32 32" className="h-6 w-6" aria-hidden="true">
      <rect x="8.5" y="12.5" width="5.5" height="9" rx="2.4" fill="#D8331C" />
      <circle cx="18.5" cy="19.5" r="8.3" fill="#F7941E" />
      <circle cx="18.5" cy="19.5" r="3.5" fill="#fff" />
      <rect x="10.6" y="5" width="5" height="16" rx="2.5" fill="#F7941E" />
    </svg>
  );
}

/** 아발란체: 흰 삼각형 A 마크 */
function AvalancheMark() {
  return (
    <svg viewBox="0 0 32 32" className="h-6 w-6" aria-hidden="true">
      <path
        d="M16 7 L25.5 24 L6.5 24 Z"
        fill="#fff"
        strokeLinejoin="round"
        stroke="#fff"
        strokeWidth="1.6"
      />
      <path d="M18.6 15.4 L22.4 22 L15 22 Z" fill="#E84142" />
    </svg>
  );
}

/** pump.fun: 기울어진 두 톤 알약 캡슐 */
function PumpMark() {
  return (
    <svg viewBox="0 0 32 32" className="h-6 w-6" aria-hidden="true">
      <g transform="rotate(-38 16 16)">
        <rect x="10" y="6" width="12" height="20" rx="6" fill="#fff" />
        <rect x="10" y="6" width="12" height="10" rx="6" fill="#8CF0C0" />
      </g>
    </svg>
  );
}

/** 브랜드 색 앱 타일 (살짝 기울여 둥둥 뜬 느낌) */
function AppTile({
  bg,
  rotate,
  className,
  children,
}: {
  bg: string;
  rotate: number;
  className?: string;
  children: ReactNode;
}) {
  return (
    <span
      className={cn(
        "absolute flex h-11 w-11 items-center justify-center rounded-[13px] shadow-[0_10px_24px_-8px_rgba(0,0,0,0.6)]",
        className,
      )}
      style={{ backgroundColor: bg, transform: `rotate(${rotate}deg)` }}
    >
      {children}
    </span>
  );
}

/** 떠다니는 코인 로고 */
function FloatCoin({
  symbol,
  size,
  className,
}: {
  symbol: string;
  size: number;
  className?: string;
}) {
  return (
    <span className={cn("absolute drop-shadow-[0_8px_16px_rgba(0,0,0,0.55)]", className)}>
      <CoinIcon symbol={symbol} size={size} className="ring-2 ring-white/10" />
    </span>
  );
}

/** 거래소 카드: 거래소 로고 타일 + 대표 코인이 둥둥 떠 있고, 체결/송금 토스트 */
function ExchangesMock() {
  return (
    <div className="relative mt-auto h-[320px] w-full">
      <div className="absolute left-1/2 top-0 -translate-x-1/2 rounded-full bg-white px-3 py-1.5 text-[11px] font-bold text-[#17172A] shadow-lg">
        송금 완료 · ₩50,000
      </div>

      <AppTile bg="#1E2329" rotate={-8} className="left-0 top-[44px] border border-white/15">
        <BinanceMark />
      </AppTile>
      <AppTile bg="#093687" rotate={8} className="right-0 top-[36px]">
        <UpbitMark />
      </AppTile>
      <AppTile bg="#FFFFFF" rotate={5} className="right-0 top-[118px]">
        <BithumbMark />
      </AppTile>
      <AppTile bg="#E84142" rotate={6} className="left-5 top-[206px]">
        <AvalancheMark />
      </AppTile>
      <AppTile bg="#12B76A" rotate={-6} className="bottom-[26px] right-1">
        <PumpMark />
      </AppTile>

      {/* 가운데 채우는 활동 칩 */}
      <div className="absolute left-0 top-[152px] -rotate-2 rounded-full bg-white px-3 py-1.5 text-[11px] font-bold text-[#17172A] shadow-lg">
        매수 완료 · 0.5 SOL
      </div>
      <div className="absolute left-1/2 top-[196px] -translate-x-1/2 rotate-2 rounded-full bg-positive px-2.5 py-1.5 text-[11px] font-black text-white shadow-lg">
        +12.4%
      </div>

      <FloatCoin symbol="BTC" size={46} className="left-1/2 top-[92px] -translate-x-1/2" />
      <FloatCoin symbol="ETH" size={34} className="bottom-0 right-20" />
      <FloatCoin symbol="SOL" size={34} className="bottom-0 left-16" />
    </div>
  );
}

/** 투자 복기 카드: 규칙 지킨 나와의 격차(=놓친 수익)를 음영으로 부각한 미니 차트 */
function RegretMock() {
  return (
    <div className="my-auto rounded-2xl bg-white p-4 shadow-[0_12px_30px_-14px_rgba(42,35,80,0.3)]">
      <p className="text-[12px] font-bold text-[#2A2350]">규칙 지킨 나와 비교</p>

      <div className="relative mt-3">
        <svg
          viewBox="0 0 300 130"
          className="h-auto w-full"
          role="img"
          aria-label="규칙 지킨 나와 실제 나의 자산 추이 비교, 그 격차가 놓친 수익"
        >
          <defs>
            <linearGradient id="regret-gap" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#12A56A" stopOpacity="0.22" />
              <stop offset="100%" stopColor="#12A56A" stopOpacity="0.02" />
            </linearGradient>
          </defs>

          {[36, 72, 108].map((y) => (
            <line key={y} x1="6" y1={y} x2="294" y2={y} stroke="#F0EEF4" strokeWidth="1" />
          ))}

          {/* 두 선 사이 격차 = 놓친 수익 */}
          <path
            d="M12,86 C34,86 34,74 56,74 C80,74 80,64 104,64 C128,64 128,72 152,72 C176,72 176,54 200,54 C224,54 224,66 248,66 C268,66 268,44 288,44 L288,90 C268,90 268,108 248,108 C224,108 224,86 200,86 C176,86 176,106 152,106 C128,106 128,80 104,80 C80,80 80,100 56,100 C34,100 34,90 12,90 Z"
            fill="url(#regret-gap)"
          />

          {/* 격차 표시 */}
          <line x1="276" y1="52" x2="276" y2="92" stroke="#12A56A" strokeWidth="1.5" strokeDasharray="3 3" />
          <line x1="272" y1="52" x2="280" y2="52" stroke="#12A56A" strokeWidth="1.5" />
          <line x1="272" y1="92" x2="280" y2="92" stroke="#12A56A" strokeWidth="1.5" />

          {/* BTC만 홀드했을 때 (벤치마크) */}
          <path
            d="M12,78 C34,78 34,62 56,62 C80,62 80,50 104,50 C128,50 128,58 152,58 C176,58 176,40 200,40 C224,40 224,50 248,50 C268,50 268,32 288,32"
            fill="none"
            stroke="#F0A030"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />

          {/* 규칙 지킨 나 (시뮬레이션) */}
          <path
            d="M12,86 C34,86 34,74 56,74 C80,74 80,64 104,64 C128,64 128,72 152,72 C176,72 176,54 200,54 C224,54 224,66 248,66 C268,66 268,44 288,44"
            fill="none"
            stroke="#12A56A"
            strokeWidth="2.5"
            strokeDasharray="6 5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />

          {/* 실제 나 */}
          <path
            d="M12,90 C34,90 34,100 56,100 C80,100 80,80 104,80 C128,80 128,106 152,106 C176,106 176,86 200,86 C224,86 224,108 248,108 C268,108 268,90 288,90"
            fill="none"
            stroke="#6C5CE7"
            strokeWidth="2.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />

          {/* 끝점 */}
          <circle cx="288" cy="44" r="3.5" fill="#12A56A" stroke="#fff" strokeWidth="1.8" />
          <circle cx="288" cy="90" r="3.5" fill="#6C5CE7" stroke="#fff" strokeWidth="1.8" />

          {/* 위반 지점 */}
          {[
            [56, 100],
            [152, 106],
            [248, 108],
          ].map(([cx, cy]) => (
            <circle key={cx} cx={cx} cy={cy} r="4" fill="#E85D75" stroke="#fff" strokeWidth="1.8" />
          ))}
        </svg>

        {/* 격차를 가리키는 배지 */}
        <div className="absolute right-0 top-0 rounded-xl bg-[#2A2350] px-2.5 py-1.5 text-right shadow-lg">
          <p className="text-[9px] font-medium text-white/60">놓친 수익</p>
          <p className="font-mono text-[15px] font-extrabold leading-none text-[#34D399]">+33만원</p>
        </div>
      </div>

      <div className="mt-2.5 flex flex-wrap gap-x-3 gap-y-1 text-[10px] font-semibold text-[#8A85A0]">
        <span className="flex items-center gap-1">
          <span className="h-0.5 w-3 rounded bg-[#6C5CE7]" />실제 나
        </span>
        <span className="flex items-center gap-1">
          <span className="h-0.5 w-3 rounded bg-[#12A56A]" />규칙 지킨 나
        </span>
        <span className="flex items-center gap-1">
          <span className="h-0.5 w-3 rounded bg-[#F0A030]" />BTC 홀드
        </span>
        <span className="flex items-center gap-1">
          <span className="h-1.5 w-1.5 rounded-full bg-[#E85D75]" />위반 지점
        </span>
      </div>
    </div>
  );
}

/** 랭킹 카드: 상위 투자자 리더보드, 내 1위를 강조 */
const RANK_ROWS: {
  rank: number;
  profit: string;
  av: string;
  medal: string;
  medalText: string;
}[] = [
  { rank: 2, profit: "+96%", av: "#EC5B8C", medal: "#C0C6CF", medalText: "#fff" },
  { rank: 3, profit: "+74%", av: "#5B9BE8", medal: "#D9A066", medalText: "#fff" },
  { rank: 4, profit: "+51%", av: "#F0A030", medal: "#E1EFE7", medalText: "#12312A" },
  { rank: 5, profit: "+38%", av: "#3FBF7F", medal: "#E1EFE7", medalText: "#12312A" },
];

function RankMock() {
  return (
    <div className="mt-4">
      {/* 내 순위 1위 강조 */}
      <div className="flex items-center gap-2.5 rounded-2xl bg-primary px-3 py-3.5 text-white shadow-[0_12px_26px_-10px_rgba(108,92,231,0.7)]">
        <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-[#F5B301] text-[11px] font-black text-white">
          1
        </span>
        <span className="h-8 w-8 shrink-0 rounded-full bg-[#4A39B0] ring-2 ring-white/40" />
        <span className="text-[13px] font-extrabold">나</span>
        <span className="ml-auto font-mono text-[15px] font-extrabold tabular-nums">+128%</span>
      </div>

      {/* 2~5위 */}
      <div className="mt-2.5 space-y-2.5">
        {RANK_ROWS.map((row) => (
          <div key={row.rank} className="flex items-center gap-2.5 rounded-xl bg-white px-3 py-3">
            <span
              className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[11px] font-bold"
              style={{ backgroundColor: row.medal, color: row.medalText }}
            >
              {row.rank}
            </span>
            <span
              className="h-8 w-8 shrink-0 rounded-full"
              style={{ backgroundColor: row.av }}
            />
            <span className="ml-auto font-mono text-[13px] font-bold tabular-nums text-[#12A56A]">
              {row.profit}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

const CARDS: {
  title: string;
  subtitle?: string;
  bg: string;
  text: string;
  visual: ReactNode;
}[] = [
  {
    title: "복잡한 절차 없이,\n여러 거래소를 한 번에.",
    bg: "bg-[#17172A]",
    text: "text-white",
    visual: <ExchangesMock />,
  },
  {
    title: "원칙을 어긴 거래를 기록해,\n놓친 수익을 숫자로 보여줍니다.",
    bg: "bg-[#E9E5FB]",
    text: "text-[#2A2350]",
    visual: <RegretMock />,
  },
  {
    title: "누가 제일 잘하나\n수익률로 가립니다.",
    bg: "bg-[#E4F4EC]",
    text: "text-[#12312A]",
    visual: <RankMock />,
  },
];

export function FeatureCarousel() {
  return (
    <div className="no-scrollbar -mx-4 flex snap-x snap-mandatory gap-4 overflow-x-auto px-4 pb-2 sm:mx-0 sm:px-0">
      {CARDS.map((card) => (
        <article
          key={card.title}
          className={cn(
            "flex h-[440px] w-[80vw] max-w-[320px] shrink-0 snap-start flex-col rounded-3xl p-6 sm:h-[480px] sm:w-[340px] sm:max-w-none sm:p-7",
            card.bg,
            card.text,
          )}
        >
          <h3 className="whitespace-pre-line break-keep text-[26px] font-bold leading-[1.2] tracking-tight">
            {card.title}
          </h3>
          {card.subtitle && (
            <p className="mt-2.5 max-w-[250px] text-[13px] leading-snug opacity-70">
              {card.subtitle}
            </p>
          )}
          {card.visual}
        </article>
      ))}
    </div>
  );
}
