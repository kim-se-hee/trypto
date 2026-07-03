// trypto 포트폴리오 보유자산 조회 부하 테스트 — 읽기 전용 GET /api/users/{userId}/wallets/{walletId}/portfolio.
//
// 한 iteration = 지갑 하나의 포트폴리오 조회 1요청. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
//   - 임의의 지갑 n(1..WALLET_POOL)을 골라 조회한다.
//   - 시드는 user n → round n → wallet n 1:1 이라 userId == walletId == n 으로 두면
//     소유권 검증(round.user_id == userId)을 항상 통과한다.
//
// 조회 경로가 밟는 쿼리(GetMyHoldingsService): 소유권 → 지갑 → 거래소 기축통화 → 기축통화 잔고
//   → 평가된 보유 자산(현재가 포함) → 코인 심볼·이름. 한 요청이 여러 lookup 을 콜드로 밟게 해
//   조합형 조회의 라운드트립 비용을 측정한다.
//
// 주의: 보유 자산(holding) 행이 base.sql 에 없으면 FindEvaluatedHoldings 가 빈 목록으로 단락되어
//   현재가/매핑 lookup 을 건너뛴다. 보유 자산 join 경로까지 재려면 holding 시드가 필요하다.
//
// SLO: 실패 0, p99 < 100ms, 목표 200 req/s.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const portfolioLatency = new Trend('portfolio_latency', true);
const portfolioFailed  = new Rate('portfolio_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '200', 10); // 초당 요청수(TPS)

// 조회 대상 지갑 풀. 시드된 지갑 수(SEED_WALLETS)와 같거나 작아야 한다.
const WALLET_POOL = Number.parseInt(__ENV.SEED_WALLETS || '1000', 10);

function randInt(maxInclusive) {
  return 1 + Math.floor(Math.random() * maxInclusive);
}

function buildUrl(id) {
  return `${API}/api/users/${id}/wallets/${id}/portfolio`;
}

export const options = {
  scenarios: {
    my_holdings: {
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
    'http_req_failed{scenario:my_holdings}':   ['rate==0'],
    'http_req_duration{scenario:my_holdings}': ['p(99)<100'],
  },
};

export function queryPortfolio() {
  const id = randInt(WALLET_POOL); // userId == walletId == n

  const res = http.get(buildUrl(id), {
    tags: { scenario: 'my_holdings' },
  });

  portfolioLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  portfolioFailed.add(!ok);
  check(res, { 'portfolio ok': () => ok });
}
