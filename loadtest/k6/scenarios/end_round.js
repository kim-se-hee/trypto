// trypto 라운드 종료 부하 테스트 — 쓰기 POST /api/rounds/{roundId}/end.
//
// 한 iteration = 라운드 1개 시작(setup, 비측정) → 그 라운드 종료(측정)로 자급자족한다.
// end 가 측정 대상이고, end 가 소비할 ACTIVE 라운드를 같은 iteration 의 start 로 직접 만들어
// 시드를 소진하지 않는다. arrival-rate 라 TARGET_RATE 가 곧 초당 end 요청수(TPS)다.
//
// 동시성: 한 user 에 ACTIVE 라운드가 둘이면 setup 의 start 가 ACTIVE_ROUND_EXISTS 로 막힌다.
// round-less user 풀 1001..1200(200명)을 VU 가 1:1 전담하게 묶어(userId = 1001 + (__VU % 200))
// 같은 user 동시 start 충돌을 없앤다. 그래서 preAllocatedVUs=maxVUs=200 로 고정한다.
//
// body 는 StartRoundRequest/EndRoundRequest 그대로: start 는 userId, seeds[{exchangeId, amount}],
// emergencyFundingLimit. end 는 userId. exchangeId 는 1(UPBIT), rules 는 생략한다.
//
// SLO: 실패 0, p99 < 100ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const endLatency = new Trend('end_round_latency', true);
const endFailed  = new Rate('end_round_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '100', 10); // 초당 end 요청수(TPS)

// round-less user 풀. base 시드의 user 1001..1200(200명, 라운드 생성 전용)과 맞춘다.
const ROUNDLESS_USER_BASE  = 1001;
const ROUNDLESS_USER_COUNT = 200;

const EXCHANGE_ID            = 1; // UPBIT(KRW)
const SEED_AMOUNT            = 1000000;
const EMERGENCY_FUNDING_LIMIT = 1000000; // 100만(도메인 MAX_EMERGENCY_FUNDING_LIMIT)

const JSON_HEADERS = { 'Content-Type': 'application/json' };

// VU 가 자기 user 를 전담한다 — 같은 user 동시 start 충돌(ACTIVE_ROUND_EXISTS) 방지.
function myUserId() {
  return ROUNDLESS_USER_BASE + (__VU % ROUNDLESS_USER_COUNT);
}

export const options = {
  scenarios: {
    end_round: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 200,
      maxVUs: 200,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'endRound',
    },
  },
  thresholds: {
    'http_req_failed{scenario:end_round}':   ['rate==0'],
    'http_req_duration{scenario:end_round}': ['p(99)<100'],
  },
};

export function endRound() {
  const userId = myUserId();

  const startPayload = JSON.stringify({
    userId,
    seeds: [{ exchangeId: EXCHANGE_ID, amount: SEED_AMOUNT }],
    emergencyFundingLimit: EMERGENCY_FUNDING_LIMIT,
  });

  // setup: end 가 소비할 ACTIVE 라운드를 만든다(태그 없음 — 측정 제외).
  const startRes = http.post(`${API}/api/rounds`, startPayload, {
    headers: JSON_HEADERS,
    tags: { scenario: 'end_round_setup' },
  });

  if (startRes.status < 200 || startRes.status >= 300) return;

  const roundId = startRes.json('data.roundId');
  if (roundId === null || roundId === undefined) return;

  // 측정 대상: 라운드 종료.
  const endRes = http.post(`${API}/api/rounds/${roundId}/end`, JSON.stringify({ userId }), {
    headers: JSON_HEADERS,
    tags: { scenario: 'end_round' },
  });

  endLatency.add(endRes.timings.duration);

  const endOk = endRes.status >= 200 && endRes.status < 300;
  endFailed.add(!endOk);
  check(endRes, { 'round ended': () => endOk });
}
