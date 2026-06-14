// trypto 긴급 충전 부하 테스트 — 쓰기 POST /api/rounds/{roundId}/emergency-funding.
//
// 한 iteration = 한 라운드에 긴급 자금 1회 투입. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
// roundId 는 1..ROUND_COUNT 에서 매번 랜덤으로 뽑아 부하를 1000 라운드에 고르게 분산한다 —
//   한 라운드 행에만 쓰기가 몰려 row lock 경합이나 buffer pool hot row 로 측정이 왜곡되는 걸 피한다.
//   base 시드가 user==round==wallet==n 으로 1:1 매핑하므로 userId 도 roundId 와 같은 값을 쓴다.
// exchangeId 는 1(UPBIT, KRW) 고정 — base 가 깔아둔 유일한 거래소이자 wallet UK(round,exchange) 의 행.
// idempotencyKey 는 매 요청 새 UUID — 멱등 조기반환에 걸리지 않고 항상 실제 충전 경로를 밟게 한다.
// amount 는 소액(기본 1) — 라운드별 한도 안에서 run 내내 충전을 이어가기 위함.
//
// 주의(도메인 캡): InvestmentRound.chargeEmergencyFunding 은 호출마다 emergencyChargeCount 를
//   1씩 깎고, 0 이 되면 EMERGENCY_FUNDING_CHANCE_EXHAUSTED 로 거절한다(기본 3회). 멱등 조기반환은
//   카운트를 깎지 않지만 본 시나리오는 매 요청 새 UUID 라 전부 실제 충전 → 카운트를 소모한다.
//   즉 라운드당 성공 횟수에 상한이 있으니, 부하 분산과 시드의 카운트/한도 설정이 run 길이를 버텨야 한다.
//
// SLO(쓰기): 실패 0, p99 < 100ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const fundingLatency = new Trend('emergency_funding_latency', true);
const fundingFailed  = new Rate('emergency_funding_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '100', 10); // 초당 요청수(TPS)

// 시드와 맞춘다. base.sql 의 round 수와 동일해야 한다 (user==round==wallet==n 1:1).
const ROUND_COUNT = Number.parseInt(__ENV.ROUND_COUNT || '1000', 10);

// 거래소: base 가 깔아둔 UPBIT(KRW) 한 곳.
const EXCHANGE_ID = Number.parseInt(__ENV.EXCHANGE_ID || '1', 10);

// 소액 충전 — 라운드 한도를 빠르게 소진하지 않도록 작게 잡는다.
const AMOUNT = Number.parseInt(__ENV.AMOUNT || '1', 10);

function pickRoundId() {
  return Math.floor(Math.random() * ROUND_COUNT) + 1; // 1 .. ROUND_COUNT
}

export const options = {
  scenarios: {
    emergency_funding: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'chargeFunding',
    },
  },
  thresholds: {
    'http_req_failed{scenario:emergency_funding}':   ['rate==0'],
    'http_req_duration{scenario:emergency_funding}': ['p(99)<100'],
  },
};

export function chargeFunding() {
  const roundId = pickRoundId();

  const payload = JSON.stringify({
    userId: roundId, // base 시드: userId == roundId == walletId
    exchangeId: EXCHANGE_ID,
    amount: AMOUNT,
    idempotencyKey: uuidv4(),
  });

  const res = http.post(`${API}/api/rounds/${roundId}/emergency-funding`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { scenario: 'emergency_funding' },
  });

  fundingLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  fundingFailed.add(!ok);
  check(res, { 'emergency funding ok': () => ok });
}
