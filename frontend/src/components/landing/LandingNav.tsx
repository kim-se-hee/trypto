import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { cn } from "@/lib/utils";
import { useAuth } from "@/contexts/AuthContext";

const anchors = [
  { href: "#features", label: "기능" },
  { href: "#regret", label: "투자 복기" },
];

export function LandingNav() {
  const { isAuthenticated } = useAuth();
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header className="fixed inset-x-0 top-0 z-50 px-3 sm:px-6">
      <div
        className={cn(
          "mx-auto mt-3 flex h-14 items-center justify-between gap-4 rounded-full px-4 transition-all duration-300 sm:px-5",
          scrolled
            ? "max-w-3xl border border-border/60 bg-background/75 shadow-[0_10px_40px_-15px_rgba(26,26,46,0.28)] backdrop-blur-xl"
            : "max-w-6xl border border-transparent bg-transparent",
        )}
      >
        <a href="#top" className="flex items-center gap-2.5">
          <img src="/favicon.png" alt="trypto" className="h-8 w-8 rounded-xl" />
          <span className="text-xl font-extrabold tracking-tight">trypto</span>
        </a>

        <nav className="hidden items-center gap-1 sm:flex">
          {anchors.map((item) => (
            <a
              key={item.href}
              href={item.href}
              className="rounded-lg px-3 py-1.5 text-[13px] font-medium text-muted-foreground transition-colors hover:bg-foreground/[0.04] hover:text-foreground"
            >
              {item.label}
            </a>
          ))}
        </nav>

        <Link
          to={isAuthenticated ? "/market" : "/login"}
          className={cn(
            "rounded-full bg-primary px-4 py-2 text-[13px] font-bold text-primary-foreground shadow-md transition-all duration-300 hover:brightness-110 active:scale-[0.98]",
            scrolled
              ? "translate-x-0 opacity-100"
              : "pointer-events-none translate-x-2 opacity-0",
          )}
        >
          {isAuthenticated ? "마켓으로 가기" : "바로 시작하기"}
        </Link>
      </div>
    </header>
  );
}
