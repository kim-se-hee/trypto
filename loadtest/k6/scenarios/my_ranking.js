// trypto 내 랭킹 조회 부하 테스트 — 읽기 전용 GET /api/rankings/me.
//
// 한 iteration = 한 사용자의 내 랭킹 1요청. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
// userId 는 1..USER_COUNT 에서 매번 랜덤으로 뽑는다 — 특정 한 행만 buffer pool 에 hot 하게
// 박히는 비현실적 측정을 피한다. USER_COUNT 는 ranking 시드의 WALLET_COUNT 와 같아야 한다.
//
// 기간 분포: DAILY 70% / WEEKLY 20% / MONTHLY 10% (일간을 가장 자주 본다 — ranking_list.js 와 동일).
// referenceDate 는 보내지 않아 항상 최신 집계일의 내 순위를 조회한다.
//
// 조회 경로: findLatestReferenceDate(MAX(reference_date)) 뒤
//   WHERE period = ? AND reference_date = ? AND user_id = ? 로 내 한 행을 찾는다.
//
// SLO: 실패 0, p99 < 50ms, 목표 200 req/s.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const myRankingLatency = new Trend('my_ranking_latency', true);
const myRankingFailed  = new Rate('my_ranking_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '200', 10); // 초당 요청수(TPS)

// ranking 시드의 user 수와 맞춘다. reset.sh 의 SEED_WALLETS 와 동일해야 한다.
const USER_COUNT = Number.parseInt(__ENV.SEED_WALLETS || '1000', 10);

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
  return Math.floor(Math.random() * USER_COUNT) + 1; // 1 .. USER_COUNT
}

export const options = {
  scenarios: {
    my_ranking: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'queryMyRanking',
    },
  },
  thresholds: {
    'http_req_failed{scenario:my_ranking}':   ['rate==0'],
    'http_req_duration{scenario:my_ranking}': ['p(99)<50'],
  },
};

export function queryMyRanking() {
  const userId = pickUserId();
  const period = pickPeriod();

  const res = http.get(`${API}/api/rankings/me?userId=${userId}&period=${period}`, {
    tags: { scenario: 'my_ranking' },
  });

  myRankingLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  myRankingFailed.add(!ok);
  check(res, { 'my ranking ok': () => ok });
}
