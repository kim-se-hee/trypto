// trypto 포트폴리오 공개 여부 변경 부하 테스트 — 쓰기 PUT /api/users/{userId}/portfolio-visibility.
//
// 한 iteration = 한 사용자의 공개 여부 1회 변경. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
// userId 는 1..USER_COUNT 에서 매번 랜덤으로 뽑는다 — 특정 사용자 한 행만 hot 하게 박히는
// 비현실적 측정을 피한다. USER_COUNT 는 시드의 라운드 보유 사용자 수(1..1000)와 같아야 한다.
//
// 멱등: portfolioPublic 는 true/false 토글이라 같은 값이 거듭 들어와도 결과가 같다.
//   iteration 의 짝/홀로 값을 뒤집어 한 행이 한쪽 상태로만 고이지 않게 한다(소비형 아님).
//
// SLO: 실패 0, p99 < 100ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const visibilityLatency = new Trend('change_visibility_latency', true);
const visibilityFailed  = new Rate('change_visibility_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '100', 10); // 초당 요청수(TPS)

// 시드와 맞춘다. 라운드를 보유한 사용자(1..1000)만 유효 ID 다.
const USER_COUNT = Number.parseInt(__ENV.USER_COUNT || '1000', 10);

function pickUserId() {
  return Math.floor(Math.random() * USER_COUNT) + 1; // 1 .. USER_COUNT
}

export const options = {
  scenarios: {
    change_visibility: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'changeVisibility',
    },
  },
  thresholds: {
    'http_req_failed{scenario:change_visibility}':   ['rate==0'],
    'http_req_duration{scenario:change_visibility}': ['p(99)<100'],
  },
};

export function changeVisibility() {
  const userId = pickUserId();
  // iteration 짝/홀로 true/false 를 뒤집어 한 행이 한 상태로만 고이지 않게 한다.
  const portfolioPublic = __ITER % 2 === 0;

  const payload = JSON.stringify({ portfolioPublic });

  const res = http.put(`${API}/api/users/${userId}/portfolio-visibility`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { scenario: 'change_visibility' },
  });

  visibilityLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  visibilityFailed.add(!ok);
  check(res, { 'change visibility ok': () => ok });
}
