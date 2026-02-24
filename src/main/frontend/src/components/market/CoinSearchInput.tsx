import { Search } from "lucide-react";

interface CoinSearchInputProps {
  value: string;
  onChange: (value: string) => void;
}

export function CoinSearchInput({ value, onChange }: CoinSearchInputProps) {
  return (
    <div className="relative">
      <Search className="pointer-events-none absolute left-3.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
      <input
        type="text"
        placeholder="코인명/심볼 검색"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="h-10 w-full rounded-full border-0 bg-secondary/40 pl-9 pr-4 text-sm text-foreground placeholder:text-muted-foreground/60 outline-none transition-all focus:bg-white focus:shadow-[0_0_0_3px_rgba(118,69,217,0.1)] sm:w-64"
      />
    </div>
  );
}
