import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
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
import type { OrderTargetFailure, OrderTargetIds } from "@/lib/api/id-mapping";
import type { UserEvent } from "@/lib/api/websocket";

type OrderTab = "buy" | "sell" | "history";
type OrderType = "limit" | "market";

interface OrderPanelProps {
  baseCurrency: string;
  coinSymbol: string;
  coinName: string;
  currentPrice: number;
  feeRate: number;
  orderTargetIds: OrderTargetIds | null;
  orderTargetFailure: OrderTargetFailure | null;
  orderFilledEvent: UserEvent | null;
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
  CANCELED: { text: "취소", className: "bg-muted text-muted-foreground" },
  FAILED: { text: "실패", className: "bg-destructive/15 text-destructive" },
};

function formatNumber(value: number, digits = 0) {
  return value.toLocaleString("ko-KR", {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  });
}

// 가격은 1 미만 코인도 있으므로 자릿수를 자르지 않고 소수 8자리까지 그대로 보여준다.
function formatPrice(value: number) {
  return value.toLocaleString("ko-KR", { maximumFractionDigits: 8 });
}

// 잔고는 서버에서 소수 8자리 내림으로 관리된다. 수량을 반올림으로 만들면
// 실잔고보다 큰 값이 제출되어 INSUFFICIENT_BALANCE 가 나므로 수량은 항상 내림한다.
const QUANTITY_SCALE = 8;

function floorTo(value: number, digits: number) {
  const factor = 10 ** digits;
  return Math.floor(value * factor) / factor;
}

function formatFloored(value: number, digits: number) {
  return floorTo(value, digits).toLocaleString("ko-KR", {
    minimumFractionDigits: 0,
    maximumFractionDigits: digits,
  });
}

function parseNumber(value: string) {
  const parsed = Number(value.replaceAll(",", ""));
  return Number.isFinite(parsed) ? parsed : 0;
}

// 매수 100%의 최대 주문 금액. 서버가 주문 금액 + 수수료를 잠그므로 수수료 몫을 미리 비워둔다.
// KRW는 서버가 수수료를 정수 원으로 내림 절삭하므로 X + floor(X × 요율) ≤ 잔고인 최대 정수 X를 찾고,
// USDT는 8자리 수수료 그대로라 잔고 / (1 + 요율) 내림으로 충분하다.
function maxBuyAmount(available: number, feeRate: number, integerFee: boolean) {
  let max = Math.floor(available / (1 + feeRate));
  if (!integerFee) return max;
  while (max + 1 + Math.floor((max + 1) * feeRate) <= available) {
    max += 1;
  }
  return max;
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
  orderTargetFailure,
  orderFilledEvent,
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
    if (!orderFilledEvent) return;
    const { orderId, side, quantity: filledQty, price: filledPrice, fee } = orderFilledEvent;

    if (side === "BUY") {
      setAvailableSell((prev) => prev + filledQty);
    } else {
      setAvailableBuy((prev) => prev + filledPrice * filledQty - fee);
    }

    // 체결된 주문은 미체결 목록에서 빠져야 한다. 서버는 재조회 시 status 필터로 제외하지만
    // 미체결 탭을 열어둔 채 체결되면 재조회가 없으므로 이벤트의 orderId 로 직접 제거한다.
    setHistoryItems((prev) => prev.filter((item) => item.orderId !== orderId));
  }, [orderFilledEvent]);

  useEffect(() => {
    if (activeTab !== "history") return;
    void loadHistory(true);
  }, [activeTab, historyFilter, orderTargetIds, loadHistory]);

  // 지정가 입력 편의를 위해 가격 칸을 현재가로 미리 채운다. 사용자가 만진 값은 시세 갱신으로 덮어쓰지 않는다.
  const priceTouched = useRef(false);
  const currentPriceRef = useRef(currentPrice);
  currentPriceRef.current = currentPrice;

  useEffect(() => {
    priceTouched.current = false;
    setPrice(currentPriceRef.current > 0 ? formatPrice(currentPriceRef.current) : "");
    setQuantity("");
    setAmount("");
    setSubmitError("");
  }, [coinSymbol, orderTargetIds, activeTab]);

  // 코인 전환 시점에 시세가 아직 없었다면 첫 시세 도착 때 한 번 채운다.
  useEffect(() => {
    if (priceTouched.current || currentPrice <= 0 || price !== "") return;
    setPrice(formatPrice(currentPrice));
  }, [currentPrice, price]);

  // 화면 불변식: 수량 × 가격 = 총액. 가격·수량을 고치면 총액을, 총액을 고치면 수량을 다시 계산한다.
  const syncByPrice = (nextPrice: number) => {
    if (orderType !== "limit" || nextPrice <= 0) return;

    const nextQty = parseNumber(quantity);
    if (nextQty <= 0) return;
    setAmount(formatNumber(nextQty * nextPrice));
  };

  const handlePriceChange = (value: string) => {
    priceTouched.current = true;
    setPrice(value);
    syncByPrice(parseNumber(value));
  };

  const handleStepPrice = (delta: number) => {
    priceTouched.current = true;
    const base = parseNumber(price) || currentPrice;
    const next = Math.max(0, base + delta);
    setPrice(formatPrice(next));
    syncByPrice(next);
  };

  const handleQuantityChange = (value: string) => {
    setQuantity(value);

    if (orderType !== "limit") return;

    const nextQty = parseNumber(value);
    if (nextQty <= 0) return;

    setAmount(formatNumber(nextQty * displayPrice));
  };

  const handleAmountChange = (value: string) => {
    setAmount(value);

    if (orderType !== "limit") return;

    const nextAmount = parseNumber(value);
    if (nextAmount <= 0) return;

    setQuantity(formatFloored(nextAmount / displayPrice, 6));
  };

  const handleRatioClick = (ratio: number) => {
    if (!isTradeTab) return;

    if (isBuy) {
      // 100%만 수수료 몫을 비워 최대 금액을 잡는다. 그 미만 비율은 수수료 이상의 여유가 남아 그대로 둔다.
      const nextAmount =
        ratio === 100
          ? maxBuyAmount(availableBuy, feeRate, baseCurrency === "KRW")
          : floorTo((availableBuy * ratio) / 100, 0);
      setAmount(formatNumber(nextAmount));
      if (orderType === "limit") {
        setQuantity(formatFloored(nextAmount / displayPrice, 6));
      }
      return;
    }

    // 전량 매도가 실잔고와 정확히 일치하도록 표시 자릿수(6)가 아닌 잔고 자릿수(8)로 내림해 제출한다.
    const nextQty = floorTo((availableSell * ratio) / 100, QUANTITY_SCALE);
    setQuantity(formatFloored(nextQty, QUANTITY_SCALE));
    if (orderType === "limit") {
      setAmount(formatNumber(nextQty * displayPrice));
    }
  };

  async function handleCancel(orderId: number) {
    if (!orderTargetIds) {
      setHistoryError("선택한 거래소/코인의 주문 매핑이 없습니다.");
      return;
    }

    try {
      await cancelOrder(orderId, orderTargetIds.walletId);
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

    const isMarketBuy = orderType === "market" && isBuy;

    if (isMarketBuy && parsedAmount <= 0) {
      setSubmitError("주문 총액을 입력해 주세요.");
      return;
    }

    if (!isMarketBuy && parsedQuantity <= 0) {
      setSubmitError("주문 수량을 입력해 주세요.");
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
        volume: isMarketBuy ? undefined : parsedQuantity,
        price: orderType === "limit" ? parsedPrice : isMarketBuy ? parsedAmount : undefined,
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

          {mappedUnavailable && orderTargetFailure === "NO_ROUND" && (
            <div className="mt-4 rounded-xl border border-warning/30 bg-warning/10 px-3 py-3 text-xs text-warning-foreground">
              <p>진행 중인 라운드가 없어 주문할 수 없습니다.</p>
              <Link
                to="/round/new"
                className="mt-2 inline-block font-semibold underline underline-offset-2"
              >
                라운드 시작하기
              </Link>
            </div>
          )}

          {mappedUnavailable && orderTargetFailure === "COIN_UNLISTED" && (
            <div className="mt-4 rounded-xl border border-warning/30 bg-warning/10 px-3 py-2 text-xs text-warning-foreground">
              이 코인은 아직 주문을 지원하지 않습니다.
            </div>
          )}

          {mappedUnavailable && orderTargetFailure === "LOOKUP_FAILED" && (
            <div className="mt-4 rounded-xl border border-warning/30 bg-warning/10 px-3 py-2 text-xs text-warning-foreground">
              주문 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.
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
                const isPending = item.status === "PENDING";
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
                    <div
                      className={cn(
                        "mt-2 grid gap-2 text-[11px] text-muted-foreground",
                        // 부분 체결이 없어 미체결 주문은 체결 금액이 아직 없다. 금액 칸을 빼고 주문 가격·수량만 보여준다.
                        isPending ? "grid-cols-2" : "grid-cols-3",
                      )}
                    >
                      <div>
                        <p>가격</p>
                        <p className="font-mono text-xs font-semibold text-foreground">
                          {formatPrice(priceValue)} {baseCurrency}
                        </p>
                      </div>
                      <div>
                        <p>수량</p>
                        <p className="font-mono text-xs font-semibold text-foreground">
                          {formatNumber(item.quantity, 6)} {coinSymbol}
                        </p>
                      </div>
                      {!isPending && (
                        <div>
                          <p>금액</p>
                          <p className="font-mono text-xs font-semibold text-foreground">
                            {formatNumber(item.orderAmount)} {baseCurrency}
                          </p>
                        </div>
                      )}
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
                <span className="text-[11px]">
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
                  {formatFloored(tradeBase, isBuy ? 0 : 6)} {unitLabel}
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

