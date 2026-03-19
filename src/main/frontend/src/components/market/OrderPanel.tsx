import { useCallback, useEffect, useMemo, useState } from "react";
import { Info } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  cancelOrder,
  getOrderAvailability,
  listOrderHistory,
  placeOrder,
  type OrderHistoryItem,
  type OrderSide,
  type OrderStatus,
} from "@/lib/api/order-api";
import { isApiClientError } from "@/lib/api/types";
import type { OrderTargetIds } from "@/lib/api/id-mapping";

type OrderTab = "buy" | "sell" | "history";
type OrderType = "limit" | "market";

interface OrderPanelProps {
  baseCurrency: string;
  coinSymbol: string;
  coinName: string;
  currentPrice: number;
  feeRate: number;
  orderTargetIds: OrderTargetIds | null;
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

const STATUS_STYLES: Record<OrderStatus, { text: string; className: string }> = {
  FILLED: { text: "체결", className: "bg-positive/15 text-positive" },
  PENDING: { text: "대기", className: "bg-warning/15 text-warning" },
  CANCELLED: { text: "취소", className: "bg-muted text-muted-foreground" },
  FAILED: { text: "실패", className: "bg-destructive/15 text-destructive" },
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

function toClientOrderId() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }

  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function toReadableError(error: unknown): string {
  if (isApiClientError(error)) {
    return error.message || error.code;
  }
  return "요청 처리 중 오류가 발생했습니다.";
}

function formatRelativeTime(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "-";

  const diffMinutes = Math.floor((Date.now() - date.getTime()) / 60000);
  if (diffMinutes < 1) return "방금 전";
  if (diffMinutes < 60) return `${diffMinutes}분 전`;

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours}시간 전`;

  return date.toLocaleString("ko-KR");
}

export function OrderPanel({
  baseCurrency,
  coinSymbol,
  coinName,
  currentPrice,
  feeRate,
  orderTargetIds,
}: OrderPanelProps) {
  const [activeTab, setActiveTab] = useState<OrderTab>("buy");
  const [historyFilter, setHistoryFilter] = useState<"filled" | "pending">("filled");
  const [historyItems, setHistoryItems] = useState<OrderHistoryItem[]>([]);
  const [historyNextCursor, setHistoryNextCursor] = useState<number | null>(null);
  const [historyHasNext, setHistoryHasNext] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState("");

  const [availableBuy, setAvailableBuy] = useState(0);
  const [availableSell, setAvailableSell] = useState(0);
  const [availabilityError, setAvailabilityError] = useState("");

  const [orderType, setOrderType] = useState<OrderType>("limit");
  const [price, setPrice] = useState("");
  const [quantity, setQuantity] = useState("");
  const [amount, setAmount] = useState("");
  const [lastEdited, setLastEdited] = useState<"quantity" | "amount" | null>(null);
  const [submitError, setSubmitError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isBuy = activeTab === "buy";
  const isTradeTab = activeTab === "buy" || activeTab === "sell";
  const isMarket = orderType === "market";
  const showQuantityInput = !isMarket || !isBuy;
  const showAmountInput = !isMarket || isBuy;

  const tradeBase = isBuy ? availableBuy : availableSell;
  const unitLabel = isBuy ? baseCurrency : coinSymbol;

  const displayPrice = useMemo(() => {
    if (isMarket) return currentPrice;
    const parsed = parseNumber(price);
    return parsed > 0 ? parsed : currentPrice;
  }, [isMarket, price, currentPrice]);

  const historyStatus: OrderStatus = historyFilter === "filled" ? "FILLED" : "PENDING";

  const loadAvailability = useCallback(async () => {
    if (!orderTargetIds) {
      setAvailableBuy(0);
      setAvailableSell(0);
      return;
    }

    try {
      const [buy, sell] = await Promise.all([
        getOrderAvailability(orderTargetIds.walletId, orderTargetIds.exchangeCoinId, "BUY"),
        getOrderAvailability(orderTargetIds.walletId, orderTargetIds.exchangeCoinId, "SELL"),
      ]);

      setAvailableBuy(buy.available);
      setAvailableSell(sell.available);
      setAvailabilityError("");
    } catch (error) {
      setAvailabilityError(toReadableError(error));
      setAvailableBuy(0);
      setAvailableSell(0);
    }
  }, [orderTargetIds]);

  const loadHistory = useCallback(
    async (reset: boolean) => {
      if (!orderTargetIds) {
        setHistoryItems([]);
        setHistoryHasNext(false);
        setHistoryNextCursor(null);
        return;
      }

      setHistoryLoading(true);
      setHistoryError("");
      try {
        const data = await listOrderHistory({
          walletId: orderTargetIds.walletId,
          exchangeCoinId: orderTargetIds.exchangeCoinId,
          status: historyStatus,
          cursorOrderId: reset ? undefined : historyNextCursor ?? undefined,
          size: 20,
        });

        setHistoryItems((prev) => (reset ? data.content : [...prev, ...data.content]));
        setHistoryNextCursor(data.nextCursor);
        setHistoryHasNext(data.hasNext);
      } catch (error) {
        setHistoryError(toReadableError(error));
      } finally {
        setHistoryLoading(false);
      }
    },
    [historyNextCursor, historyStatus, orderTargetIds],
  );

  useEffect(() => {
    void loadAvailability();
  }, [loadAvailability]);

  useEffect(() => {
    if (activeTab !== "history") return;
    void loadHistory(true);
  }, [activeTab, historyFilter, orderTargetIds, loadHistory]);

  useEffect(() => {
    setPrice("");
    setQuantity("");
    setAmount("");
    setSubmitError("");
  }, [coinSymbol, orderTargetIds, activeTab]);

  const syncByPrice = (nextPrice: number) => {
    if (orderType !== "limit" || nextPrice <= 0) return;

    if (lastEdited === "amount") {
      const nextAmount = parseNumber(amount);
      if (nextAmount <= 0) return;
      setQuantity(formatNumber(nextAmount / nextPrice, 6));
    }

    if (lastEdited === "quantity") {
      const nextQty = parseNumber(quantity);
      if (nextQty <= 0) return;
      setAmount(formatNumber(nextQty * nextPrice));
    }
  };

  const handlePriceChange = (value: string) => {
    setPrice(value);
    syncByPrice(parseNumber(value));
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

    setAmount(formatNumber(nextQty * displayPrice));
  };

  const handleAmountChange = (value: string) => {
    setAmount(value);
    setLastEdited("amount");

    if (orderType !== "limit") return;

    const nextAmount = parseNumber(value);
    if (nextAmount <= 0) return;

    setQuantity(formatNumber(nextAmount / displayPrice, 6));
  };

  const handleRatioClick = (ratio: number) => {
    if (!isTradeTab) return;

    if (isBuy) {
      const nextAmount = (availableBuy * ratio) / 100;
      setAmount(formatNumber(nextAmount));
      setLastEdited("amount");
      if (orderType === "limit") {
        setQuantity(formatNumber(nextAmount / displayPrice, 6));
      }
      return;
    }

    const nextQty = (availableSell * ratio) / 100;
    setQuantity(formatNumber(nextQty, 6));
    setLastEdited("quantity");
    if (orderType === "limit") {
      setAmount(formatNumber(nextQty * displayPrice));
    }
  };

  async function handleCancel(orderId: number) {
    try {
      await cancelOrder(orderId);
      setHistoryItems((prev) => prev.filter((item) => item.orderId !== orderId));
      await loadAvailability();
    } catch (error) {
      setHistoryError(toReadableError(error));
    }
  }

  async function handleSubmitOrder() {
    if (!orderTargetIds) {
      setSubmitError("선택한 거래소/코인의 주문 매핑이 없습니다.");
      return;
    }

    const side: OrderSide = isBuy ? "BUY" : "SELL";
    const parsedAmount = parseNumber(amount);
    const parsedQuantity = parseNumber(quantity);
    const parsedPrice = parseNumber(price);

    const requestAmount = side === "BUY" ? parsedAmount : parsedQuantity;

    if (requestAmount <= 0) {
      setSubmitError(side === "BUY" ? "주문 총액을 입력해 주세요." : "주문 수량을 입력해 주세요.");
      return;
    }

    if (orderType === "limit" && parsedPrice <= 0) {
      setSubmitError("지정가를 입력해 주세요.");
      return;
    }

    setIsSubmitting(true);
    setSubmitError("");

    try {
      await placeOrder({
        clientOrderId: toClientOrderId(),
        walletId: orderTargetIds.walletId,
        exchangeCoinId: orderTargetIds.exchangeCoinId,
        side,
        orderType: orderType === "limit" ? "LIMIT" : "MARKET",
        price: orderType === "limit" ? parsedPrice : undefined,
        amount: requestAmount,
      });

      setPrice("");
      setQuantity("");
      setAmount("");

      await Promise.all([
        loadAvailability(),
        activeTab === "history" ? loadHistory(true) : Promise.resolve(),
      ]);
    } catch (error) {
      setSubmitError(toReadableError(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  const mappedUnavailable = !orderTargetIds;

  return (
    <div className="sticky top-24 space-y-4">
      <div className="rounded-xl border border-border bg-card p-5">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-xs font-semibold text-muted-foreground">주문 패널</p>
              <h2 className="mt-1 text-lg font-bold tracking-tight">
                {coinSymbol} <span className="text-muted-foreground">/ {baseCurrency}</span>
              </h2>
              <p className="mt-1 text-xs text-muted-foreground">
                {coinName} · {formatNumber(currentPrice)} {baseCurrency}
              </p>
            </div>
          </div>

          {mappedUnavailable && (
            <div className="mt-4 rounded-xl border border-warning/30 bg-warning/10 px-3 py-2 text-xs text-warning-foreground">
              이 코인은 아직 주문 ID 매핑이 없어 주문 API를 호출할 수 없습니다.
            </div>
          )}

          <div className="mt-5 rounded-xl bg-secondary/60 p-1">
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

              {historyItems.map((item) => {
                const status = STATUS_STYLES[item.status] ?? STATUS_STYLES.PENDING;
                const isBuySide = item.side === "BUY";
                const priceValue = item.filledPrice ?? item.price ?? 0;

                return (
                  <div
                    key={item.orderId}
                    className="rounded-xl border border-border/60 bg-white px-4 py-3 shadow-sm transition hover:shadow-card-hover"
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
                          {item.orderType === "MARKET" ? "시장가" : "지정가"}
                        </span>
                        <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-semibold", status.className)}>
                          {status.text}
                        </span>
                      </div>
                      <div className="flex items-center gap-2">
                        {item.status === "PENDING" && (
                          <button
                            onClick={() => void handleCancel(item.orderId)}
                            className="rounded-full border border-border/60 px-2.5 py-1 text-[10px] font-semibold text-muted-foreground transition hover:border-destructive/30 hover:text-destructive"
                          >
                            취소
                          </button>
                        )}
                        <span className="text-[11px] text-muted-foreground">{formatRelativeTime(item.createdAt)}</span>
                      </div>
                    </div>
                    <div className="mt-2 grid grid-cols-3 gap-2 text-[11px] text-muted-foreground">
                      <div>
                        <p>가격</p>
                        <p className="font-mono text-xs font-semibold text-foreground">
                          {formatNumber(priceValue)} {baseCurrency}
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
                          {formatNumber(item.orderAmount)} {baseCurrency}
                        </p>
                      </div>
                    </div>
                  </div>
                );
              })}

              {historyLoading && (
                <div className="rounded-xl border border-dashed border-border/70 bg-secondary/30 px-4 py-3 text-center text-sm text-muted-foreground">
                  거래 내역을 불러오는 중입니다...
                </div>
              )}

              {!historyLoading && historyItems.length === 0 && (
                <div className="rounded-xl border border-dashed border-border/70 bg-secondary/30 px-4 py-6 text-center text-sm text-muted-foreground">
                  {historyFilter === "filled" ? "체결 내역이 없습니다." : "미체결 주문이 없습니다."}
                </div>
              )}

              {historyHasNext && (
                <Button
                  variant="outline"
                  className="w-full"
                  onClick={() => void loadHistory(false)}
                  disabled={historyLoading}
                >
                  더보기
                </Button>
              )}

              {historyError && (
                <p className="text-xs font-medium text-destructive">{historyError}</p>
              )}
            </div>
          )}

          {isTradeTab && (
            <>
              <div className="mt-5 flex items-center justify-between text-xs font-semibold text-muted-foreground">
                <span>주문 유형</span>
                <span className="flex items-center gap-1 text-[11px]">
                  <Info className="h-3 w-3" />
                  주문 가능 조회
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
                <span>주문 가능</span>
                <span className="font-mono text-sm text-foreground">
                  {formatNumber(tradeBase, isBuy ? 0 : 6)} {unitLabel}
                </span>
              </div>

              <div className="mt-4 space-y-3">
                <div>
                  <label className="text-xs font-semibold text-muted-foreground">
                    {isBuy ? "매수 가격" : "매도 가격"} ({baseCurrency})
                  </label>
                  <div className="mt-1.5 flex items-center gap-2 rounded-2xl border border-border/70 bg-white px-3 py-2">
                    <Input
                      value={isMarket ? formatNumber(currentPrice) : price}
                      onChange={(event) => handlePriceChange(event.target.value)}
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
                    <label className="text-xs font-semibold text-muted-foreground">주문 수량 ({coinSymbol})</label>
                    <div className="mt-1.5 rounded-2xl border border-border/70 bg-white px-3 py-2">
                      <Input
                        value={quantity}
                        onChange={(event) => handleQuantityChange(event.target.value)}
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
                </div>

                {showAmountInput && (
                  <div>
                    <label className="text-xs font-semibold text-muted-foreground">주문 총액 ({baseCurrency})</label>
                    <div className="mt-1.5 rounded-2xl border border-border/70 bg-white px-3 py-2">
                      <Input
                        value={amount}
                        onChange={(event) => handleAmountChange(event.target.value)}
                        placeholder="0"
                        className="h-8 border-0 bg-transparent p-0 text-right text-sm font-semibold shadow-none focus-visible:ring-0"
                      />
                    </div>
                  </div>
                )}
              </div>

              <div className="mt-4 flex items-center justify-between text-[11px] text-muted-foreground">
                <span>수수료 {formatNumber(feeRate * 100, 2)}%</span>
                <span>최소 주문 5,000 {baseCurrency}</span>
              </div>

              {availabilityError && (
                <p className="mt-2 text-xs font-medium text-destructive">{availabilityError}</p>
              )}

              {submitError && (
                <p className="mt-2 text-xs font-medium text-destructive">{submitError}</p>
              )}

              <div className="mt-5 grid grid-cols-2 gap-2">
                <Button
                  variant="outline"
                  className="h-11 rounded-xl text-sm font-semibold"
                  onClick={() => {
                    setPrice("");
                    setQuantity("");
                    setAmount("");
                    setSubmitError("");
                  }}
                >
                  초기화
                </Button>
                <Button
                  className={cn(
                    "h-11 rounded-xl text-sm font-semibold",
                    isBuy ? "bg-primary text-primary-foreground" : "bg-destructive text-white",
                  )}
                  onClick={() => void handleSubmitOrder()}
                  disabled={isSubmitting || mappedUnavailable}
                >
                  {isSubmitting ? "요청 중..." : isBuy ? "매수" : "매도"}
                </Button>
              </div>
            </>
          )}
      </div>
    </div>
  );
}

