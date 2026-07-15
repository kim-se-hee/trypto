import { LogOut } from "lucide-react";
import { Link } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";

export function RoundCreateHeader() {
  const { user, logout } = useAuth();

  return (
    <header className="sticky top-0 z-50 bg-white/90 shadow-sm backdrop-blur-xl">
      <div className="mx-auto flex h-16 max-w-2xl items-center justify-between px-4">
        <Link to="/" className="flex items-center gap-2">
          <img src="/favicon.png" alt="trypto" className="h-6 w-6 rounded-md" />
          <span className="text-lg font-bold tracking-tight">trypto</span>
        </Link>

        <div className="flex items-center gap-3">
          {user && (
            <span className="text-sm font-medium text-muted-foreground">{user.nickname}</span>
          )}
          <button
            onClick={logout}
            className="flex items-center gap-1.5 rounded-lg px-2 py-1.5 text-sm text-muted-foreground transition-colors hover:bg-secondary/60 hover:text-foreground"
          >
            <LogOut className="h-4 w-4" />
            <span className="hidden sm:inline">로그아웃</span>
          </button>
        </div>
      </div>
    </header>
  );
}
