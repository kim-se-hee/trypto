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
    // 거래소가 늘어나도 좁은 화면에서 줄이 깨지지 않도록, 넘치면 가로로 넘겨 보게 한다.
    <div className="no-scrollbar flex max-w-full gap-1 overflow-x-auto rounded-full bg-secondary/80 p-1">
      {exchanges.map((exchange) => {
        const isActive = exchange.id === selected;
        return (
          <button
            key={exchange.id}
            onClick={() => onSelect(exchange.id)}
            className={cn(
              "relative shrink-0 whitespace-nowrap rounded-full px-3 py-1.5 text-sm font-medium transition-all duration-200 sm:px-4",
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
