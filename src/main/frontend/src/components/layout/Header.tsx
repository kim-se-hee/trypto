import { useState } from "react";
import { Activity, Menu, X, LogOut } from "lucide-react";
import { Link, useLocation } from "react-router-dom";
import { cn } from "@/lib/utils";
import { useAuth } from "@/contexts/AuthContext";

const navItems = [
  { path: "/market", label: "마켓" },
  { path: "/portfolio", label: "포트폴리오" },
  { path: "/wallet", label: "입출금" },
  { path: "/ranking", label: "랭킹" },
  { path: "/regret", label: "투자 복기" },
];

export function Header() {
  const location = useLocation();
  const { user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 bg-white/90 shadow-sm backdrop-blur-xl">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <Link to="/market" className="flex items-center gap-2">
          <Activity className="h-5 w-5 text-primary" />
          <span className="text-lg font-bold tracking-tight">Trypto</span>
        </Link>

        {/* Desktop nav */}
        <nav className="hidden h-full items-center gap-6 text-sm sm:flex">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                className={cn(
                  "flex h-full items-center border-b-2 font-medium transition-colors",
                  isActive
                    ? "border-primary font-semibold text-primary"
                    : "border-transparent text-muted-foreground hover:text-primary",
                )}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        {/* Desktop user info */}
        <div className="hidden items-center gap-3 sm:flex">
          {user && (
            <Link
              to="/mypage"
              className="text-sm font-medium text-muted-foreground transition-colors hover:text-primary"
            >
              {user.nickname}
            </Link>
          )}
          <button
            onClick={logout}
            className="flex items-center gap-1 rounded-lg px-2 py-1.5 text-sm text-muted-foreground transition-colors hover:bg-secondary/60 hover:text-foreground"
          >
            <LogOut className="h-4 w-4" />
            <span>로그아웃</span>
          </button>
        </div>

        {/* Mobile hamburger */}
        <button
          className="sm:hidden rounded-lg p-2 text-muted-foreground transition-colors hover:bg-secondary/60"
          onClick={() => setMobileOpen((v) => !v)}
          aria-label={mobileOpen ? "메뉴 닫기" : "메뉴 열기"}
        >
          {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </div>

      {/* Mobile nav dropdown */}
      {mobileOpen && (
        <nav className="border-t border-border/30 bg-white px-4 pb-3 pt-2 sm:hidden">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setMobileOpen(false)}
                className={cn(
                  "block rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-primary/10 text-primary"
                    : "text-muted-foreground hover:bg-secondary/60 hover:text-foreground",
                )}
              >
                {item.label}
              </Link>
            );
          })}

          <div className="mt-2 flex items-center justify-between border-t border-border/30 px-3 pt-3">
            {user && (
              <Link
                to="/mypage"
                onClick={() => setMobileOpen(false)}
                className="text-sm font-medium text-muted-foreground transition-colors hover:text-primary"
              >
                {user.nickname}
              </Link>
            )}
            <button
              onClick={() => {
                setMobileOpen(false);
                logout();
              }}
              className="flex items-center gap-1 rounded-lg px-2 py-1.5 text-sm text-muted-foreground transition-colors hover:bg-secondary/60 hover:text-foreground"
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
