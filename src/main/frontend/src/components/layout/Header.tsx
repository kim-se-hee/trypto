import { Activity } from "lucide-react";

export function Header() {
  return (
    <header className="sticky top-0 z-50 bg-white/90 shadow-sm backdrop-blur-xl">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <div className="flex items-center gap-2">
          <Activity className="h-5 w-5 text-primary" />
          <span className="text-lg font-bold tracking-tight">
            Trypto
          </span>
        </div>
        <nav className="flex h-full items-center gap-6 text-sm">
          <a href="#" className="flex h-full items-center border-b-2 border-primary font-semibold text-primary">시세</a>
          <a href="#" className="flex h-full items-center border-b-2 border-transparent font-medium text-muted-foreground transition-colors hover:text-primary">투자내역</a>
          <a href="#" className="flex h-full items-center border-b-2 border-transparent font-medium text-muted-foreground transition-colors hover:text-primary">입출금</a>
          <a href="#" className="flex h-full items-center border-b-2 border-transparent font-medium text-muted-foreground transition-colors hover:text-primary">랭킹</a>
        </nav>
      </div>
    </header>
  );
}
