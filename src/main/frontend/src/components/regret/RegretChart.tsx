import { useState, useMemo, useCallback } from "react";
import { cn } from "@/lib/utils";
import type { RegretSummary, AssetSnapshot, ViolationMarker } from "@/lib/types/regret";
import { getTickInterval } from "@/lib/types/regret";

interface RegretChartProps {
  summary: RegretSummary;
  snapshots: AssetSnapshot[];
  markers: ViolationMarker[];
  simulationLine: number[];      // 활성 규칙 조합 시뮬레이션
  btcHoldValues: number[] | null; // null이면 비활성
  hasEnabledRules: boolean;
  totalDays: number;
}

function formatKRWShort(value: number): string {
  const abs = Math.abs(value);
  if (abs >= 1_0000_0000) {
    const eok = Math.floor(abs / 1_0000_0000);
    const man = Math.round((abs % 1_0000_0000) / 1_0000);
    if (man > 0) return `${eok}억${man.toLocaleString("ko-KR")}만`;
    return `${eok}억`;
  }
  if (abs >= 1_0000) return `${Math.round(abs / 1_0000).toLocaleString("ko-KR")}만`;
  return abs.toLocaleString("ko-KR");
}

const W = 700;
const H = 280;
const PAD = { top: 20, right: 20, bottom: 32, left: 60 };

export function RegretChart({
  summary,
  snapshots,
  markers,
  simulationLine,
  btcHoldValues,
  hasEnabledRules,
  totalDays,
}: RegretChartProps) {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  const chartData = useMemo(() => {
    if (snapshots.length < 2) return null;

    const allValues = [
      ...snapshots.map((s) => s.actual),
      ...simulationLine,
      ...(btcHoldValues ?? []),
    ];
    const minVal = Math.min(...allValues);
    const maxVal = Math.max(...allValues);
    const valPad = (maxVal - minVal) * 0.1 || 1;
    const yMin = minVal - valPad;
    const yMax = maxVal + valPad;

    const plotW = W - PAD.left - PAD.right;
    const plotH = H - PAD.top - PAD.bottom;
    const n = snapshots.length;

    const getX = (i: number) => PAD.left + (i / (n - 1)) * plotW;
    const getY = (val: number) => PAD.top + (1 - (val - yMin) / (yMax - yMin)) * plotH;

    const toPath = (values: number[]) =>
      values.map((v, i) => `${i === 0 ? "M" : "L"}${getX(i).toFixed(1)},${getY(v).toFixed(1)}`).join(" ");

    const actualPath = toPath(snapshots.map((s) => s.actual));
    const simPath = toPath(simulationLine);
    const btcPath = btcHoldValues ? toPath(btcHoldValues) : null;

    // Y축 눈금
    const yTicks = Array.from({ length: 5 }, (_, i) => {
      const val = yMin + ((yMax - yMin) / 4) * i;
      return { value: Math.round(val), y: getY(val) };
    });

    // X축 라벨 — 시즌 기간에 따라 adaptive 간격
    const tickInterval = getTickInterval(totalDays);
    const xLabels: { date: string; x: number }[] = [];
    for (let i = 0; i < n; i += tickInterval) {
      xLabels.push({ date: snapshots[i].date, x: getX(i) });
    }
    // 마지막 날짜가 빠지면 추가 (단, 직전 라벨과 너무 가까우면 생략)
    const lastLabel = xLabels[xLabels.length - 1];
    if (lastLabel?.date !== snapshots[n - 1].date && n - 1 - (xLabels.length - 1) * tickInterval > tickInterval / 2) {
      xLabels.push({ date: snapshots[n - 1].date, x: getX(n - 1) });
    }

    // 위반 마커 — snapshot index 기준으로 매핑
    const markerByIndex = new Map<number, "loss" | "gain">();
    const markerPoints: { x: number; y: number; type: "loss" | "gain" }[] = [];
    for (const m of markers) {
      const idx = snapshots.findIndex((s) => s.date === m.date);
      if (idx === -1) continue;
      markerByIndex.set(idx, m.type);
      markerPoints.push({ x: getX(idx), y: getY(m.value), type: m.type });
    }

    // 호버
    const hoverPoints = snapshots.map((s, i) => ({
      x: getX(i),
      actualY: getY(s.actual),
      simY: getY(simulationLine[i]),
      violation: markerByIndex.get(i) ?? null,
    }));

    return { actualPath, simPath, btcPath, yTicks, xLabels, markerPoints, hoverPoints };
  }, [snapshots, markers, simulationLine, btcHoldValues]);

  const handleMouseMove = useCallback(
    (e: React.MouseEvent<SVGSVGElement>) => {
      if (!chartData) return;
      const rect = e.currentTarget.getBoundingClientRect();
      const mouseX = ((e.clientX - rect.left) / rect.width) * W;
      let closest = 0;
      let minDist = Infinity;
      chartData.hoverPoints.forEach((pt, i) => {
        const d = Math.abs(pt.x - mouseX);
        if (d < minDist) { minDist = d; closest = i; }
      });
      setHoveredIndex(closest);
    },
    [chartData],
  );

  return (
    <div className="rounded-xl border border-border bg-card p-5 sm:p-6">
      {/* 상단 요약 */}
      <div className="mb-5">
        <p className="text-xs font-medium text-muted-foreground">놓친 수익</p>
        <p className="mt-1 font-mono text-3xl font-bold tabular-nums text-negative">
          {summary.missedProfit.toLocaleString("ko-KR")}
          <span className="ml-2 text-base font-bold text-muted-foreground">KRW</span>
        </p>
      </div>

      {/* 3-stat 카드 */}
      <div className="mb-6 grid grid-cols-3 gap-3">
        <div className="rounded-xl bg-secondary/50 px-3 py-3">
          <p className="text-[11px] font-medium text-muted-foreground">실제</p>
          <p className={cn(
            "mt-1 font-mono text-lg font-bold tabular-nums",
            summary.actualProfitRate >= 0 ? "text-positive" : "text-negative",
          )}>
            {summary.actualProfitRate >= 0 ? "+" : ""}{summary.actualProfitRate}%
          </p>
        </div>
        <div className="rounded-xl bg-secondary/50 px-3 py-3">
          <p className="text-[11px] font-medium text-muted-foreground">규칙 준수 시</p>
          <p className={cn(
            "mt-1 font-mono text-lg font-bold tabular-nums",
            summary.ruleFollowedProfitRate >= 0 ? "text-positive" : "text-negative",
          )}>
            {summary.ruleFollowedProfitRate >= 0 ? "+" : ""}{summary.ruleFollowedProfitRate}%
          </p>
        </div>
        <div className="rounded-xl bg-secondary/50 px-3 py-3">
          <p className="text-[11px] font-medium text-muted-foreground">위반</p>
          <p className="mt-1 font-mono text-lg font-bold tabular-nums">
            {summary.totalViolations}<span className="text-sm font-bold">건</span>
          </p>
        </div>
      </div>

      {/* 차트 */}
      {chartData && (
        <div className="relative">
          <svg
            viewBox={`0 0 ${W} ${H}`}
            className="w-full"
            onMouseMove={handleMouseMove}
            onMouseLeave={() => setHoveredIndex(null)}
          >
            {/* Y축 그리드 */}
            {chartData.yTicks.map((tick) => (
              <g key={tick.value}>
                <line
                  x1={PAD.left} y1={tick.y}
                  x2={W - PAD.right} y2={tick.y}
                  stroke="var(--border)" strokeWidth={0.5} strokeDasharray="4 3"
                />
                <text
                  x={PAD.left - 8} y={tick.y + 1}
                  textAnchor="end" dominantBaseline="middle"
                  fill="var(--muted-foreground)" fontSize={10} fontFamily="inherit"
                >
                  {formatKRWShort(tick.value)}
                </text>
              </g>
            ))}

            {/* X축 라벨 */}
            {chartData.xLabels.map((l) => (
              <text
                key={l.date} x={l.x} y={H - 6}
                textAnchor="middle" fill="var(--muted-foreground)" fontSize={10} fontFamily="inherit"
              >
                {l.date}
              </text>
            ))}

            {/* BTC 벤치마크 */}
            {chartData.btcPath && (
              <path
                d={chartData.btcPath}
                fill="none" stroke="#f7931a" strokeWidth={1.5}
                strokeLinecap="round" opacity={0.5}
              />
            )}

            {/* 시뮬레이션 라인 (점선) — 규칙 1개 이상 활성일 때만 */}
            {hasEnabledRules && (
              <path
                d={chartData.simPath}
                fill="none" stroke="var(--negative)" strokeWidth={1.8}
                strokeDasharray="6 4" strokeLinecap="round"
              />
            )}

            {/* 실제 라인 */}
            <path
              d={chartData.actualPath}
              fill="none" stroke="var(--primary)" strokeWidth={2.2}
              strokeLinecap="round" strokeLinejoin="round"
            />

            {/* 위반 마커 — 작은 점 */}
            {chartData.markerPoints.map((pt, i) => (
              <circle
                key={i}
                cx={pt.x} cy={pt.y} r={3}
                fill={pt.type === "loss" ? "var(--negative)" : "var(--warning)"}
                stroke="white" strokeWidth={1.5}
              />
            ))}

            {/* 호버 */}
            {hoveredIndex !== null && chartData.hoverPoints[hoveredIndex] && (
              <g>
                <line
                  x1={chartData.hoverPoints[hoveredIndex].x} y1={PAD.top}
                  x2={chartData.hoverPoints[hoveredIndex].x} y2={H - PAD.bottom}
                  stroke="var(--muted-foreground)" strokeWidth={0.5} strokeDasharray="3 2" opacity={0.5}
                />
                <circle
                  cx={chartData.hoverPoints[hoveredIndex].x}
                  cy={chartData.hoverPoints[hoveredIndex].actualY}
                  r={4} fill="var(--primary)" stroke="white" strokeWidth={2}
                />
                {hasEnabledRules && (
                  <circle
                    cx={chartData.hoverPoints[hoveredIndex].x}
                    cy={chartData.hoverPoints[hoveredIndex].simY}
                    r={4} fill="var(--negative)" stroke="white" strokeWidth={2}
                  />
                )}
              </g>
            )}
          </svg>

          {/* 툴팁 */}
          {hoveredIndex !== null && snapshots[hoveredIndex] && (() => {
            const pct = (chartData.hoverPoints[hoveredIndex].x / W) * 100;
            const nearRight = pct > 75;
            return (
              <div
                className="pointer-events-none absolute top-0 z-10 whitespace-nowrap rounded-lg bg-foreground/90 px-3 py-2 text-xs text-white shadow-lg backdrop-blur-sm"
                style={{
                  left: `${pct}%`,
                  transform: nearRight ? "translateX(-100%)" : "translateX(0%)",
                }}
              >
                <p className="mb-1 font-semibold">{snapshots[hoveredIndex].fullDate}</p>
                <p>
                  <span className="mr-1.5 inline-block h-1.5 w-1.5 rounded-full bg-primary" />
                  실제: {formatKRWShort(snapshots[hoveredIndex].actual)}
                </p>
                {hasEnabledRules && (
                  <p>
                    <span className="mr-1.5 inline-block h-1.5 w-1.5 rounded-full bg-negative" />
                    시뮬레이션: {formatKRWShort(simulationLine[hoveredIndex])}
                  </p>
                )}
                {btcHoldValues && (
                  <p>
                    <span className="mr-1.5 inline-block h-1.5 w-1.5 rounded-full" style={{ backgroundColor: "#f7931a" }} />
                    BTC 홀드: {formatKRWShort(btcHoldValues[hoveredIndex])}
                  </p>
                )}
                {chartData.hoverPoints[hoveredIndex].violation && (
                  <p className={cn(
                    "mt-1 border-t border-white/20 pt-1 font-semibold",
                    chartData.hoverPoints[hoveredIndex].violation === "loss" ? "text-red-300" : "text-amber-300",
                  )}>
                    {chartData.hoverPoints[hoveredIndex].violation === "loss" ? "규칙 위반 (손실)" : "규칙 위반 (수익)"}
                  </p>
                )}
              </div>
            );
          })()}
        </div>
      )}

      {/* 범례 */}
      <div className="mt-4 flex flex-wrap items-center gap-x-5 gap-y-1.5 text-xs text-muted-foreground">
        <div className="flex items-center gap-1.5">
          <span className="inline-block h-0.5 w-4 rounded-full bg-primary" />
          <span>실제</span>
        </div>
        {hasEnabledRules && (
          <div className="flex items-center gap-1.5">
            <span className="inline-block h-[3px] w-4 border-b-2 border-dashed border-negative" />
            <span>규칙 준수 시뮬레이션</span>
          </div>
        )}
        {btcHoldValues && (
          <div className="flex items-center gap-1.5">
            <span className="inline-block h-0.5 w-4 rounded-full" style={{ backgroundColor: "#f7931a", opacity: 0.5 }} />
            <span>BTC 홀드</span>
          </div>
        )}
        <div className="flex items-center gap-1.5">
          <span className="inline-block h-2 w-2 rounded-full bg-negative" />
          <span>위반 지점</span>
        </div>
      </div>
    </div>
  );
}
