// trypto 송금 실행 부하 테스트 — 쓰기 POST /api/transfers.
//
// 한 iteration = 한 건의 송금 1요청. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
// from 지갑은 1..WALLET_COUNT 에서 매번 랜덤으로 뽑고, to 는 같은 라운드의 2번째 거래소 지갑
// (1000+n) 으로 보낸다 — base 시드에서 from=n 과 to=1000+n 은 같은 round n 의 KRW 지갑이라
// 송금이 항상 유효하다. 모든 지갑 KRW 잔고가 100억이고 1회 1000원만 보내므로 잔고가 소진되지
// 않는 비소모형 — run 중 시드 보충이 필요 없다.
//
// idempotencyKey 는 매 요청 새 UUID 라 멱등 중복으로 막히지 않는다 — 순수 송금 처리 비용을 잰다.
//
// SLO: 실패 0, p99 < 100ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const transferLatency = new Trend('transfer_latency', true);
const transferFailed  = new Rate('transfer_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '100', 10); // 초당 요청수(TPS)

// 시드와 맞춘다. base 시드의 1..WALLET_COUNT 지갑이 from 풀이고, to 는 1000+n 이다.
const WALLET_COUNT = Number.parseInt(__ENV.WALLET_COUNT || '1000', 10);

const COIN_ID = 1;    // KRW
const AMOUNT  = 1000; // 1회 송금액(원)

function pickFromWalletId() {
  return Math.floor(Math.random() * WALLET_COUNT) + 1; // 1 .. WALLET_COUNT
}

export const options = {
  scenarios: {
    transfer: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'transfer',
    },
  },
  thresholds: {
    'http_req_failed{scenario:transfer}':   ['rate==0'],
    'http_req_duration{scenario:transfer}': ['p(99)<100'],
  },
};

export function transfer() {
  const fromWalletId = pickFromWalletId();
  const toWalletId = 1000 + fromWalletId; // 같은 라운드의 2번째 거래소 지갑

  const payload = JSON.stringify({
    idempotencyKey: uuidv4(),
    fromWalletId,
    toWalletId,
    coinId: COIN_ID,
    amount: AMOUNT,
  });

  const res = http.post(`${API}/api/transfers`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { scenario: 'transfer' },
  });

  transferLatency.add(res.timings.duration);

  const ok = res.status === 201;
  transferFailed.add(!ok);
  check(res, { 'transfer created': () => ok });
}
