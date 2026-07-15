/** 복기 화면(자산 추이)을 본뜬 정적 미리보기. 실데이터가 아니므로 '예시'임을 표기한다. */
export function RegretShowcase() {
  return (
    <div className="relative">
      <div className="rounded-2xl border border-border bg-card p-5 sm:p-6">
        {/* 놓친 수익 헤더 */}
        <div>
          <p className="text-[11px] font-medium text-muted-foreground">UPBIT · KRW</p>
          <p className="mt-2 text-[13px] font-medium text-muted-foreground">놓친 수익</p>
          <p className="font-mono text-4xl font-extrabold tabular-nums text-negative">33만원</p>
          <p className="mt-1.5 text-[13px] text-muted-foreground">
            규칙을 지켰다면 이만큼 더 벌었습니다.
          </p>
        </div>

        {/* 실제 / 규칙 준수 / 위반 */}
        <div className="mt-4 grid grid-cols-3 gap-2 border-t border-border/60 pt-4">
          <div>
            <p className="text-[11px] text-muted-foreground">실제</p>
            <p className="mt-0.5 text-lg font-extrabold text-negative">-4.80%</p>
          </div>
          <div>
            <p className="text-[11px] text-muted-foreground">규칙 준수 시</p>
            <p className="mt-0.5 text-lg font-extrabold text-positive">+1.70%</p>
          </div>
          <div>
            <p className="text-[11px] text-muted-foreground">위반</p>
            <p className="mt-0.5 text-lg font-extrabold text-negative">3건</p>
          </div>
        </div>

        {/* 자산 추이 차트 */}
        <div className="mt-5 rounded-xl bg-secondary/40 p-3">
          <div className="flex items-baseline justify-between px-1">
            <p className="text-sm font-bold">자산 추이</p>
            <p className="text-[11px] text-muted-foreground">분석 구간 7/1 ~ 7/15</p>
          </div>

          <svg
            viewBox="0 0 370 224"
            className="mt-2 h-auto w-full"
            role="img"
            aria-label="실제·규칙 준수·BTC 홀드 자산 추이 비교 예시"
          >
            {/* 가로 격자 + y축 라벨 */}
            {[
              [48, "522만"],
              [92, "505만"],
              [138, "487만"],
              [182, "470만"],
            ].map(([y, label]) => (
              <g key={label}>
                <line x1="44" y1={y} x2="354" y2={y} stroke="#E8E6E1" strokeWidth="1" />
                <text x="38" y={Number(y) + 3} textAnchor="end" fontSize="9" fill="#9A96A8">
                  {label}
                </text>
              </g>
            ))}

            {/* x축 라벨 */}
            {[
              [50, "7/1"],
              [200, "7/8"],
              [350, "7/15"],
            ].map(([x, label]) => (
              <text key={label} x={x} y="214" textAnchor="middle" fontSize="9" fill="#9A96A8">
                {label}
              </text>
            ))}

            {/* BTC 홀드 */}
            <polyline
              points="50,95 93,53 136,41 179,79 221,46 286,87 350,46"
              fill="none"
              stroke="#F0A030"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            {/* 규칙 준수 시뮬레이션 */}
            <polyline
              points="50,97 71,74 114,156 157,131 200,92 221,133 264,172 307,120 350,146"
              fill="none"
              stroke="#E85D75"
              strokeWidth="2.5"
              strokeDasharray="6 5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            {/* 실제 */}
            <polyline
              points="50,97 71,74 114,161 157,136 200,100 221,141 264,182 307,131 350,167"
              fill="none"
              stroke="#6C5CE7"
              strokeWidth="2.8"
              strokeLinecap="round"
              strokeLinejoin="round"
            />

            {/* 위반 지점 */}
            {[
              [114, 161],
              [221, 141],
              [307, 131],
            ].map(([cx, cy]) => (
              <circle key={cx} cx={cx} cy={cy} r="4.5" fill="#E85D75" stroke="#fff" strokeWidth="2" />
            ))}
          </svg>

          {/* 범례 */}
          <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 px-1 text-[10px] font-semibold text-muted-foreground">
            <span className="flex items-center gap-1">
              <span className="h-0.5 w-3.5 rounded bg-[#6C5CE7]" />실제
            </span>
            <span className="flex items-center gap-1">
              <span className="h-0.5 w-3.5 rounded bg-[#E85D75]" />규칙 준수 시뮬레이션
            </span>
            <span className="flex items-center gap-1">
              <span className="h-0.5 w-3.5 rounded bg-[#F0A030]" />BTC 홀드
            </span>
            <span className="flex items-center gap-1">
              <span className="h-1.5 w-1.5 rounded-full bg-[#E85D75]" />위반 지점
            </span>
          </div>
        </div>
      </div>

      {/* 뒤에 깔리는 장식 카드 — 겹쳐 쌓인 복기 리포트 느낌 */}
      <div className="absolute -bottom-3 left-4 right-4 -z-10 h-16 rotate-[1.5deg] rounded-2xl border border-border bg-card/60" />
    </div>
  );
}
