// trypto 입금 주소 조회 부하 테스트 — 읽기 전용 GET /api/wallets/{walletId}/deposit-address?coinId={coinId}.
//
// 한 iteration = 한 지갑의 한 코인 입금 주소 1요청. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
// walletId 는 1..WALLET_COUNT 에서, coinId 는 2..COIN_MAX 에서 매번 랜덤으로 뽑는다 —
// 특정 (지갑,코인) 한 행만 buffer pool 에 hot 하게 박히는 비현실적 측정을 피한다.
// 기축통화 KRW(coinId=1) 는 입금 주소 대상이 아니라 제외한다(coinId 는 2 부터 시작).
//
// SLO: 실패 0, p99 < 50ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const addressLatency = new Trend('deposit_address_latency', true);
const addressFailed  = new Rate('deposit_address_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '200', 10); // 초당 요청수(TPS)

// 시드와 맞춘다. base 시드는 wallet 1..1000, coin 2..11 (1=KRW 기축은 제외).
const WALLET_COUNT = Number.parseInt(__ENV.WALLET_COUNT || '1000', 10);
const COIN_MIN     = Number.parseInt(__ENV.COIN_MIN || '2', 10);
const COIN_MAX     = Number.parseInt(__ENV.COIN_MAX || '11', 10);

function pickWalletId() {
  return Math.floor(Math.random() * WALLET_COUNT) + 1; // 1 .. WALLET_COUNT
}

function pickCoinId() {
  return COIN_MIN + Math.floor(Math.random() * (COIN_MAX - COIN_MIN + 1)); // COIN_MIN .. COIN_MAX
}

export const options = {
  scenarios: {
    deposit_address: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'queryDepositAddress',
    },
  },
  thresholds: {
    'http_req_failed{scenario:deposit_address}':   ['rate==0'],
    'http_req_duration{scenario:deposit_address}': ['p(99)<50'],
  },
};

export function queryDepositAddress() {
  const walletId = pickWalletId();
  const coinId = pickCoinId();

  const res = http.get(
    `${API}/api/wallets/${walletId}/deposit-address?coinId=${coinId}`,
    { tags: { scenario: 'deposit_address' } },
  );

  addressLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  addressFailed.add(!ok);
  check(res, { 'deposit address ok': () => ok });
}
