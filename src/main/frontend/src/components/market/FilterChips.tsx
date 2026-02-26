import { cn } from "@/lib/utils";

export type FilterType = "all" | "rising" | "falling" | "volume";

interface FilterChipsProps {
  selected: FilterType;
  onSelect: (filter: FilterType) => void;
}

const FILTERS: { key: FilterType; label: string }[] = [
  { key: "all", label: "전체" },
  { key: "rising", label: "상승" },
  { key: "falling", label: "하락" },
  { key: "volume", label: "거래량순" },
];

export function FilterChips({ selected, onSelect }: FilterChipsProps) {
  return (
    <div className="flex gap-1.5">
      {FILTERS.map(({ key, label }) => (
        <button
          key={key}
          onClick={() => onSelect(key)}
          className={cn(
            "rounded-full px-3 py-1 text-xs font-medium transition-all",
            selected === key
              ? "bg-primary text-primary-foreground shadow-sm"
              : "bg-white border border-border text-muted-foreground hover:border-primary/30 hover:text-foreground",
          )}
        >
          {label}
        </button>
      ))}
    </div>
  );
}
