import { cn } from "@/lib/utils";

interface PresetButtonsProps {
  presets: { label: string; value: number }[];
  onSelect: (value: number) => void;
  activeValue?: number;
}

export function PresetButtons({ presets, onSelect, activeValue }: PresetButtonsProps) {
  return (
    <div className="flex gap-2">
      {presets.map((p) => {
        const isActive = activeValue === p.value;
        return (
          <button
            key={p.value}
            type="button"
            onClick={() => onSelect(p.value)}
            className={cn(
              "rounded-lg px-3 py-1.5 text-xs font-bold transition-all duration-150",
              isActive
                ? "bg-gradient-to-r from-primary to-[#9A6AFF] text-white shadow-sm"
                : "bg-secondary/60 text-muted-foreground hover:bg-secondary hover:text-foreground",
            )}
          >
            {p.label}
          </button>
        );
      })}
    </div>
  );
}
