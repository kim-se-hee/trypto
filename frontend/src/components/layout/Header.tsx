import { useState } from "react";
import { Menu, X, LogOut } from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { cn } from "@/lib/utils";
import { useAuth } from "@/contexts/AuthContext";
import { useRound } from "@/contexts/RoundContext";

const navItems = [
  { path: "/market", label: "마켓" },
  { path: "/portfolio", label: "포트폴리오" },
  { path: "/wallet", label: "입출금" },
  { path: "/ranking", label: "랭킹" },
  { path: "/regret", label: "투자 복기" },
];

export function Header() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { hasActiveRound, isRoundLoading } = useRound();
  const [mobileOpen, setMobileOpen] = useState(false);
  const showRoundStart = !isRoundLoading && !hasActiveRound;

  // 로그아웃 후에는 랜딩으로 보낸다. 순서가 중요하다 — 먼저 공개 라우트(/)로 옮긴 뒤 세션을 비운다.
  // 반대로 하면 user 가 비는 순간 보호 라우트가 /login 으로 리다이렉트해 이 이동을 덮어쓴다.
  const handleLogout = async () => {
    navigate("/", { replace: true });
    await logout();
  };

  return (
    <header className="sticky top-0 z-50 border-b border-border/60 bg-background/95 backdrop-blur-md">
      <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4">
        <Link to="/market" className="flex items-center gap-2">
          <img src="/favicon.png" alt="trypto" className="h-6 w-6 rounded-md" />
          <span className="text-lg font-extrabold tracking-tight">trypto</span>
        </Link>

        {/* Desktop nav */}
        <nav className="hidden items-center gap-1 text-sm sm:flex">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                className={cn(
                  "rounded-lg px-3 py-1.5 text-[13px] font-medium transition-colors",
                  isActive
                    ? "bg-foreground/[0.06] font-semibold text-foreground"
                    : "text-muted-foreground hover:bg-foreground/[0.04] hover:text-foreground",
                )}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        {/* Desktop user info */}
        <div className="hidden items-center gap-2 sm:flex">
          {showRoundStart && (
            <Link
              to="/round/new"
              className="rounded-lg bg-primary px-3 py-1.5 text-[13px] font-semibold text-primary-foreground transition-opacity hover:opacity-90"
            >
              라운드 시작
            </Link>
          )}
          {user && (
            <Link
              to="/mypage"
              className="text-[13px] font-medium text-muted-foreground transition-colors hover:text-foreground"
            >
              {user.nickname}
            </Link>
          )}
          <button
            onClick={() => void handleLogout()}
            className="flex items-center gap-1 rounded-lg px-2 py-1.5 text-[13px] text-muted-foreground transition-colors hover:bg-foreground/[0.04] hover:text-foreground"
          >
            <LogOut className="h-3.5 w-3.5" />
            <span>로그아웃</span>
          </button>
        </div>

        {/* Mobile hamburger */}
        <button
          className="rounded-lg p-2 text-muted-foreground transition-colors hover:bg-foreground/[0.04] sm:hidden"
          onClick={() => setMobileOpen((v) => !v)}
          aria-label={mobileOpen ? "메뉴 닫기" : "메뉴 열기"}
        >
          {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </div>

      {/* Mobile nav dropdown */}
      {mobileOpen && (
        <nav className="border-t border-border/40 bg-background px-4 pb-3 pt-2 sm:hidden">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setMobileOpen(false)}
                className={cn(
                  "block rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-foreground/[0.06] text-foreground"
                    : "text-muted-foreground hover:bg-foreground/[0.04] hover:text-foreground",
                )}
              >
                {item.label}
              </Link>
            );
          })}

          {showRoundStart && (
            <Link
              to="/round/new"
              onClick={() => setMobileOpen(false)}
              className="mt-1 block rounded-lg bg-primary px-3 py-2.5 text-center text-sm font-semibold text-primary-foreground"
            >
              라운드 시작
            </Link>
          )}

          <div className="mt-2 flex items-center justify-between border-t border-border/40 px-3 pt-3">
            {user && (
              <Link
                to="/mypage"
                onClick={() => setMobileOpen(false)}
                className="text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
              >
                {user.nickname}
              </Link>
            )}
            <button
              onClick={() => {
                setMobileOpen(false);
                void handleLogout();
              }}
              className="flex items-center gap-1 rounded-lg px-2 py-1.5 text-sm text-muted-foreground transition-colors hover:bg-foreground/[0.04] hover:text-foreground"
            >
              <LogOut className="h-4 w-4" />
              <span>로그아웃</span>
            </button>
          </div>
        </nav>
      )}
    </header>
  );
}
