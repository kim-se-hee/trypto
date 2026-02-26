import { useState, useMemo } from "react";

export type SortDir = "asc" | "desc";

export interface UseSortOptions<T, K extends string> {
  items: T[];
  defaultKey?: K | null;
  defaultDir?: SortDir;
  comparator: (key: K, dir: SortDir) => (a: T, b: T) => number;
}

export interface UseSortReturn<T, K extends string> {
  sorted: T[];
  sortKey: K | null;
  sortDir: SortDir;
  handleSort: (key: K) => void;
  resetSort: () => void;
}

export function useSort<T, K extends string>({
  items,
  defaultKey = null,
  defaultDir = "desc",
  comparator,
}: UseSortOptions<T, K>): UseSortReturn<T, K> {
  const [sortKey, setSortKey] = useState<K | null>(defaultKey);
  const [sortDir, setSortDir] = useState<SortDir>(defaultDir);

  const sorted = useMemo(() => {
    if (!sortKey) return items;
    return [...items].sort(comparator(sortKey, sortDir));
  }, [items, sortKey, sortDir, comparator]);

  const handleSort = (key: K) => {
    if (sortKey === key) {
      setSortDir((prev) => (prev === "desc" ? "asc" : "desc"));
    } else {
      setSortKey(key);
      setSortDir("desc");
    }
  };

  const resetSort = () => {
    setSortKey(defaultKey);
    setSortDir(defaultDir);
  };

  return { sorted, sortKey, sortDir, handleSort, resetSort };
}
