// trypto 거래소 상장 코인 목록 조회 부하 테스트 — 읽기 전용 GET /api/exchanges/{exchangeId}/coins.
//
// 한 iteration = 한 거래소의 상장 코인 목록 1요청. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
// exchangeId 는 1(UPBIT) 또는 2(BITHUMB) 를 랜덤으로 뽑는다 — base 시드에 존재하는 두 거래소만
// 번갈아 때려 한 거래소 결과만 캐시/buffer pool 에 hot 하게 박히는 편향을 줄인다.
// 마스터 데이터라 행 수가 적어 자연히 hot 하지만, 그래도 분산해 둔다.
//
// SLO: 실패 0, p99 < 50ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const coinsLatency = new Trend('exchange_coins_latency', true);
const coinsFailed  = new Rate('exchange_coins_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '200', 10); // 초당 요청수(TPS)

// base 시드의 거래소: 1=UPBIT, 2=BITHUMB.
const EXCHANGE_IDS = [1, 2];

function pickExchangeId() {
  return EXCHANGE_IDS[Math.floor(Math.random() * EXCHANGE_IDS.length)];
}

export const options = {
  scenarios: {
    exchange_coins: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'queryExchangeCoins',
    },
  },
  thresholds: {
    'http_req_failed{scenario:exchange_coins}':   ['rate==0'],
    'http_req_duration{scenario:exchange_coins}': ['p(99)<50'],
  },
};

export function queryExchangeCoins() {
  const exchangeId = pickExchangeId();

  const res = http.get(`${API}/api/exchanges/${exchangeId}/coins`, {
    tags: { scenario: 'exchange_coins' },
  });

  coinsLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  coinsFailed.add(!ok);
  check(res, { 'exchange coins ok': () => ok });
}
