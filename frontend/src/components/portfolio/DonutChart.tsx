import { useState } from "react";
import { formatCurrency } from "@/lib/formatters";
import { getCoinColor } from "@/lib/types/coins";
import type { HoldingData } from "@/lib/types/portfolio";

interface DonutChartProps {
  availableCash: number;
  holdings: HoldingData[];
  baseCurrency: string;
}

interface Segment {
  label: string;
  value: number;
  ratio: number;
  color: string;
}

const OTHER_COLOR = "#8b949e";
const CASH_COLOR = "#c2b8ab";

/** 보유 현금은 코인이 하나도 없어도 항상 한 조각으로 남는다. */
function buildSegments(
  availableCash: number,
  holdings: HoldingData[],
  baseCurrency: string,
): Segment[] {
  const total =
    availableCash + holdings.reduce((sum, h) => sum + h.currentPrice * h.quantity, 0);
  if (total === 0) return [];

  const coins = holdings
    .map((h) => ({
      label: h.coinSymbol,
      value: h.currentPrice * h.quantity,
      ratio: (h.currentPrice * h.quantity) / total,
      color: getCoinColor(h.coinSymbol),
    }))
    .filter((c) => c.value > 0)
    .sort((a, b) => b.value - a.value);

  const shown = coins.length <= 6 ? coins : coins.slice(0, 5);
  const rest = coins.slice(shown.length);
  if (rest.length > 0) {
    const otherValue = rest.reduce((s, r) => s + r.value, 0);
    shown.push({
      label: "기타",
      value: otherValue,
      ratio: otherValue / total,
      color: OTHER_COLOR,
    });
  }

  if (availableCash <= 0) return shown;

  return [
    {
      label: baseCurrency,
      value: availableCash,
      ratio: availableCash / total,
      color: CASH_COLOR,
    },
    ...shown,
  ];
}

export function DonutChart({ availableCash, holdings, baseCurrency }: DonutChartProps) {
  const [hoveredLabel, setHoveredLabel] = useState<string | null>(null);
  const totalAsset =
    availableCash + holdings.reduce((sum, h) => sum + h.currentPrice * h.quantity, 0);
  const segments = buildSegments(availableCash, holdings, baseCurrency);

  const size = 180;
  const strokeWidth = 28;
  const hoveredStrokeWidth = 34;
  const radius = (size - hoveredStrokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const cx = size / 2;
  const cy = size / 2;

  // 조각이 시작하는 위치는 앞선 조각들의 비율 합이다. 그리면서 변수를 누적하면
  // 렌더 도중 값을 바꾸는 셈이라, 그리기 전에 시작 위치를 미리 계산해 둔다. 조각은 많아야 7개다.
  const arcs = segments.map((seg, i) => ({
    ...seg,
    start: segments.slice(0, i).reduce((sum, prev) => sum + prev.ratio, 0),
  }));

  return (
    <div className="rounded-xl border border-border bg-card p-5">
      <p className="mb-4 text-xs font-medium text-muted-foreground">자산 구성</p>

      <div className="flex justify-center">
        <div className="relative">
          <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
            {/* Background circle */}
            <circle
              cx={cx}
              cy={cy}
              r={radius}
              fill="none"
              stroke="var(--secondary)"
              strokeWidth={strokeWidth}
            />
            {/* Segments */}
            {arcs.map((seg) => {
              const dashLength = circumference * seg.ratio;
              const dashOffset = circumference * (0.25 - seg.start);
              const isHovered = hoveredLabel === seg.label;
              const isOtherHovered = hoveredLabel !== null && hoveredLabel !== seg.label;
              return (
                <circle
                  key={seg.label}
                  cx={cx}
                  cy={cy}
                  r={radius}
                  fill="none"
                  stroke={seg.color}
                  strokeWidth={isHovered ? hoveredStrokeWidth : strokeWidth}
                  strokeDasharray={`${dashLength} ${circumference - dashLength}`}
                  strokeDashoffset={dashOffset}
                  strokeLinecap="butt"
                  opacity={isOtherHovered ? 0.4 : 1}
                  className="transition-all duration-300"
                  onMouseEnter={() => setHoveredLabel(seg.label)}
                  onMouseLeave={() => setHoveredLabel(null)}
                />
              );
            })}
          </svg>
          {/* Center label */}
          <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
            <span className="text-[10px] font-medium text-muted-foreground">총 자산</span>
            <span className="font-mono text-sm font-bold tabular-nums">
              {formatCurrency(totalAsset, baseCurrency)}
            </span>
          </div>
        </div>
      </div>

      {/* Legend */}
      <div className="mt-4 space-y-1.5">
        {segments.map((seg) => {
          const isHovered = hoveredLabel === seg.label;
          const isOtherHovered = hoveredLabel !== null && hoveredLabel !== seg.label;
          return (
            <div
              key={seg.label}
              className={`flex cursor-default items-center justify-between rounded-md px-1.5 py-0.5 text-xs transition-all duration-300 ${
                isHovered ? "bg-secondary/60" : ""
              } ${isOtherHovered ? "opacity-40" : "opacity-100"}`}
              onMouseEnter={() => setHoveredLabel(seg.label)}
              onMouseLeave={() => setHoveredLabel(null)}
            >
              <div className="flex items-center gap-2">
                <span
                  className="inline-block h-2.5 w-2.5 rounded-full"
                  style={{ backgroundColor: seg.color }}
                />
                <span className="font-medium">{seg.label}</span>
              </div>
              <span className="font-mono tabular-nums text-muted-foreground">
                {(seg.ratio * 100).toFixed(1)}%
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
