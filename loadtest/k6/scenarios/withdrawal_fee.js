// trypto 출금 수수료 조회 부하 테스트 — 읽기 전용 GET /api/withdrawal-fees.
//
// 한 iteration = 한 (거래소, 코인, 체인) 조합의 수수료 1요청. arrival-rate 라
// TARGET_RATE 가 곧 초당 요청수(TPS)다.
// exchangeId 는 1(UPBIT) 고정, coinId 는 2..11 에서 매번 랜덤으로 뽑아 특정 한 행만
// buffer pool 에 hot 하게 박히는 비현실적 측정을 피한다. chain 은 시드와 맞춰 'BTC' 통일 —
// withdrawal_fee 시드가 chain='BTC' 한 종류만 채우므로 다른 값을 보내면 404 가 된다.
//
// 조회 경로(WithdrawalFeeQueryAdapter): WHERE exchange_id=? AND coin_id=? AND chain=? 단건 조회.
//
// SLO: 실패 0, p99 < 50ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const feeLatency = new Trend('withdrawal_fee_latency', true);
const feeFailed  = new Rate('withdrawal_fee_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '200', 10); // 초당 요청수(TPS)

// 시드와 맞춘다. withdrawal-fee.sql.tmpl 의 exchange 1 × coin 2~11 × chain 'BTC' 와 동일해야 한다.
const EXCHANGE_ID = Number.parseInt(__ENV.EXCHANGE_ID || '1', 10);
const COIN_MIN    = 2;
const COIN_MAX    = 11;
const CHAIN       = __ENV.CHAIN || 'BTC';

function pickCoinId() {
  return COIN_MIN + Math.floor(Math.random() * (COIN_MAX - COIN_MIN + 1)); // 2 .. 11
}

export const options = {
  scenarios: {
    withdrawal_fee: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'queryFee',
    },
  },
  thresholds: {
    'http_req_failed{scenario:withdrawal_fee}':   ['rate==0'],
    'http_req_duration{scenario:withdrawal_fee}': ['p(99)<50'],
  },
};

export function queryFee() {
  const coinId = pickCoinId();

  const url = `${API}/api/withdrawal-fees?exchangeId=${EXCHANGE_ID}&coinId=${coinId}&chain=${CHAIN}`;
  const res = http.get(url, { tags: { scenario: 'withdrawal_fee' } });

  feeLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  feeFailed.add(!ok);
  check(res, { 'withdrawal fee ok': () => ok });
}
