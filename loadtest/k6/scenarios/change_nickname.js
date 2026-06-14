// trypto 닉네임 변경 부하 테스트 — 쓰기 PUT /api/users/{userId}/nickname.
//
// 한 iteration = 한 사용자의 닉네임 1회 변경. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
// userId 는 1..USER_COUNT 에서 매번 랜덤으로 뽑는다 — 특정 사용자 한 행만 hot 하게 박히는
// 비현실적 측정을 피한다. USER_COUNT 는 시드의 라운드 보유 사용자 수(1..1000)와 같아야 한다.
//
// 자급자족: 닉네임은 매 요청 'ld'+userId+'_'+iteration+'_'+__VU 로 조합해 전 구간 유니크하게 만든다.
//   같은 사용자를 반복 갱신해도 값이 매번 달라 멱등성·시드 소진 걱정이 없다(쓰기지만 소비형 아님).
//
// SLO: 실패 0, p99 < 100ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const nicknameLatency = new Trend('change_nickname_latency', true);
const nicknameFailed  = new Rate('change_nickname_failed');

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
    change_nickname: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'changeNickname',
    },
  },
  thresholds: {
    'http_req_failed{scenario:change_nickname}':   ['rate==0'],
    'http_req_duration{scenario:change_nickname}': ['p(99)<100'],
  },
};

export function changeNickname() {
  const userId = pickUserId();
  // 'ld'+userId+'_'+iteration+'_'+__VU 조합 — VU·iteration 별로 갈라 충돌 없는 유니크 닉네임.
  const nickname = `ld${userId}_${__ITER}_${__VU}`;

  const payload = JSON.stringify({ nickname });

  const res = http.put(`${API}/api/users/${userId}/nickname`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { scenario: 'change_nickname' },
  });

  nicknameLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  nicknameFailed.add(!ok);
  check(res, { 'change nickname ok': () => ok });
}
