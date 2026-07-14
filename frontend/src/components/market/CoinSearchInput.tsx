import { Search, X } from "lucide-react";

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
        placeholder="코인명/심볼 검색 (초성 가능)"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Escape") onChange("");
        }}
        className="h-10 w-full rounded-full border-0 bg-secondary/40 pl-9 pr-10 text-sm text-foreground placeholder:text-muted-foreground/60 outline-none transition-all focus:bg-white focus:shadow-[0_0_0_3px_rgba(118,69,217,0.1)]"
      />
      {value && (
        <button
          type="button"
          onClick={() => onChange("")}
          aria-label="검색어 지우기"
          className="absolute right-2.5 top-1/2 flex h-6 w-6 -translate-y-1/2 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground"
        >
          <X className="h-3.5 w-3.5" />
        </button>
      )}
    </div>
  );
}
