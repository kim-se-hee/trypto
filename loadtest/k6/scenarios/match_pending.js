import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const API = __ENV.API_TARGET || 'http://localhost:8080';
const COLLECTOR = __ENV.COLLECTOR_TARGET || 'http://localhost:8081';

const WALLET_ID = 1;
const EXCHANGE_COIN_ID = 1;

function jitterPrice() {
  return Math.floor(49900 + Math.random() * 200);
}

export const options = {
  scenarios: {
    place_order: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 300,
      stages: [
        { duration: '5m',  target: 150 },
        { duration: '30m', target: 150 },
      ],
      exec: 'placeOrder',
    },
    feed_ticker: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 50,
      maxVUs: 400,
      stages: [
        { duration: '5m',  target: 241 },
        { duration: '30m', target: 241 },
      ],
      exec: 'feedTicker',
    },
  },
  thresholds: {
    'http_req_failed{scenario:place_order}': ['rate<0.01'],
    'http_req_duration{scenario:place_order}': ['p(99)<500'],
  },
};

export function placeOrder() {
  const payload = JSON.stringify({
    clientOrderId: uuidv4(),
    walletId: WALLET_ID,
    exchangeCoinId: EXCHANGE_COIN_ID,
    side: 'BUY',
    orderType: 'LIMIT',
    amount: 10000,
    price: jitterPrice(),
  });
  const res = http.post(`${API}/api/orders`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { scenario: 'place_order' },
  });
  check(res, { 'order accepted': (r) => r.status >= 200 && r.status < 300 });
}

export function feedTicker() {
  const payload = JSON.stringify({
    exchange: 'UPBIT',
    base: 'BTC',
    quote: 'KRW',
    displayName: 'BTC',
    lastPrice: jitterPrice(),
    changeRate: 0,
    quoteTurnover: 0,
    tsMs: Date.now(),
  });
  const res = http.post(`${COLLECTOR}/internal/loadtest/ticker`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { scenario: 'feed_ticker' },
  });
  check(res, { 'ticker accepted': (r) => r.status >= 200 && r.status < 300 });
}
