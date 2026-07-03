// trypto 지갑 잔고 조회 부하 테스트 — 읽기 전용 GET /api/users/{userId}/wallets/{walletId}/balances.
//
// 한 iteration = 한 지갑의 잔고 1요청. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
// base 시드는 userId==walletId==n (1..N) 으로 1:1 매핑이라 둘을 같은 n 으로 보낸다.
// n 은 1..WALLET_COUNT 에서 매번 랜덤으로 뽑는다 — 특정 지갑 한 행만 buffer pool 에
// hot 하게 박히는 비현실적 측정을 피한다. WALLET_COUNT 는 시드의 지갑 수와 같아야 한다.
//
// SLO: 실패 0, p99 < 50ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const balanceLatency = new Trend('wallet_assets_latency', true);
const balanceFailed  = new Rate('wallet_assets_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '200', 10); // 초당 요청수(TPS)

// 시드와 맞춘다. base 시드는 wallet 1..1000 이 userId==walletId 로 1:1 존재한다.
const WALLET_COUNT = Number.parseInt(__ENV.WALLET_COUNT || '1000', 10);

function pickWalletId() {
  return Math.floor(Math.random() * WALLET_COUNT) + 1; // 1 .. WALLET_COUNT
}

export const options = {
  scenarios: {
    wallet_assets: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'queryBalances',
    },
  },
  thresholds: {
    'http_req_failed{scenario:wallet_assets}':   ['rate==0'],
    'http_req_duration{scenario:wallet_assets}': ['p(99)<50'],
  },
};

export function queryBalances() {
  const id = pickWalletId(); // userId == walletId

  const res = http.get(`${API}/api/users/${id}/wallets/${id}/balances`, {
    tags: { scenario: 'wallet_assets' },
  });

  balanceLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  balanceFailed.add(!ok);
  check(res, { 'wallet assets ok': () => ok });
}
