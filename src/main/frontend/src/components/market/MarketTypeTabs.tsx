import { cn } from "@/lib/utils";

export type MarketType = "cex" | "dex";

interface MarketTypeTabsProps {
  selected: MarketType;
  onSelect: (type: MarketType) => void;
}

const tabs: { value: MarketType; label: string }[] = [
  { value: "cex", label: "CEX" },
  { value: "dex", label: "DEX" },
];

export function MarketTypeTabs({ selected, onSelect }: MarketTypeTabsProps) {
  return (
    <div className="relative flex gap-1 rounded-xl bg-secondary/70 p-1">
      {tabs.map((tab) => {
        const isActive = tab.value === selected;
        return (
          <button
            key={tab.value}
            onClick={() => onSelect(tab.value)}
            className={cn(
              "relative rounded-lg px-6 py-2.5 text-base font-bold tracking-wide transition-all duration-200",
              isActive
                ? "bg-gradient-to-r from-primary to-[#9A6AFF] text-primary-foreground shadow-md"
                : "text-muted-foreground hover:text-foreground hover:bg-white/50",
            )}
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
