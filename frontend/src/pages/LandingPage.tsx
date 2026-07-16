import { type ReactNode } from "react";
import { Link } from "react-router-dom";
import { cn } from "@/lib/utils";
import { useAuth } from "@/contexts/AuthContext";
import { useInView } from "@/hooks/useInView";
import { LandingNav } from "@/components/landing/LandingNav";
import { FeatureCarousel } from "@/components/landing/FeatureCarousel";
import { RegretShowcase } from "@/components/landing/RegretShowcase";

const ENTER_CLASS = [
  "animate-enter",
  "animate-enter-delay-1",
  "animate-enter-delay-2",
  "animate-enter-delay-3",
  "animate-enter-delay-4",
] as const;

/** 뷰포트에 들어올 때 한 번 떠오르는 래퍼 */
function Reveal({
  delay = 0,
  className,
  children,
}: {
  delay?: 0 | 1 | 2 | 3 | 4;
  className?: string;
  children: ReactNode;
}) {
  const { ref, inView } = useInView<HTMLDivElement>();
  return (
    <div ref={ref} className={cn(inView ? ENTER_CLASS[delay] : "opacity-0", className)}>
      {children}
    </div>
  );
}

const REGRET_STEPS = [
  { step: "1", text: "라운드를 시작하며 나만의 투자 원칙을 정합니다" },
  { step: "2", text: "추격 매수·물타기·과매매, 어길 때마다 자동으로 기록됩니다" },
  { step: "3", text: "그래프를 통해 원칙을 지킨 나와 실제 나를 겹쳐 봅니다" },
];

export function LandingPage() {
  const { isAuthenticated } = useAuth();

  const ctaTo = isAuthenticated ? "/market" : "/login";
  const ctaLabel = isAuthenticated ? "이어서 투자하기" : "지금 날려보기";

  return (
    <div id="top" className="min-h-dvh bg-background">
      <LandingNav />

      <main>
        {/* ── 히어로 ── */}
        <section className="relative">
          {/* 헤드라인 + 버튼: 팬텀처럼 위 중앙에 크게 */}
          <div className="mx-auto max-w-3xl px-4 pb-24 pt-32 text-center sm:px-6 lg:pt-40">
            <h1 className="animate-enter-delay-1 font-display break-keep text-[40px] leading-[1.1] tracking-tight sm:text-[68px]">
              <span className="text-primary">날려도 됩니다</span> 종이니까
            </h1>

            <p className="animate-enter-delay-2 mx-auto mt-6 max-w-md text-base leading-relaxed text-muted-foreground sm:text-lg">
              진짜 시세, 가짜 돈. 잃는 건 없습니다.
            </p>

            <div className="animate-enter-delay-3 mt-9 flex justify-center">
              <Link
                to={ctaTo}
                className="flex h-14 items-center rounded-full bg-primary px-8 text-base font-extrabold text-primary-foreground shadow-lg transition-all duration-150 hover:-translate-y-0.5 hover:brightness-110 hover:shadow-xl active:scale-[0.98]"
              >
                {ctaLabel}
              </Link>
            </div>
          </div>
        </section>

        {/* ── 핵심 기능 (가로 스와이프) ── */}
        <section id="features" className="mx-auto max-w-6xl scroll-mt-24 px-4 py-24 sm:px-6">
          <Reveal>
            <FeatureCarousel />
          </Reveal>
        </section>

        {/* ── 투자 복기 딥다이브 ── */}
        <section id="regret" className="scroll-mt-20 border-y border-border/60 bg-card/50">
          <div className="mx-auto grid max-w-6xl items-center gap-12 px-4 py-24 sm:px-6 lg:grid-cols-2 lg:gap-16">
            <Reveal>
              <h2 className="font-display text-3xl leading-[1.25] tracking-tight sm:text-4xl">
                실수를 데이터로 바꿉니다.
              </h2>
              <p className="mt-5 max-w-lg text-[15px] leading-relaxed text-muted-foreground">
                수익률만 보면 왜 잃었는지 모릅니다. trypto는 원칙을 어긴 거래를 전부 기록해서,
                어떤 습관이 얼마짜리였는지 그래프로 보여줍니다.
              </p>

              <ul className="mt-8 space-y-4">
                {REGRET_STEPS.map((item) => (
                  <li key={item.step} className="flex items-start gap-3.5">
                    <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-brand/10 text-[13px] font-extrabold text-brand">
                      {item.step}
                    </span>
                    <span className="pt-0.5 text-sm font-medium">{item.text}</span>
                  </li>
                ))}
              </ul>
            </Reveal>

            <Reveal delay={2}>
              <RegretShowcase />
            </Reveal>
          </div>
        </section>

        {/* ── 마지막 CTA ── */}
        <section className="mx-auto max-w-6xl px-4 py-24 sm:px-6">
          <Reveal>
            <div className="relative overflow-hidden rounded-[2rem] bg-primary px-6 py-16 text-center sm:px-12 sm:py-20">
              <h2 className="font-display text-3xl tracking-tight text-white sm:text-4xl">
                다음 불장은 연습이 아닙니다.
              </h2>
              <p className="mx-auto mt-4 max-w-md text-[15px] leading-relaxed text-white/80">
                그래서 연습장을 하나 만들었습니다.
              </p>
              <Link
                to={ctaTo}
                className="mt-8 inline-flex h-13 items-center rounded-full bg-white px-8 text-[15px] font-extrabold text-primary shadow-lg transition-all duration-150 hover:-translate-y-0.5 hover:bg-white/90 hover:shadow-xl active:scale-[0.98]"
              >
                미리 대비하기
              </Link>
            </div>
          </Reveal>
        </section>
      </main>
    </div>
  );
}
