import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const orderLatency = new Trend('order_latency', true);
const orderFailed  = new Rate('order_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const WALLET_POOL = 1000;
const COIN_POOL   = 10;

function randInt(maxInclusive) {
  return 1 + Math.floor(Math.random() * maxInclusive);
}

function jitterPrice() {
  return Math.floor(49900 + Math.random() * 200);
}

export const options = {
  scenarios: {
    place_order: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 2000,
      stages: [
        { duration: '5m', target: 150 },
        { duration: '10m', target: 150 },
      ],
      exec: 'placeOrder',
    },
  },
  thresholds: {
    'http_req_failed{scenario:place_order}':   ['rate<0.01'],
    'http_req_duration{scenario:place_order}': ['p(99)<500'],
  },
};

export function placeOrder() {
  const payload = JSON.stringify({
    clientOrderId: uuidv4(),
    walletId: randInt(WALLET_POOL),
    exchangeCoinId: randInt(COIN_POOL),
    side: 'BUY',
    orderType: 'LIMIT',
    amount: 10000,
    price: jitterPrice(),
  });
  const res = http.post(`${API}/api/orders`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { scenario: 'place_order' },
  });
  orderLatency.add(res.timings.duration);
  orderFailed.add(res.status < 200 || res.status >= 300);
  check(res, { 'order accepted': (r) => r.status >= 200 && r.status < 300 });
}
