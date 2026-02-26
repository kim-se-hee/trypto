import { useMemo, useState } from "react";
import { Info } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

type OrderTab = "buy" | "sell" | "history";
type OrderType = "limit" | "market";
type OrderSide = "BUY" | "SELL";
type OrderStatus = "FILLED" | "PENDING" | "CANCELLED";

interface OrderHistoryItem {
  id: number;
  side: OrderSide;
  type: "LIMIT" | "MARKET";
  status: OrderStatus;
  price: number;
  quantity: number;
  amount: number;
  time: string;
}

interface OrderPanelProps {
  baseCurrency: string;
  coinSymbol: string;
  coinName: string;
  currentPrice: number;
  availableBase: number;
  availableCoin: number;
  feeRate: number;
}

const ORDER_TABS: { key: OrderTab; label: string }[] = [
  { key: "buy", label: "매수" },
  { key: "sell", label: "매도" },
  { key: "history", label: "거래내역" },
];

const ORDER_TYPES: { key: OrderType; label: string }[] = [
  { key: "limit", label: "지정가" },
  { key: "market", label: "시장가" },
];

const QUICK_RATIO_BUTTONS = [10, 25, 50, 100];

const MOCK_HISTORY: OrderHistoryItem[] = [
  { id: 1, side: "BUY", type: "LIMIT", status: "FILLED", price: 99850000, quantity: 0.0032, amount: 319520, time: "방금 전" },
  { id: 2, side: "SELL", type: "MARKET", status: "FILLED", price: 100120000, quantity: 0.0011, amount: 110132, time: "12분 전" },
  { id: 3, side: "BUY", type: "LIMIT", status: "PENDING", price: 99000000, quantity: 0.002, amount: 198000, time: "38분 전" },
];

const STATUS_STYLES: Record<string, { text: string; className: string }> = {
  FILLED: { text: "체결", className: "bg-positive/15 text-positive" },
  PENDING: { text: "대기", className: "bg-warning/15 text-warning" },
  CANCELLED: { text: "취소", className: "bg-muted text-muted-foreground" },
};

function formatNumber(value: number, digits = 0) {
  return value.toLocaleString("ko-KR", {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  });
}

function parseNumber(value: string) {
  const parsed = Number(value.replaceAll(",", ""));
  return Number.isFinite(parsed) ? parsed : 0;
}

export function OrderPanel({
  baseCurrency,
  coinSymbol,
  coinName,
  currentPrice,
  availableBase,
  availableCoin,
  feeRate,
}: OrderPanelProps) {
  const [activeTab, setActiveTab] = useState<OrderTab>("buy");
  const [historyFilter, setHistoryFilter] = useState<"filled" | "pending">("filled");
  const [historyItems, setHistoryItems] = useState(MOCK_HISTORY);
  const [orderType, setOrderType] = useState<OrderType>("limit");
  const [price, setPrice] = useState("");
  const [quantity, setQuantity] = useState("");
  const [amount, setAmount] = useState("");
  const [lastEdited, setLastEdited] = useState<"quantity" | "amount" | null>(null);

  const filteredHistory = historyItems.filter((item) =>
    historyFilter === "filled" ? item.status === "FILLED" : item.status === "PENDING",
  );

  const handleCancel = (id: number) => {
    setHistoryItems((prev) =>
      prev.map((item) =>
        item.id === id ? { ...item, status: "CANCELLED" as const } : item,
      ),
    );
  };

  const isBuy = activeTab === "buy";
  const isTradeTab = activeTab === "buy" || activeTab === "sell";
  const isMarket = orderType === "market";
  const showQuantityInput = !isMarket || !isBuy;
  const showAmountInput = !isMarket || isBuy;

  const tradeBase = isBuy ? availableBase : availableCoin;
  const unitLabel = isBuy ? baseCurrency : coinSymbol;

  const displayPrice = useMemo(() => {
    if (isMarket) return currentPrice;
    const parsed = parseNumber(price);
    return parsed > 0 ? parsed : currentPrice;
  }, [isMarket, price, currentPrice]);

  const syncByPrice = (nextPrice: number) => {
    if (orderType !== "limit" || nextPrice <= 0) return;
    if (lastEdited === "amount") {
      const nextAmount = parseNumber(amount);
      if (nextAmount <= 0) return;
      const nextQty = nextAmount / nextPrice;
      setQuantity(formatNumber(nextQty, 6));
    }
    if (lastEdited === "quantity") {
      const nextQty = parseNumber(quantity);
      if (nextQty <= 0) return;
      const nextAmount = nextQty * nextPrice;
      setAmount(formatNumber(nextAmount));
    }
  };

  const handlePriceChange = (value: string) => {
    setPrice(value);
    const nextPrice = parseNumber(value);
    syncByPrice(nextPrice);
  };

  const handleStepPrice = (delta: number) => {
    const base = parseNumber(price) || currentPrice;
    const next = Math.max(0, base + delta);
    setPrice(formatNumber(next));
    syncByPrice(next);
  };

  const handleQuantityChange = (value: string) => {
    setQuantity(value);
    setLastEdited("quantity");
    if (orderType !== "limit") return;
    const nextQty = parseNumber(value);
    if (nextQty <= 0) return;
    const nextAmount = nextQty * displayPrice;
    setAmount(formatNumber(nextAmount));
  };

  const handleAmountChange = (value: string) => {
    setAmount(value);
    setLastEdited("amount");
    if (orderType !== "limit") return;
    const nextAmount = parseNumber(value);
    if (nextAmount <= 0) return;
    const nextQty = nextAmount / displayPrice;
    setQuantity(formatNumber(nextQty, 6));
  };

  const handleRatioClick = (ratio: number) => {
    if (!isTradeTab) return;
    if (isBuy) {
      const nextAmount = (availableBase * ratio) / 100;
      setAmount(formatNumber(nextAmount));
      setLastEdited("amount");
      if (orderType === "limit") {
        const nextQty = nextAmount / displayPrice;
        setQuantity(formatNumber(nextQty, 6));
      }
    } else {
      const nextQty = (availableCoin * ratio) / 100;
      setQuantity(formatNumber(nextQty, 6));
      setLastEdited("quantity");
      if (orderType === "limit") {
        const nextAmount = nextQty * displayPrice;
        setAmount(formatNumber(nextAmount));
      }
    }
  };

  return (
    <div className="sticky top-24 space-y-4">
      <div className="rounded-3xl border border-primary/10 bg-gradient-to-br from-white via-white to-primary/5 p-[1px] shadow-card">
        <div className="rounded-[22px] bg-card/95 p-5 backdrop-blur">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-xs font-semibold text-muted-foreground">주문 패널</p>
              <h2 className="mt-1 text-lg font-extrabold tracking-tight">
                {coinSymbol} <span className="text-muted-foreground">/ {baseCurrency}</span>
              </h2>
              <p className="mt-1 text-xs text-muted-foreground">{coinName} · {formatNumber(currentPrice)} {baseCurrency}</p>
            </div>
          </div>

          <div className="mt-5 rounded-2xl bg-secondary/60 p-1">
            <div className="grid grid-cols-3 gap-1">
              {ORDER_TABS.map((tab) => (
                <button
                  key={tab.key}
                  onClick={() => setActiveTab(tab.key)}
                  className={cn(
                    "rounded-xl px-2 py-2 text-xs font-semibold transition-all",
                    activeTab === tab.key
                      ? "bg-card text-foreground shadow-sm"
                      : "text-muted-foreground hover:text-foreground",
                  )}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>

          {activeTab === "history" && (
            <div className="mt-6 space-y-3">
              <div className="flex items-center gap-2 rounded-xl bg-secondary/60 p-1 text-xs font-semibold text-muted-foreground">
                <button
                  onClick={() => setHistoryFilter("filled")}
                  className={cn(
                    "flex-1 rounded-lg px-3 py-1.5 transition-all",
                    historyFilter === "filled"
                      ? "bg-card text-foreground shadow-sm"
                      : "hover:text-foreground",
                  )}
                >
                  체결
                </button>
                <button
                  onClick={() => setHistoryFilter("pending")}
                  className={cn(
                    "flex-1 rounded-lg px-3 py-1.5 transition-all",
                    historyFilter === "pending"
                      ? "bg-card text-foreground shadow-sm"
                      : "hover:text-foreground",
                  )}
                >
                  미체결
                </button>
              </div>

              {filteredHistory.map((item) => {
                const status = STATUS_STYLES[item.status] ?? STATUS_STYLES.PENDING;
                const isBuySide = item.side === "BUY";
                return (
                  <div
                    key={item.id}
                    className="rounded-2xl border border-border/60 bg-white px-4 py-3 shadow-sm transition hover:shadow-card-hover"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span
                          className={cn(
                            "rounded-full px-2 py-0.5 text-[10px] font-bold",
                            isBuySide ? "bg-primary/10 text-primary" : "bg-destructive/10 text-destructive",
                          )}
                        >
                          {isBuySide ? "매수" : "매도"}
                        </span>
                        <span className="text-xs font-semibold text-muted-foreground">
                          {item.type === "MARKET" ? "시장가" : "지정가"}
                        </span>
                        <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-semibold", status.className)}>
                          {status.text}
                        </span>
                      </div>
                      <div className="flex items-center gap-2">
                        {item.status === "PENDING" && (
                          <button
                            onClick={() => handleCancel(item.id)}
                            className="rounded-full border border-border/60 px-2.5 py-1 text-[10px] font-semibold text-muted-foreground transition hover:border-destructive/30 hover:text-destructive"
                          >
                            취소
                          </button>
                        )}
                        <span className="text-[11px] text-muted-foreground">{item.time}</span>
                      </div>
                    </div>
                    <div className="mt-2 grid grid-cols-3 gap-2 text-[11px] text-muted-foreground">
                      <div>
                        <p>가격</p>
                        <p className="font-mono text-xs font-semibold text-foreground">
                          {formatNumber(item.price)} {baseCurrency}
                        </p>
                      </div>
                      <div>
                        <p>수량</p>
                        <p className="font-mono text-xs font-semibold text-foreground">
                          {formatNumber(item.quantity, 6)} {coinSymbol}
                        </p>
                      </div>
                      <div>
                        <p>금액</p>
                        <p className="font-mono text-xs font-semibold text-foreground">
                          {formatNumber(item.amount)} {baseCurrency}
                        </p>
                      </div>
                    </div>
                  </div>
                );
              })}

              {filteredHistory.length === 0 && (
                <div className="rounded-2xl border border-dashed border-border/70 bg-secondary/30 px-4 py-6 text-center text-sm text-muted-foreground">
                  {historyFilter === "filled" ? "체결 내역이 없습니다." : "미체결 주문이 없습니다."}
                </div>
              )}
            </div>
          )}

          {isTradeTab && (
            <>
              <div className="mt-5 flex items-center justify-between text-xs font-semibold text-muted-foreground">
                <span>주문유형</span>
                <span className="flex items-center gap-1 text-[11px]">
                  <Info className="h-3 w-3" />
                  주문 가능
                </span>
              </div>

              <div className="mt-2 grid grid-cols-2 gap-2">
                {ORDER_TYPES.map((type) => (
                  <button
                    key={type.key}
                    onClick={() => setOrderType(type.key)}
                    className={cn(
                      "rounded-xl border px-3 py-2 text-xs font-semibold transition-all",
                      orderType === type.key
                        ? "border-primary bg-primary/10 text-primary shadow-sm"
                        : "border-border/60 bg-white text-muted-foreground hover:text-foreground",
                    )}
                  >
                    {type.label}
                  </button>
                ))}
              </div>

              <div className="mt-5 flex items-center justify-between text-xs font-semibold text-muted-foreground">
                <span>주문가능</span>
                <span className="font-mono text-sm text-foreground">
                  {formatNumber(tradeBase, isBuy ? 0 : 6)} {unitLabel}
                </span>
              </div>

              <div className="mt-4 space-y-3">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground">
                    {isBuy ? "매수가격" : "매도가격"} ({baseCurrency})
                  </label>
                  <div className="mt-1.5 flex items-center gap-2 rounded-2xl border border-border/70 bg-white px-3 py-2">
                    <Input
                      value={isMarket ? formatNumber(currentPrice) : price}
                      onChange={(e) => handlePriceChange(e.target.value)}
                      disabled={isMarket}
                      className="h-8 border-0 bg-transparent p-0 text-right text-sm font-semibold shadow-none focus-visible:ring-0"
                    />
                    <div className="flex items-center gap-1">
                      <button
                        type="button"
                        onClick={() => handleStepPrice(-1000)}
                        className="h-7 w-7 rounded-full border border-border/60 text-sm text-muted-foreground transition hover:text-foreground"
                      >
                        -
                      </button>
                      <button
                        type="button"
                        onClick={() => handleStepPrice(1000)}
                        className="h-7 w-7 rounded-full border border-border/60 text-sm text-muted-foreground transition hover:text-foreground"
                      >
                        +
                      </button>
                    </div>
                  </div>
                </div>

                {showQuantityInput && (
                <div>
                  <label className="text-xs font-semibold text-muted-foreground">
                    주문수량 ({coinSymbol})
                  </label>
                  <div className="mt-1.5 rounded-2xl border border-border/70 bg-white px-3 py-2">
                    <Input
                      value={quantity}
                      onChange={(e) => handleQuantityChange(e.target.value)}
                      placeholder="0"
                      className="h-8 border-0 bg-transparent p-0 text-right text-sm font-semibold shadow-none focus-visible:ring-0"
                    />
                  </div>
                </div>
                )}

                <div className="flex flex-wrap gap-2">
                  {QUICK_RATIO_BUTTONS.map((ratio) => (
                    <button
                      key={ratio}
                      onClick={() => handleRatioClick(ratio)}
                      className="rounded-lg border border-border/70 bg-white px-3 py-1.5 text-xs font-semibold text-muted-foreground transition hover:border-primary/30 hover:text-foreground"
                    >
                      {ratio}%
                    </button>
                  ))}
                  <button className="rounded-lg border border-border/70 bg-white px-3 py-1.5 text-xs font-semibold text-muted-foreground transition hover:border-primary/30 hover:text-foreground">
                    직접입력
                  </button>
                </div>

                {showAmountInput && (
                <div>
                  <label className="text-xs font-semibold text-muted-foreground">
                    주문총액 ({baseCurrency})
                  </label>
                  <div className="mt-1.5 rounded-2xl border border-border/70 bg-white px-3 py-2">
                    <Input
                      value={amount}
                      onChange={(e) => handleAmountChange(e.target.value)}
                      placeholder="0"
                      className="h-8 border-0 bg-transparent p-0 text-right text-sm font-semibold shadow-none focus-visible:ring-0"
                    />
                  </div>
                </div>
                )}
              </div>

              <div className="mt-4 flex items-center justify-between text-[11px] text-muted-foreground">
                <span>수수료 {formatNumber(feeRate * 100, 2)}%</span>
                <span>최소주문 5,000 {baseCurrency}</span>
              </div>

              <div className="mt-5 grid grid-cols-2 gap-2">
                <Button variant="outline" className="h-11 rounded-2xl text-sm font-semibold">
                  초기화
                </Button>
                <Button
                  className={cn(
                    "h-11 rounded-2xl text-sm font-semibold",
                    isBuy ? "bg-primary text-primary-foreground" : "bg-destructive text-white",
                  )}
                >
                  {isBuy ? "매수" : "매도"}
                </Button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
