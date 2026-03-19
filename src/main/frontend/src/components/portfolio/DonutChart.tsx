import { useState } from "react";
import { formatCurrency } from "@/lib/formatters";
import { getCoinColor } from "@/lib/types/coins";
import type { HoldingData } from "@/lib/types/portfolio";

interface DonutChartProps {
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

function buildSegments(holdings: HoldingData[]): Segment[] {
  const total = holdings.reduce((sum, h) => sum + h.currentPrice * h.quantity, 0);
  if (total === 0) return [];

  const items = holdings
    .map((h) => ({
      label: h.coinSymbol,
      value: h.currentPrice * h.quantity,
      ratio: (h.currentPrice * h.quantity) / total,
      color: getCoinColor(h.coinSymbol),
    }))
    .sort((a, b) => b.value - a.value);

  if (items.length <= 7) return items;

  const top = items.slice(0, 6);
  const rest = items.slice(6);
  const otherValue = rest.reduce((s, r) => s + r.value, 0);
  top.push({
    label: "기타",
    value: otherValue,
    ratio: otherValue / total,
    color: OTHER_COLOR,
  });
  return top;
}

export function DonutChart({ holdings, baseCurrency }: DonutChartProps) {
  const [hoveredLabel, setHoveredLabel] = useState<string | null>(null);
  const totalEval = holdings.reduce((sum, h) => sum + h.currentPrice * h.quantity, 0);
  const segments = buildSegments(holdings);

  const size = 180;
  const strokeWidth = 28;
  const hoveredStrokeWidth = 34;
  const radius = (size - hoveredStrokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const cx = size / 2;
  const cy = size / 2;

  let accumulated = 0;

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
            {segments.map((seg) => {
              const dashLength = circumference * seg.ratio;
              const dashOffset = circumference * (0.25 - accumulated);
              accumulated += seg.ratio;
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
            <span className="text-[10px] font-medium text-muted-foreground">총 평가</span>
            <span className="font-mono text-sm font-bold tabular-nums">
              {formatCurrency(totalEval, baseCurrency)}
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
