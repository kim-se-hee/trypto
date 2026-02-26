import { ArrowUpDown, ArrowUp, ArrowDown } from "lucide-react";
import type { SortDir } from "@/hooks/useSort";

interface SortIconProps {
  column: string;
  activeColumn: string | null;
  direction: SortDir;
}

export function SortIcon({ column, activeColumn, direction }: SortIconProps) {
  if (activeColumn !== column) return <ArrowUpDown className="h-3 w-3 opacity-30" />;
  return direction === "asc"
    ? <ArrowUp className="h-3 w-3 text-primary" />
    : <ArrowDown className="h-3 w-3 text-primary" />;
}
