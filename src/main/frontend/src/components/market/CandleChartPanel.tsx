import { useEffect, useMemo, useRef, useState } from "react";
import { cn } from "@/lib/utils";
import { formatChangeRate, formatPrice, getCurrencySymbol } from "@/lib/formatters";
import {
  findCandles,
  normalizeCandleTime,
  resolveCandleExchangeCode,
  type CandleInterval,
  type CandleItem,
} from "@/lib/api/candle-api";
import type { CoinData } from "@/lib/types/coins";

interface CandleChartPanelProps {
  exchangeKey: string;
  baseCurrency: string;
  coin: CoinData;
}

const INTERVAL_OPTIONS: { value: CandleInterval; label: string; candleCount: number }[] = [
  { value: "1m", label: "1분", candleCount: 120 },
  { value: "1h", label: "1시간", candleCount: 96 },
  { value: "4h", label: "4시간", candleCount: 84 },
  { value: "1d", label: "일", candleCount: 90 },
  { value: "1w", label: "주", candleCount: 72 },
  { value: "1M", label: "월", candleCount: 48 },
];

const DEFAULT_VISIBLE_COUNT: Record<CandleInterval, number> = {
  "1m": 40,
  "1h": 32,
  "4h": 28,
  "1d": 32,
  "1w": 24,
  "1M": 18,
};

const MIN_VISIBLE_COUNT = 12;
const CHART_WIDTH = 960;
const CHART_HEIGHT = 440;
const PADDING = { top: 20, right: 124, bottom: 42, left: 20 };
const TOOLTIP_WIDTH = 220;
const TOOLTIP_HEIGHT = 138;
const TOOLTIP_OFFSET_X = 10;
const TOOLTIP_OFFSET_Y = 10;

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

function formatAxisLabel(value: number, baseCurrency: string): string {
  return `${getCurrencySymbol(baseCurrency)}${formatPrice(value, baseCurrency)}`;
}

function formatTooltipTime(time: string): string {
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) return "-";
  return new Intl.DateTimeFormat("ko-KR", {
    year: "2-digit",
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(date);
}

function formatTickTime(time: string, interval: CandleInterval): string {
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) return "";

  if (interval === "1m") {
    return new Intl.DateTimeFormat("ko-KR", {
      hour: "2-digit",
      minute: "2-digit",
    }).format(date);
  }

  if (interval === "1h" || interval === "4h") {
    return new Intl.DateTimeFormat("ko-KR", {
      month: "numeric",
      day: "numeric",
      hour: "2-digit",
    }).format(date);
  }

  if (interval === "1M") {
    return new Intl.DateTimeFormat("ko-KR", {
      year: "2-digit",
      month: "numeric",
    }).format(date);
  }

  return new Intl.DateTimeFormat("ko-KR", {
    month: "numeric",
    day: "numeric",
  }).format(date);
}

function getIntervalMs(interval: CandleInterval): number {
  switch (interval) {
    case "1m":
      return 60_000;
    case "1h":
      return 60 * 60_000;
    case "4h":
      return 4 * 60 * 60_000;
    case "1d":
      return 24 * 60 * 60_000;
    case "1w":
      return 7 * 24 * 60 * 60_000;
    case "1M":
      return 30 * 24 * 60 * 60_000;
  }
}

function hashSeed(value: string): number {
  let hash = 0;
  for (let index = 0; index < value.length; index += 1) {
    hash = (hash * 31 + value.charCodeAt(index)) >>> 0;
  }
  return hash || 1;
}

function createSeededRandom(seed: number): () => number {
  let state = seed;
  return () => {
    state = (state * 1664525 + 1013904223) >>> 0;
    return state / 0xffffffff;
  };
}

function interpolateSeries(values: number[], targetLength: number): number[] {
  if (values.length === 0) {
    return Array.from({ length: targetLength }, () => 0);
  }

  if (values.length === 1) {
    return Array.from({ length: targetLength }, () => values[0]);
  }

  return Array.from({ length: targetLength }, (_, index) => {
    const position = (index / Math.max(targetLength - 1, 1)) * (values.length - 1);
    const leftIndex = Math.floor(position);
    const rightIndex = Math.min(values.length - 1, Math.ceil(position));
    const ratio = position - leftIndex;
    const left = values[leftIndex];
    const right = values[rightIndex];
    return left + (right - left) * ratio;
  });
}

function buildMockCandles(
  coin: CoinData,
  interval: CandleInterval,
  candleCount: number,
): CandleItem[] {
  const random = createSeededRandom(hashSeed(`${coin.symbol}-${interval}-${coin.currentPrice}`));
  const baseSeries = interpolateSeries(
    [...coin.sparkline.slice(-24), coin.currentPrice],
    candleCount + 1,
  );
  const now = new Date();
  const intervalMs = getIntervalMs(interval);
  const lastCandleTime = new Date(normalizeCandleTime(now.toISOString(), interval));

  return Array.from({ length: candleCount }, (_, index) => {
    const openBase = baseSeries[index];
    const closeBase = baseSeries[index + 1];
    const amplitude = Math.max(openBase * 0.0045, Math.abs(closeBase - openBase) * 1.15);
    const open = openBase * (1 + (random() - 0.5) * 0.01);
    const close = closeBase * (1 + (random() - 0.5) * 0.01);
    const wickHigh = amplitude * (0.35 + random() * 0.8);
    const wickLow = amplitude * (0.35 + random() * 0.8);

    return {
      time: new Date(
        lastCandleTime.getTime() - (candleCount - index - 1) * intervalMs,
      ).toISOString(),
      open,
      high: Math.max(open, close) + wickHigh,
      low: Math.max(0, Math.min(open, close) - wickLow),
      close,
    };
  });
}

function getDefaultVisibleCount(interval: CandleInterval, totalCount: number): number {
  return Math.min(DEFAULT_VISIBLE_COUNT[interval], totalCount);
}

export function CandleChartPanel({
  exchangeKey,
  baseCurrency,
  coin,
}: CandleChartPanelProps) {
  const chartContainerRef = useRef<HTMLDivElement | null>(null);
  const [interval, setInterval] = useState<CandleInterval>("1d");
  const [candles, setCandles] = useState<CandleItem[]>([]);
  const [visibleCount, setVisibleCount] = useState(DEFAULT_VISIBLE_COUNT["1d"]);
  const [endIndex, setEndIndex] = useState(0);
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const [pointerPosition, setPointerPosition] = useState<{ x: number; y: number } | null>(null);

  const dragStateRef = useRef<{
    startX: number;
    startEndIndex: number;
  } | null>(null);

  const fallbackCandles = useMemo(() => {
    const option = INTERVAL_OPTIONS.find((item) => item.value === interval);
    return buildMockCandles(coin, interval, option?.candleCount ?? 60);
  }, [coin, interval]);

  useEffect(() => {
    setCandles(fallbackCandles);
  }, [fallbackCandles]);

  useEffect(() => {
    let active = true;

    async function loadCandles() {
      const exchangeCode = resolveCandleExchangeCode(exchangeKey);
      if (!exchangeCode) return;

      try {
        const option = INTERVAL_OPTIONS.find((item) => item.value === interval);
        const data = await findCandles({
          exchange: exchangeCode,
          coin: coin.symbol,
          interval,
          limit: option?.candleCount ?? 60,
        });

        if (!active || data.length === 0) return;
        setCandles(data);
      } catch {
        // Keep mock candles when the backend candle API is unavailable.
      }
    }

    void loadCandles();

    return () => {
      active = false;
    };
  }, [coin.symbol, exchangeKey, interval]);

  useEffect(() => {
    const defaultCount = getDefaultVisibleCount(interval, candles.length);
    setVisibleCount(defaultCount);
    setEndIndex(candles.length);
    setHoveredIndex(null);
    setPointerPosition(null);
  }, [candles, interval]);

  const visibleStartIndex = useMemo(() => {
    const safeVisibleCount = Math.min(visibleCount, candles.length);
    return Math.max(0, endIndex - safeVisibleCount);
  }, [candles.length, endIndex, visibleCount]);

  const visibleCandles = useMemo(() => {
    return candles.slice(visibleStartIndex, endIndex);
  }, [candles, endIndex, visibleStartIndex]);

  const chartData = useMemo(() => {
    if (visibleCandles.length === 0) return null;

    const minPrice = Math.min(...visibleCandles.map((candle) => candle.low));
    const maxPrice = Math.max(...visibleCandles.map((candle) => candle.high));
    const range = maxPrice - minPrice || maxPrice * 0.02 || 1;
    const paddedMin = Math.max(0, minPrice - range * 0.08);
    const paddedMax = maxPrice + range * 0.08;
    const plotWidth = CHART_WIDTH - PADDING.left - PADDING.right;
    const plotHeight = CHART_HEIGHT - PADDING.top - PADDING.bottom;
    const slotWidth = plotWidth / visibleCandles.length;
    const candleWidth = Math.max(6, Math.min(16, slotWidth * 0.62));

    const getX = (index: number) => PADDING.left + (index + 0.5) * slotWidth;
    const getY = (price: number) =>
      PADDING.top + (1 - (price - paddedMin) / (paddedMax - paddedMin || 1)) * plotHeight;

    const yTicks = Array.from({ length: 4 }, (_, index) => {
      const value = paddedMax - ((paddedMax - paddedMin) / 3) * index;
      return { value, y: getY(value) };
    });

    const xTickStep = Math.max(1, Math.floor(visibleCandles.length / 4));
    const xTicks = visibleCandles
      .map((candle, index) => ({
        index,
        x: getX(index),
        label: formatTickTime(candle.time, interval),
      }))
      .filter((tick) => tick.index % xTickStep === 0 || tick.index === visibleCandles.length - 1);

    const first = visibleCandles[0];
    const last = visibleCandles[visibleCandles.length - 1];
    const changeRate =
      first.close === 0 ? 0 : ((last.close - first.close) / first.close) * 100;

    const hoverPoints = visibleCandles.map((candle, index) => ({
      ...candle,
      x: getX(index),
      highY: getY(candle.high),
      lowY: getY(candle.low),
      openY: getY(candle.open),
      closeY: getY(candle.close),
      chartIndex: index,
    }));

    return {
      plotWidth,
      slotWidth,
      candleWidth,
      getX,
      getY,
      yTicks,
      xTicks,
      hoverPoints,
      latest: last,
      changeRate,
    };
  }, [interval, visibleCandles]);

  const hoveredCandle =
    chartData && hoveredIndex !== null ? chartData.hoverPoints[hoveredIndex] : null;

  const tooltipLeft = hoveredCandle && pointerPosition
    ? clamp(
        pointerPosition.x + TOOLTIP_OFFSET_X,
        8,
        (chartContainerRef.current?.clientWidth ?? CHART_WIDTH) - TOOLTIP_WIDTH - 8,
      )
    : 12;
  const tooltipTop = hoveredCandle && pointerPosition
    ? clamp(
        pointerPosition.y + TOOLTIP_OFFSET_Y,
        8,
        (chartContainerRef.current?.clientHeight ?? CHART_HEIGHT) - TOOLTIP_HEIGHT - 8,
      )
    : 8;

  function moveViewport(nextEndIndex: number) {
    const safeVisibleCount = Math.min(visibleCount, candles.length);
    const minEndIndex = safeVisibleCount;
    const maxEndIndex = candles.length;
    setEndIndex(clamp(nextEndIndex, minEndIndex, maxEndIndex));
  }

  function zoomTo(nextVisibleCount: number, anchorGlobalIndex?: number) {
    const boundedVisibleCount = clamp(nextVisibleCount, MIN_VISIBLE_COUNT, candles.length);
    const previousVisibleCount = Math.min(visibleCount, candles.length);
    const previousStartIndex = Math.max(0, endIndex - previousVisibleCount);
    const fallbackAnchor = previousStartIndex + Math.floor(previousVisibleCount / 2);
    const anchorIndex = clamp(
      anchorGlobalIndex ?? fallbackAnchor,
      0,
      Math.max(candles.length - 1, 0),
    );
    const ratio =
      previousVisibleCount <= 1 ? 1 : (anchorIndex - previousStartIndex) / previousVisibleCount;
    const nextStartIndex = clamp(
      Math.round(anchorIndex - boundedVisibleCount * ratio),
      0,
      Math.max(candles.length - boundedVisibleCount, 0),
    );

    setVisibleCount(boundedVisibleCount);
    setEndIndex(nextStartIndex + boundedVisibleCount);
    setHoveredIndex(null);
  }

  function handlePointerDown(event: React.PointerEvent<SVGSVGElement>) {
    dragStateRef.current = {
      startX: event.clientX,
      startEndIndex: endIndex,
    };
    event.currentTarget.setPointerCapture(event.pointerId);
  }

  function handlePointerMove(event: React.PointerEvent<SVGSVGElement>) {
    if (!chartData) return;

    const svgRect = event.currentTarget.getBoundingClientRect();
    const containerRect = chartContainerRef.current?.getBoundingClientRect() ?? svgRect;
    const x = ((event.clientX - svgRect.left) / svgRect.width) * CHART_WIDTH;
    const y = ((event.clientY - svgRect.top) / svgRect.height) * CHART_HEIGHT;
    const localX = event.clientX - containerRect.left;
    const localY = event.clientY - containerRect.top;

    if (dragStateRef.current) {
      const deltaX = event.clientX - dragStateRef.current.startX;
      const movedCandles = Math.round(deltaX / Math.max(chartData.slotWidth, 1));
      moveViewport(dragStateRef.current.startEndIndex - movedCandles);
      return;
    }

    const isOutsidePlot =
      x < PADDING.left ||
      x > CHART_WIDTH - PADDING.right ||
      y < PADDING.top ||
      y > CHART_HEIGHT - PADDING.bottom;
    if (isOutsidePlot) {
      setHoveredIndex(null);
      setPointerPosition(null);
      return;
    }

    let nextHoveredIndex: number | null = null;

    chartData.hoverPoints.forEach((candle, index) => {
      const horizontalDistance = Math.abs(candle.x - x);
      if (horizontalDistance > chartData.candleWidth) {
        return;
      }

      const upperY = Math.min(candle.highY, candle.lowY) - 10;
      const lowerY = Math.max(candle.highY, candle.lowY) + 10;
      if (y < upperY || y > lowerY) {
        return;
      }

      nextHoveredIndex = index;
    });

    setHoveredIndex(nextHoveredIndex);
    if (nextHoveredIndex === null) {
      setPointerPosition(null);
      return;
    }

    const containerWidth = containerRect.width;
    const containerHeight = containerRect.height;
    const nextLeft =
      localX + TOOLTIP_OFFSET_X + TOOLTIP_WIDTH > containerWidth - 8
        ? localX - TOOLTIP_WIDTH - TOOLTIP_OFFSET_X
        : localX + TOOLTIP_OFFSET_X;
    const nextTop =
      localY + TOOLTIP_OFFSET_Y + TOOLTIP_HEIGHT > containerHeight - 8
        ? localY - TOOLTIP_HEIGHT - TOOLTIP_OFFSET_Y
        : localY + TOOLTIP_OFFSET_Y;

    setPointerPosition({
      x: clamp(nextLeft, 8, containerWidth - TOOLTIP_WIDTH - 8),
      y: clamp(nextTop, 8, containerHeight - TOOLTIP_HEIGHT - 8),
    });
  }

  function handlePointerUp(event: React.PointerEvent<SVGSVGElement>) {
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    dragStateRef.current = null;
  }

  useEffect(() => {
    const onNativeWheel = (event: WheelEvent) => {
      const container = chartContainerRef.current;
      if (!container || !chartData) return;

      const target = event.target;
      if (!(target instanceof Node) || !container.contains(target)) return;

      event.preventDefault();
      event.stopPropagation();

      const rect = container.getBoundingClientRect();
      const x = ((event.clientX - rect.left) / rect.width) * CHART_WIDTH;
      const hoveredVisibleIndex = clamp(
        Math.floor((x - PADDING.left) / Math.max(chartData.slotWidth, 1)),
        0,
        Math.max(visibleCandles.length - 1, 0),
      );
      const anchorGlobalIndex = visibleStartIndex + hoveredVisibleIndex;

      if (event.ctrlKey || event.metaKey) {
        const nextVisibleCount =
          event.deltaY < 0
            ? Math.max(MIN_VISIBLE_COUNT, visibleCount - 4)
            : Math.min(candles.length, visibleCount + 4);
        zoomTo(nextVisibleCount, anchorGlobalIndex);
        return;
      }

      const dominantDelta =
        Math.abs(event.deltaX) > Math.abs(event.deltaY) ? event.deltaX : event.deltaY;
      const movedCandles = Math.round(dominantDelta / Math.max(chartData.slotWidth * 0.8, 1));
      moveViewport(endIndex + movedCandles);
    };

    document.addEventListener("wheel", onNativeWheel, {
      passive: false,
      capture: true,
    });
    return () => {
      document.removeEventListener("wheel", onNativeWheel, {
        capture: true,
      });
    };
  }, [
    candles.length,
    chartData,
    endIndex,
    visibleCandles.length,
    visibleCount,
    visibleStartIndex,
  ]);

  if (!chartData) {
    return null;
  }

  return (
    <section className="overflow-hidden rounded-xl border border-border bg-card">
      <div className="border-b border-border/50 px-5 py-5 sm:px-6">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
          <div className="space-y-2">
            <div className="flex flex-wrap items-end gap-2">
              <h2 className="text-4xl font-bold tracking-tight">
                {coin.symbol}
                <span className="ml-2 text-2xl font-semibold text-muted-foreground">
                  / {baseCurrency}
                </span>
              </h2>
              <span className="pb-1 text-base font-medium text-muted-foreground">
                {coin.name}
              </span>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <p className="font-mono text-4xl font-bold tabular-nums">
                {getCurrencySymbol(baseCurrency)}
                {formatPrice(coin.currentPrice, baseCurrency)}
              </p>
              <span
                className={cn(
                  "rounded-full px-3 py-1.5 text-sm font-bold",
                  coin.changeRate > 0 && "bg-positive/15 text-positive",
                  coin.changeRate < 0 && "bg-negative/15 text-negative",
                  coin.changeRate === 0 && "bg-secondary text-muted-foreground",
                )}
              >
                {formatChangeRate(coin.changeRate)}
              </span>
            </div>
          </div>

          <div className="flex min-w-0 flex-col gap-2 xl:items-end">
            <div className="overflow-x-auto">
              <div className="flex min-w-max flex-nowrap gap-2 pb-1">
              {INTERVAL_OPTIONS.map((option) => (
                <button
                  key={option.value}
                  onClick={() => setInterval(option.value)}
                  className={cn(
                    "rounded-full border px-4 py-2 text-sm font-semibold transition-all",
                    interval === option.value
                      ? "border-primary bg-primary text-primary-foreground"
                      : "border-border/70 bg-white text-muted-foreground hover:border-primary/30 hover:text-foreground",
                  )}
                >
                  {option.label}
                </button>
              ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="px-4 py-4 sm:px-6 sm:py-6">
        <div
          ref={chartContainerRef}
          className="relative overflow-hidden rounded-[24px] border border-border/60 bg-white"
          style={{ overscrollBehavior: "contain" }}
        >
          <svg
            viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
            className="block w-full touch-none"
            style={{ touchAction: "none" }}
            onPointerDown={handlePointerDown}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerUp}
            onPointerLeave={() => {
              dragStateRef.current = null;
              setHoveredIndex(null);
              setPointerPosition(null);
            }}
          >
            {chartData.yTicks.map((tick) => (
              <g key={tick.value}>
                <line
                  x1={PADDING.left}
                  y1={tick.y}
                  x2={CHART_WIDTH - PADDING.right}
                  y2={tick.y}
                  stroke="var(--border)"
                  strokeWidth={1}
                  strokeDasharray="4 6"
                />
                <text
                  x={CHART_WIDTH - 12}
                  y={tick.y + 4}
                  textAnchor="end"
                  fill="var(--muted-foreground)"
                  fontSize="13"
                >
                  {formatAxisLabel(tick.value, baseCurrency)}
                </text>
              </g>
            ))}

            {chartData.xTicks.map((tick) => (
              <text
                key={`${tick.index}-${tick.label}`}
                x={tick.x}
                y={CHART_HEIGHT - 12}
                textAnchor="middle"
                fill="var(--muted-foreground)"
                fontSize="12"
              >
                {tick.label}
              </text>
            ))}

            {visibleCandles.map((candle, index) => {
              const x = chartData.getX(index);
              const openY = chartData.getY(candle.open);
              const closeY = chartData.getY(candle.close);
              const highY = chartData.getY(candle.high);
              const lowY = chartData.getY(candle.low);
              const bodyTop = Math.min(openY, closeY);
              const bodyHeight = Math.max(2, Math.abs(closeY - openY));
              const isUp = candle.close >= candle.open;

              return (
                <g key={`${candle.time}-${index}`}>
                  <line
                    x1={x}
                    y1={highY}
                    x2={x}
                    y2={lowY}
                    stroke={isUp ? "var(--positive)" : "var(--negative)"}
                    strokeWidth={1.6}
                    strokeLinecap="round"
                  />
                  <rect
                    x={x - chartData.candleWidth / 2}
                    y={bodyTop}
                    width={chartData.candleWidth}
                    height={bodyHeight}
                    rx={2}
                    fill={isUp ? "var(--positive)" : "var(--negative)"}
                    opacity={hoveredIndex === index ? 1 : 0.9}
                  />
                </g>
              );
            })}

            {hoveredCandle && (
              <>
                <line
                  x1={hoveredCandle.x}
                  y1={PADDING.top}
                  x2={hoveredCandle.x}
                  y2={CHART_HEIGHT - PADDING.bottom}
                  stroke="var(--foreground)"
                  strokeOpacity="0.24"
                  strokeWidth={1}
                  strokeDasharray="4 4"
                />
                <line
                  x1={PADDING.left}
                  y1={chartData.getY(hoveredCandle.close)}
                  x2={CHART_WIDTH - PADDING.right}
                  y2={chartData.getY(hoveredCandle.close)}
                  stroke="var(--foreground)"
                  strokeOpacity="0.16"
                  strokeWidth={1}
                  strokeDasharray="4 4"
                />
              </>
            )}
          </svg>

          {hoveredCandle && (
            <div
              className="pointer-events-none absolute z-10 w-[220px] rounded-xl bg-foreground/92 px-3 py-2.5 text-sm text-white shadow-xl"
              style={{
                left: `${tooltipLeft}px`,
                top: `${tooltipTop}px`,
              }}
            >
              <p className="font-semibold">{formatTooltipTime(hoveredCandle.time)}</p>
              <div className="mt-2 space-y-0.5 font-mono">
                <div className="grid grid-cols-[44px_minmax(0,1fr)] items-center gap-2">
                  <span className="text-white/70">시가</span>
                  <span className="text-right">{formatAxisLabel(hoveredCandle.open, baseCurrency)}</span>
                </div>
                <div className="grid grid-cols-[44px_minmax(0,1fr)] items-center gap-2">
                  <span className="text-white/70">고가</span>
                  <span className="text-right">{formatAxisLabel(hoveredCandle.high, baseCurrency)}</span>
                </div>
                <div className="grid grid-cols-[44px_minmax(0,1fr)] items-center gap-2">
                  <span className="text-white/70">저가</span>
                  <span className="text-right">{formatAxisLabel(hoveredCandle.low, baseCurrency)}</span>
                </div>
                <div className="grid grid-cols-[44px_minmax(0,1fr)] items-center gap-2">
                  <span className="text-white/70">종가</span>
                  <span className="text-right">{formatAxisLabel(hoveredCandle.close, baseCurrency)}</span>
                </div>
              </div>
            </div>
          )}
        </div>

      </div>
    </section>
  );
}
