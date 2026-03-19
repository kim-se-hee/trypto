import { useMemo, useState } from "react";
import { ArrowDownLeft, ArrowUpRight } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { CoinIcon } from "@/components/market/CoinIcon";
import { cn } from "@/lib/utils";
import { formatQuantity } from "@/lib/formatters";
import type { TransferRecord, WalletData } from "@/lib/types/wallet";

interface TransferHistoryPanelProps {
  exchangeId: string;
  exchanges: WalletData[];
  records: TransferRecord[];
  assetFilter?: string | null;
}

type TypeFilter = "all" | "deposit" | "withdraw";
type StatusFilter = "all" | "progress" | "done";

const TYPE_TABS: { key: TypeFilter; label: string }[] = [
  { key: "all", label: "전체" },
  { key: "deposit", label: "입금" },
  { key: "withdraw", label: "출금" },
];

const STATUS_TABS: { key: StatusFilter; label: string }[] = [
  { key: "all", label: "전체" },
  { key: "progress", label: "진행중" },
  { key: "done", label: "완료" },
];


const STATUS_LABEL: Record<TransferRecord["status"], string> = {
  PENDING: "대기",
  PROCESSING: "처리중",
  COMPLETED: "완료",
  FAILED: "실패",
  RETURNED: "반환",
  DELAYED: "지연",
};

const STATUS_CLASS: Record<TransferRecord["status"], string> = {
  PENDING: "bg-chart-4/15 text-chart-4 border-chart-4/30",
  PROCESSING: "bg-chart-3/15 text-chart-3 border-chart-3/30",
  COMPLETED: "bg-emerald-500/15 text-emerald-600 border-emerald-500/30",
  FAILED: "bg-destructive/15 text-destructive border-destructive/30",
  RETURNED: "bg-orange-500/15 text-orange-600 border-orange-500/30",
  DELAYED: "bg-amber-500/15 text-amber-600 border-amber-500/30",
};

function isProgressStatus(status: TransferRecord["status"]): boolean {
  return status === "PENDING" || status === "PROCESSING" || status === "DELAYED";
}

const dateFormatter = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
});

function formatDate(value: string): string {
  return dateFormatter.format(new Date(value));
}

export function TransferHistoryPanel({ exchangeId, exchanges, records, assetFilter }: TransferHistoryPanelProps) {
  const [typeFilter, setTypeFilter] = useState<TypeFilter>("all");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");
  const [selected, setSelected] = useState<TransferRecord | null>(null);

  const exchangeName = useMemo(
    () => exchanges.find((exchange) => exchange.exchangeId === exchangeId)?.exchangeName ?? exchangeId,
    [exchangeId, exchanges],
  );

  const coinNameMap = useMemo(() => {
    const map = new Map<string, string>();
    exchanges.forEach((exchange) => {
      exchange.balances.forEach((balance) => {
        map.set(balance.coinSymbol, balance.coinName);
      });
    });
    return map;
  }, [exchanges]);

  const filtered = useMemo(() => {
    return records
      .filter((item) => item.exchangeId === exchangeId)
      .filter((item) => (assetFilter ? item.asset === assetFilter : true))
      .filter((item) => {
        if (typeFilter === "all") return true;
        const targetType = typeFilter === "deposit" ? "DEPOSIT" : "WITHDRAW";
        return item.type === targetType;
      })
      .filter((item) => {
        if (statusFilter === "all") return true;
        if (statusFilter === "progress") return isProgressStatus(item.status);
        if (statusFilter === "done") return item.status === "COMPLETED";
        return true;
      })
      .sort((a, b) => new Date(b.requestedAt).getTime() - new Date(a.requestedAt).getTime());
  }, [records, exchangeId, typeFilter, statusFilter, assetFilter]);

  return (
    <section className="mt-6 overflow-hidden rounded-xl border border-border bg-card">
      <div className="border-b border-border/30 px-6 py-5">
        <h3 className="text-lg font-bold">입출금 내역</h3>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-border/30 px-6 py-4">
        <div className="flex flex-wrap items-center gap-2">
          {TYPE_TABS.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setTypeFilter(tab.key)}
              className={cn(
                "rounded-full px-3 py-1 text-xs font-semibold transition",
                typeFilter === tab.key
                  ? "bg-primary text-primary-foreground"
                  : "border border-border/60 text-muted-foreground hover:border-primary/40 hover:text-foreground",
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {STATUS_TABS.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setStatusFilter(tab.key)}
              className={cn(
                "rounded-full px-3 py-1 text-xs font-semibold transition",
                statusFilter === tab.key
                  ? "bg-secondary text-foreground"
                  : "border border-border/60 text-muted-foreground hover:border-primary/40 hover:text-foreground",
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="flex h-48 items-center justify-center text-sm text-muted-foreground">
          조건에 맞는 입출금 내역이 없습니다.
        </div>
      ) : (
        <div className="divide-y divide-border/30">
          {filtered.map((item) => {
            const directionLabel = item.type === "DEPOSIT" ? "입금" : "출금";
            const directionIcon = item.type === "DEPOSIT" ? ArrowDownLeft : ArrowUpRight;
            const Icon = directionIcon;
            const coinName = coinNameMap.get(item.asset) ?? item.asset;

            return (
              <button
                key={item.id}
                onClick={() => setSelected(item)}
                className="w-full px-6 py-4 text-left transition hover:bg-primary/[0.04]"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="text-xs text-muted-foreground">{formatDate(item.requestedAt)}</span>
                </div>

                <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
                  <span className="flex items-center gap-2">
                    <CoinIcon symbol={item.asset} size={26} />
                    <span className="flex flex-col text-xs">
                      <span className="font-semibold text-foreground">{item.asset}</span>
                      <span className="text-muted-foreground">{coinName}</span>
                    </span>
                  </span>
                  <span className="flex items-center gap-2 text-xs font-semibold text-muted-foreground">
                    <Icon className="h-3.5 w-3.5 text-primary" />
                    {directionLabel}
                  </span>
                  <span className="font-mono text-sm font-semibold tabular-nums">
                    {formatQuantity(item.amount)} {item.asset}
                  </span>
                </div>

              </button>
            );
          })}
        </div>
      )}

      <Dialog open={!!selected} onOpenChange={(open) => setSelected(open ? selected : null)}>
        <DialogContent className="max-w-2xl">
          {selected && (
            <>
              <DialogHeader>
                <DialogTitle>{selected.asset} {selected.type === "DEPOSIT" ? "입금" : "출금"} 상세</DialogTitle>
                <DialogDescription>
                  {formatDate(selected.requestedAt)} 요청 · {exchangeName}
                </DialogDescription>
              </DialogHeader>

              <div className="grid gap-4 text-sm">
                <div className="grid grid-cols-2 gap-4 rounded-xl border border-border/60 bg-secondary/20 p-4">
                  <div>
                    <p className="text-xs text-muted-foreground">수량</p>
                    <p className="mt-1 font-mono text-base font-semibold">{formatQuantity(selected.amount)} {selected.asset}</p>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground">상태</p>
                    <Badge variant="outline" className={cn("mt-1 border text-[11px]", STATUS_CLASS[selected.status])}>
                      {STATUS_LABEL[selected.status]}
                    </Badge>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground">출발 거래소</p>
                    <p className="mt-1 font-semibold">{selected.fromExchangeName}</p>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground">도착 거래소</p>
                    <p className="mt-1 font-semibold">{selected.toExchangeName}</p>
                  </div>
                </div>

                {selected.completedAt && (
                  <div className="grid gap-3 rounded-xl border border-border/60 p-4 text-xs">
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-muted-foreground">완료 시각</span>
                      <span className="font-mono text-foreground">{formatDate(selected.completedAt)}</span>
                    </div>
                  </div>
                )}
              </div>
            </>
          )}
        </DialogContent>
      </Dialog>
    </section>
  );
}
