// trypto 랭킹 통계 조회 부하 테스트 — 읽기 전용 GET /api/rankings/stats.
//
// 한 iteration = 한 기간의 랭킹 통계 1요청. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
// 입력은 period 하나뿐이라 같은 (period, 최신 reference_date) 집계가 반복 조회된다 —
//   참여자 수·평균 수익률 같은 집계가 캐시/버퍼풀에 hot 하게 올라간 상태의 비용을 측정한다.
//
// 기간 분포: DAILY 70% / WEEKLY 20% / MONTHLY 10% (일간을 가장 자주 본다 — ranking_list.js 와 동일).
// referenceDate 는 보내지 않아 항상 최신 집계일의 통계를 조회한다.
//
// SLO: 실패 0, p99 < 50ms, 목표 200 req/s.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const statsLatency = new Trend('ranking_stats_latency', true);
const statsFailed  = new Rate('ranking_stats_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '200', 10); // 초당 요청수(TPS)

const PERIODS = [
  { value: 'DAILY',   weight: 0.7 },
  { value: 'WEEKLY',  weight: 0.2 },
  { value: 'MONTHLY', weight: 0.1 },
];

function pickPeriod() {
  const dice = Math.random();
  let acc = 0;
  for (const p of PERIODS) {
    acc += p.weight;
    if (dice < acc) return p.value;
  }
  return PERIODS[PERIODS.length - 1].value;
}

export const options = {
  scenarios: {
    ranking_stats: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'queryRankingStats',
    },
  },
  thresholds: {
    'http_req_failed{scenario:ranking_stats}':   ['rate==0'],
    'http_req_duration{scenario:ranking_stats}': ['p(99)<50'],
  },
};

export function queryRankingStats() {
  const period = pickPeriod();

  const res = http.get(`${API}/api/rankings/stats?period=${period}`, {
    tags: { scenario: 'ranking_stats' },
  });

  statsLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  statsFailed.add(!ok);
  check(res, { 'ranking stats ok': () => ok });
}
