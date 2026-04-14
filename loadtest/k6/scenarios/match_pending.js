import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const orderLatency  = new Trend('order_latency', true);
const tickerLatency = new Trend('ticker_latency', true);
const orderFailed   = new Rate('order_failed');
const tickerFailed  = new Rate('ticker_failed');

const API       = __ENV.API_TARGET       || 'http://localhost:8080';
const COLLECTOR = __ENV.COLLECTOR_TARGET || 'http://localhost:8081';

const WALLET_POOL = 1000;
const ORDER_AMOUNT_KRW = 10000;
const PRICE_JITTER_PCT = 0.02;

const COINS = [
  { id: 1,  exchange: 'UPBIT', base: 'BTC',  quote: 'KRW', displayName: '비트코인',         anchor: 50_000_000 },
  { id: 2,  exchange: 'UPBIT', base: 'ETH',  quote: 'KRW', displayName: '이더리움',         anchor: 3_500_000  },
  { id: 3,  exchange: 'UPBIT', base: 'XRP',  quote: 'KRW', displayName: '리플',             anchor: 800        },
  { id: 4,  exchange: 'UPBIT', base: 'SOL',  quote: 'KRW', displayName: '솔라나',           anchor: 250_000    },
  { id: 5,  exchange: 'UPBIT', base: 'ADA',  quote: 'KRW', displayName: '에이다',           anchor: 500        },
  { id: 6,  exchange: 'UPBIT', base: 'DOGE', quote: 'KRW', displayName: '도지코인',         anchor: 200        },
  { id: 7,  exchange: 'UPBIT', base: 'AVAX', quote: 'KRW', displayName: '아발란체',         anchor: 30_000     },
  { id: 8,  exchange: 'UPBIT', base: 'DOT',  quote: 'KRW', displayName: '폴카닷',           anchor: 7_000      },
  { id: 9,  exchange: 'UPBIT', base: 'LINK', quote: 'KRW', displayName: '체인링크',         anchor: 20_000     },
  { id: 10, exchange: 'UPBIT', base: 'POL',  quote: 'KRW', displayName: '폴리곤에코시스템토큰', anchor: 300        },
];

function randInt(maxInclusive) {
  return 1 + Math.floor(Math.random() * maxInclusive);
}

function pickCoin() {
  return COINS[Math.floor(Math.random() * COINS.length)];
}

function jitterPrice(anchor) {
  const delta = (Math.random() * 2 - 1) * PRICE_JITTER_PCT;
  const raw = anchor * (1 + delta);
  return anchor >= 1000 ? Math.round(raw) : Number(raw.toFixed(2));
}

export const options = {
  scenarios: {
    place_order: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 500,
      stages: [
        { duration: '5m',  target: 150 },
        { duration: '15m', target: 150 },
      ],
      exec: 'placeOrder',
    },
    feed_ticker: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 600,
      stages: [
        { duration: '5m',  target: 240 },
        { duration: '15m', target: 240 },
      ],
      exec: 'feedTicker',
    },
  },
  thresholds: {
    'http_req_failed{scenario:place_order}':   ['rate<0.01'],
    'http_req_duration{scenario:place_order}': ['p(95)<100', 'p(99)<100'],
  },
};

export function placeOrder() {
  const coin = pickCoin();
  const payload = JSON.stringify({
    clientOrderId: uuidv4(),
    walletId: randInt(WALLET_POOL),
    exchangeCoinId: coin.id,
    side: 'BUY',
    orderType: 'LIMIT',
    amount: ORDER_AMOUNT_KRW,
    price: jitterPrice(coin.anchor),
  });
  const res = http.post(`${API}/api/orders`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { scenario: 'place_order' },
  });
  orderLatency.add(res.timings.duration);
  orderFailed.add(res.status < 200 || res.status >= 300);
  check(res, { 'order accepted': (r) => r.status >= 200 && r.status < 300 });
}

export function feedTicker() {
  const coin = pickCoin();
  const payload = JSON.stringify({
    exchange: coin.exchange,
    base: coin.base,
    quote: coin.quote,
    displayName: coin.displayName,
    lastPrice: jitterPrice(coin.anchor),
    changeRate: 0,
    quoteTurnover: 0,
    tsMs: Date.now(),
  });
  const res = http.post(`${COLLECTOR}/internal/loadtest/ticker`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { scenario: 'feed_ticker' },
  });
  tickerLatency.add(res.timings.duration);
  tickerFailed.add(res.status < 200 || res.status >= 300);
  check(res, { 'ticker accepted': (r) => r.status >= 200 && r.status < 300 });
}
