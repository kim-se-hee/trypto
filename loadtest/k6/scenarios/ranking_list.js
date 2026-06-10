// trypto 수익률 랭킹 조회 부하 테스트 — 읽기 전용 GET /api/rankings.
//
// 한 iteration = 랭킹 한 페이지 조회 1요청. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
//   - 일부 요청(FIRST_PAGE_PROB): 첫 페이지 조회 (cursorRank 없음 → rank 1..size).
//   - 나머지: 임의 순위로 진입 (cursorRank = 1..SEED_RANKS-size).
//     → 커서 조건 `rank > cursorRank` 가 1..SEED_RANKS 전 구간에서 콜드로 밟히게 해
//       시드한 끝 순위까지의 인덱스 range 스캔을 측정한다.
//
// 기간 분포: DAILY 70% / WEEKLY 20% / MONTHLY 10% (일간을 가장 자주 본다).
// referenceDate 는 보내지 않아 항상 최신 집계일을 조회한다. 과거 날짜는 테이블 규모와
// MAX(reference_date) 비용을 현실화하는 용도다.
//
// SLO: 실패 0, p99 < 100ms, 목표 200 req/s.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const rankingLatency = new Trend('ranking_latency', true);
const rankingFailed  = new Rate('ranking_failed');
const queryRank      = new Trend('ranking_query_rank'); // 진입 순위 (첫 페이지=0)

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '200', 10); // 초당 요청수(TPS)

const PAGE_SIZE = Number.parseInt(__ENV.PAGE_SIZE || '20', 10);

// (period, reference_date)당 시드된 순위 수. ranking 시드의 SEED_WALLETS 와 같아야 한다.
const SEED_RANKS = Number.parseInt(__ENV.SEED_WALLETS || '1000', 10);
// 첫 페이지를 보는 요청 비율 (나머지는 임의 순위로 진입).
const FIRST_PAGE_PROB = Number.parseFloat(__ENV.FIRST_PAGE_PROB || '0.5');

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

function randInt(maxExclusive) {
  return Math.floor(Math.random() * maxExclusive);
}

function pickCursorRank() {
  if (Math.random() < FIRST_PAGE_PROB || SEED_RANKS <= PAGE_SIZE) return null;
  return randInt(SEED_RANKS - PAGE_SIZE) + 1; // 1 .. SEED_RANKS-PAGE_SIZE
}

function buildUrl(period, cursorRank) {
  let url = `${API}/api/rankings?period=${period}&size=${PAGE_SIZE}`;
  if (cursorRank != null) url += `&cursorRank=${cursorRank}`;
  return url;
}

export const options = {
  scenarios: {
    ranking_list: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'queryRanking',
    },
  },
  thresholds: {
    'http_req_failed{scenario:ranking_list}':   ['rate==0'],
    'http_req_duration{scenario:ranking_list}': ['p(99)<100'],
  },
};

export function queryRanking() {
  const period = pickPeriod();
  const cursorRank = pickCursorRank();

  const res = http.get(buildUrl(period, cursorRank), {
    tags: { scenario: 'ranking_list' },
  });

  rankingLatency.add(res.timings.duration);
  queryRank.add(cursorRank == null ? 0 : cursorRank);

  const ok = res.status >= 200 && res.status < 300;
  rankingFailed.add(!ok);
  check(res, { 'ranking ok': () => ok });
}
