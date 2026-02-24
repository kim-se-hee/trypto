import { cn } from "@/lib/utils";

interface SparklineProps {
  data: number[];
  width?: number;
  height?: number;
  positive?: boolean;
  className?: string;
}

export function Sparkline({ data, width = 80, height = 28, positive, className }: SparklineProps) {
  if (data.length < 2) return null;

  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;

  const padding = 1;
  const innerW = width - padding * 2;
  const innerH = height - padding * 2;

  const points = data
    .map((v, i) => {
      const x = padding + (i / (data.length - 1)) * innerW;
      const y = padding + innerH - ((v - min) / range) * innerH;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(" ");

  const isUp = positive ?? data[data.length - 1] >= data[0];
  const strokeColor = isUp ? "var(--positive)" : "var(--negative)";
  const fillColor = isUp ? "var(--positive)" : "var(--negative)";

  const lastPoint = data[data.length - 1];
  const lastX = padding + innerW;
  const lastY = padding + innerH - ((lastPoint - min) / range) * innerH;

  const areaPoints = `${padding.toFixed(1)},${(padding + innerH).toFixed(1)} ${points} ${lastX.toFixed(1)},${(padding + innerH).toFixed(1)}`;

  return (
    <svg
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      className={cn("shrink-0", className)}
    >
      <polygon
        points={areaPoints}
        fill={fillColor}
        opacity="0.12"
      />
      <polyline
        points={points}
        fill="none"
        stroke={strokeColor}
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle
        cx={lastX}
        cy={lastY}
        r="1.5"
        fill={strokeColor}
      />
    </svg>
  );
}
