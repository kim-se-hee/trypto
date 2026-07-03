// trypto 주문 취소 부하 테스트 — 쓰기 POST /api/orders/{orderId}/cancel.
//
// 사전 PENDING 시드 없이 한 iteration 에서 자급자족한다:
//   (1) 지정가 BUY 주문을 새로 만들고(setup, 태그 없음) 응답에서 orderId 를 뽑은 뒤
//   (2) 그 orderId 로 취소를 호출한다(act, scenario:cancel_order 태그).
// 측정 대상은 (2) 취소 호출뿐 — thresholds 는 scenario:cancel_order 태그만 본다.
//
// 지정가 가격을 체결 불가 수준(1 KRW)으로 박아 BUY 주문이 매도 호가에 닿지 않게 한다.
// 이러면 즉시 체결되지 않고 PENDING 으로 남아 취소가 가능하다. arrival-rate 라
// TARGET_RATE 가 곧 초당 취소 요청수(TPS)다.
//
// 멱등키(clientOrderId)는 매 요청 새 UUID 로 만들어 setup 주문이 run 중 소진되지 않게 한다.
//
// SLO: 실패 0, p99 < 100ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const cancelLatency = new Trend('cancel_order_latency', true);
const cancelFailed  = new Rate('cancel_order_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '100', 10); // 초당 취소 요청수(TPS)

// base 시드: wallet 1..1000, 각 wallet 의 exchange_coin 1..10 이 유효.
const WALLET_POOL = Number.parseInt(__ENV.WALLET_POOL || '1000', 10);
const COIN_POOL   = Number.parseInt(__ENV.COIN_POOL   || '10', 10);

// 체결되면 안 되는 BUY 지정가 가격. 매도 호가에 절대 닿지 않게 1 KRW 로 박는다.
const UNFILLABLE_PRICE = 1;

function randInt(maxInclusive) {
  return 1 + Math.floor(Math.random() * maxInclusive);
}

export const options = {
  scenarios: {
    cancel_order: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'cancelOrder',
    },
  },
  thresholds: {
    'http_req_failed{scenario:cancel_order}':   ['rate==0'],
    'http_req_duration{scenario:cancel_order}': ['p(99)<100'],
  },
};

export function cancelOrder() {
  const walletId = randInt(WALLET_POOL);

  // (1) setup: 체결 안 될 지정가 BUY 주문 생성. scenario 태그 없음(측정 제외).
  const placePayload = JSON.stringify({
    clientOrderId: uuidv4(),
    walletId: walletId,
    exchangeCoinId: randInt(COIN_POOL),
    side: 'BUY',
    orderType: 'LIMIT',
    amount: 10000,
    price: UNFILLABLE_PRICE,
  });
  const placeRes = http.post(`${API}/api/orders`, placePayload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { phase: 'setup' },
  });

  const placed = check(placeRes, {
    'order placed': (r) => r.status >= 200 && r.status < 300,
  });
  if (!placed) return;

  const orderId = placeRes.json('data.orderId');
  if (orderId == null) return;

  // (2) act: 위에서 만든 주문을 취소. 이 호출만 측정한다.
  const cancelPayload = JSON.stringify({ walletId: walletId });
  const cancelRes = http.post(`${API}/api/orders/${orderId}/cancel`, cancelPayload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { scenario: 'cancel_order' },
  });

  cancelLatency.add(cancelRes.timings.duration);

  const ok = cancelRes.status >= 200 && cancelRes.status < 300;
  cancelFailed.add(!ok);
  check(cancelRes, { 'order cancelled': () => ok });
}
