import { cn } from "@/lib/utils";

export interface ExchangeTabItem {
  id: string;
  name: string;
  baseCurrency: string;
}

interface ExchangeTabsProps {
  exchanges: ExchangeTabItem[];
  selected: string;
  onSelect: (exchangeId: string) => void;
}

export function ExchangeTabs({ exchanges, selected, onSelect }: ExchangeTabsProps) {
  return (
    <div className="flex gap-1 rounded-full bg-secondary/80 p-1">
      {exchanges.map((exchange) => {
        const isActive = exchange.id === selected;
        return (
          <button
            key={exchange.id}
            onClick={() => onSelect(exchange.id)}
            className={cn(
              "relative rounded-full px-4 py-1.5 text-sm font-medium transition-all duration-200",
              isActive
                ? "bg-primary text-primary-foreground shadow-md"
                : "text-muted-foreground hover:text-foreground hover:bg-white/60",
            )}
          >
            {exchange.name}
            <span className={cn(
              "ml-1.5 text-xs",
              isActive ? "text-primary/60" : "text-muted-foreground/60",
            )}>
              {exchange.baseCurrency}
            </span>
          </button>
        );
      })}
    </div>
  );
}
