// trypto 투자 복기 그래프 조회 부하 테스트 — 읽기 전용 GET /api/rounds/{roundId}/regret/chart.
//
// 한 iteration = 한 라운드의 복기 그래프 데이터 1요청. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
// roundId 는 1..REGRET_ROUNDS 에서 매번 랜덤으로 뽑는다 — 특정 라운드 한 행만 buffer pool 에
// hot 하게 박히는 비현실적 측정을 피한다. base 시드상 userId == roundId == n 이라
// userId 도 같은 n 을 넣어 소유자 검증(ROUND_ACCESS_DENIED)을 통과시킨다.
// exchangeId 는 1(UPBIT) 고정 — regret 시드가 exchange 1 기준으로만 깔린다.
//
// 조회 경로: 라운드 소유자 검증 → regret_report 존재 검증 → violation_detail 조회 → exchange 메타 →
//   portfolio_snapshot 일별 조회(자산 추이) → 누적손실/위반마커 계산 → BTC 벤치마크.
// BTC 벤치마크는 InfluxDB 의 라운드 시작일 BTC 가격이 없으면 빈 벤치마크(0)로 떨어져 200 을 막지 않는다.
// 따라서 MySQL 시드만으로 200 이 나온다. BTC 라인을 채우려면 candle_1h(BTC) InfluxDB 시드가 별도로 필요하다.
// regret 시드(regret.sql.tmpl)가 report + portfolio_snapshot 을 미리 깔아둬야 200 이 나온다.
//
// SLO: 실패 0, p99 < 100ms (배치 산출물 + 일별 스냅샷 조회 경로).

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const chartLatency = new Trend('regret_chart_latency', true);
const chartFailed  = new Rate('regret_chart_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '100', 10); // 초당 요청수(TPS)

// 시드와 맞춘다. reset.sh 의 REGRET_ROUNDS 와 동일해야 한다.
const REGRET_ROUNDS = Number.parseInt(__ENV.REGRET_ROUNDS || '1000', 10);
const EXCHANGE_ID   = 1;

function pickRoundId() {
  return Math.floor(Math.random() * REGRET_ROUNDS) + 1; // 1 .. REGRET_ROUNDS
}

export const options = {
  scenarios: {
    regret_chart: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'queryChart',
    },
  },
  thresholds: {
    'http_req_failed{scenario:regret_chart}':   ['rate==0'],
    'http_req_duration{scenario:regret_chart}': ['p(99)<100'],
  },
};

export function queryChart() {
  const roundId = pickRoundId();
  const userId = roundId; // base 시드: userId == roundId == n

  const url =
    `${API}/api/rounds/${roundId}/regret/chart?userId=${userId}&exchangeId=${EXCHANGE_ID}`;

  const res = http.get(url, { tags: { scenario: 'regret_chart' } });

  chartLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  chartFailed.add(!ok);
  check(res, { 'regret chart ok': () => ok });
}
