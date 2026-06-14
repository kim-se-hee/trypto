// trypto 프로필 조회 부하 테스트 — 읽기 전용 GET /api/users/{userId}.
//
// 한 iteration = 한 사용자의 프로필 1요청. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
// userId 는 1..USER_COUNT 에서 매번 랜덤으로 뽑는다 — 특정 사용자 한 행만 buffer pool 에
// hot 하게 박히는 비현실적 측정을 피한다. USER_COUNT 는 시드의 사용자 수와 같아야 한다.
//
// SLO: 실패 0, p99 < 50ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const profileLatency = new Trend('get_profile_latency', true);
const profileFailed  = new Rate('get_profile_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '200', 10); // 초당 요청수(TPS)

// 시드와 맞춘다. base 시드는 user 1..1000 이 존재한다.
const USER_COUNT = Number.parseInt(__ENV.USER_COUNT || '1000', 10);

function pickUserId() {
  return Math.floor(Math.random() * USER_COUNT) + 1; // 1 .. USER_COUNT
}

export const options = {
  scenarios: {
    get_profile: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'getProfile',
    },
  },
  thresholds: {
    'http_req_failed{scenario:get_profile}':   ['rate==0'],
    'http_req_duration{scenario:get_profile}': ['p(99)<50'],
  },
};

export function getProfile() {
  const userId = pickUserId();

  const res = http.get(`${API}/api/users/${userId}`, {
    tags: { scenario: 'get_profile' },
  });

  profileLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  profileFailed.add(!ok);
  check(res, { 'get profile ok': () => ok });
}
