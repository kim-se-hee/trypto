// trypto 랭커 포트폴리오 열람 부하 테스트 — 읽기 전용 GET /api/rankings/{userId}/portfolio.
//
// 한 iteration = 한 명의 포트폴리오 열람 1요청. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
// userId 는 1..TOP_N 에서 매번 랜덤으로 뽑는다 — 랭킹 목록에서 상위 100명 중 아무나 클릭해
// 들어가는 실제 흐름을 본떠, 단일 행만 buffer pool 에 hot 하게 박히는 비현실적 측정을 피한다.
// TOP_N 은 시드의 열람 가능 상위 구간과 같아야 한다(ranking rank=user_id, snapshot SNAPSHOT_USERS).
//
// 기간 분포: DAILY 70% / WEEKLY 20% / MONTHLY 10% (일간을 가장 자주 본다).
//
// SLO: 실패 0, p99 < 50ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const portfolioLatency = new Trend('ranker_portfolio_latency', true);
const portfolioFailed  = new Rate('ranker_portfolio_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '200', 10); // 초당 요청수(TPS)

// 열람 가능 상위 구간. snapshot 시드의 SNAPSHOT_USERS, ranking rank<=100 과 맞춘다.
const TOP_N = Number.parseInt(__ENV.RANKER_TOP_N || '100', 10);

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

function pickUserId() {
  return Math.floor(Math.random() * TOP_N) + 1; // 1 .. TOP_N
}

export const options = {
  scenarios: {
    ranker_portfolio: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'queryPortfolio',
    },
  },
  thresholds: {
    'http_req_failed{scenario:ranker_portfolio}':   ['rate==0'],
    'http_req_duration{scenario:ranker_portfolio}': ['p(99)<50'],
  },
};

export function queryPortfolio() {
  const userId = pickUserId();
  const period = pickPeriod();

  const res = http.get(`${API}/api/rankings/${userId}/portfolio?period=${period}`, {
    tags: { scenario: 'ranker_portfolio' },
  });

  portfolioLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  portfolioFailed.add(!ok);
  check(res, { 'ranker portfolio ok': () => ok });
}
