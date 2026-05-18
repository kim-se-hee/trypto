// trypto 시세 SSE 부하 테스트 — 단일 ramp(up/sustain/down).
//
// 시나리오 시간선:
//   T=0~10분  : 동접 0 → 5,000 / 시세 0 → 241/s   (선형 ramp up, 비율 5000:241 동조)
//   T=10~20분 : 동접 5,000 / 시세 241/s            (sustain)
//   T=20~25분 : 동접 5,000 → 0 / 시세 241 → 0/s   (선형 ramp down)
//
// 거래소 분포 (VU 입주):
//   Upbit   90% → /api/sse/tickers/1
//   Bithumb  5% → /api/sse/tickers/2
//   Binance  5% → /api/sse/tickers/3
//
// 측정값 (k6 결과 — client 한계 시그널만 측정):
//   sse_disconnects        : VU 연결 끊김 누계 (서버 강제 종료 / 네트워크 끊김 감지용)
//   sse_connect_failures   : 초기 연결 실패 누계 (HTTP status != 200)
//   sse_messages_received  : 받은 SSE event 누계 (부하 검증용)
//
// 서버 측 처리 시간 ("서버가 보내기 직전까지의 시간") 은 server-side metric 으로 본다 —
//   ticker_collectorToOutbound_duration_seconds (api 가 SSE write 직전까지의 시간).
//
// 메시지 받으면 JSON.parse 까지만 흉내내고 결과는 버림 (운영 client 의 CPU 부담 시늉).
//
// xk6-sse 확장이 필요하다 (github.com/phymbert/xk6-sse). 표준 k6 이미지엔 없으므로 커스텀 빌드된 k6 이미지로 실행한다.
// /performance-test 스킬이 loadtest/k6/Dockerfile 을 빌드해 Docker Hub 의 kimsehee98/trypto-k6:lt-<hash>
// 태그로 push 하고, 각 loadgen 인스턴스가 그 이미지를 pull 해서 사용한다.

import http from 'k6/http';
import { check, fail } from 'k6';
import { Counter } from 'k6/metrics';
import sse from 'k6/x/sse';

const disconnects = new Counter('sse_disconnects');
const connectFailures = new Counter('sse_connect_failures');
const messagesReceived = new Counter('sse_messages_received');

const API_HOST = __ENV.API_HOST || 'localhost:8080';
const COLLECTOR_HOST = __ENV.COLLECTOR_HOST || 'localhost:8081';

const TARGET_VU = parseInt(__ENV.TARGET_VU || '5000', 10);
const PEAK_RATE_UPBIT = 217;
const PEAK_RATE_BITHUMB = 12;
const PEAK_RATE_BINANCE = 12;

// 분산 모드: loadgen N대를 띄울 때 collector ramp 는 1대만 호출해야 한다.
// orchestrator(skill) 가 첫 인스턴스만 RUN_RAMP_SETUP=true, 나머지는 false 로 띄운다.
const RUN_RAMP_SETUP = (__ENV.RUN_RAMP_SETUP || 'true').toLowerCase() !== 'false';

const RAMP_UP_DURATION = '10m';
const SUSTAIN_DURATION = '10m';
const RAMP_DOWN_DURATION = '5m';

export const options = {
  scenarios: {
    ticker_subscription: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: RAMP_UP_DURATION, target: TARGET_VU },
        { duration: SUSTAIN_DURATION, target: TARGET_VU },
        { duration: RAMP_DOWN_DURATION, target: 0 },
      ],
      gracefulRampDown: '30s',
      gracefulStop: '30s',
      exec: 'subscribeAndIdle',
    },
  },
  thresholds: {
    'sse_disconnects':       ['count<50'],
    'sse_connect_failures':  ['count<50'],
  },
};

// k6 setup() 은 테스트 시작 시 한 번 호출. collector 의 ramp 도 같은 모양으로 시작시킨다.
// VU 의 ramp up 과 시세 발행 ramp 이 같은 시각에 출발하므로 비율 5000:241 이 자연스럽게 유지된다.
//
// 분산 모드에서는 RUN_RAMP_SETUP=false 인 인스턴스는 ramp POST 를 건너뛴다.
// 시세 발행 rate 는 SUT 의 collector 1개가 들고 있으므로 N대로 곱하지 않는다.
export function setup() {
  if (!RUN_RAMP_SETUP) {
    return { startedAt: Date.now() };
  }
  const profile = {
    phases: [
      {
        durationSeconds: durationToSeconds(RAMP_UP_DURATION),
        toRates: { UPBIT: PEAK_RATE_UPBIT, BITHUMB: PEAK_RATE_BITHUMB, BINANCE: PEAK_RATE_BINANCE },
      },
      {
        durationSeconds: durationToSeconds(SUSTAIN_DURATION),
        toRates: { UPBIT: PEAK_RATE_UPBIT, BITHUMB: PEAK_RATE_BITHUMB, BINANCE: PEAK_RATE_BINANCE },
      },
      {
        durationSeconds: durationToSeconds(RAMP_DOWN_DURATION),
        toRates: { UPBIT: 0, BITHUMB: 0, BINANCE: 0 },
      },
    ],
  };
  const res = http.post(
    `http://${COLLECTOR_HOST}/loadtest/ticker/ramp`,
    JSON.stringify(profile),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(res, { 'collector ramp 시작 200': (r) => r.status === 200 }) || fail('collector ramp 시작 실패');
  return { startedAt: Date.now() };
}

export function teardown() {
  if (!RUN_RAMP_SETUP) {
    return;
  }
  http.post(`http://${COLLECTOR_HOST}/loadtest/ticker/stop`);
}

export function subscribeAndIdle() {
  const exchangeId = pickExchangeId();
  const url = `http://${API_HOST}/api/sse/tickers/${exchangeId}`;
  const params = { tags: { exchangeId: String(exchangeId) } };

  let opened = false;

  // sse.open 은 SSE stream 이 닫힐 때까지 (서버 close / VU 종료 / client.close) 블로킹한다.
  // SseEmitter timeout 0L 라 서버는 능동 close 안 함 → ramping-vus executor 가 VU 종료시킬 때
  // 자동으로 underlying HTTP 연결을 끊는다. 클라이언트는 별도 sleep / explicit close 불필요.
  const response = sse.open(url, params, function (client) {
    client.on('open', () => {
      opened = true;
    });

    client.on('event', (event) => {
      messagesReceived.add(1);
      // 운영 client 의 CPU 부담 시늉 — JSON.parse 까지만 흉내내고 결과는 버린다.
      consumeBody(event.data);
    });

    client.on('error', () => {
      // 정상 sustain 중 끊김 = 서버 강제 종료 / 네트워크 끊김 의심
      if (opened) {
        disconnects.add(1);
      }
    });
  });

  if (!response || response.status !== 200) {
    connectFailures.add(1);
  }
}

function consumeBody(body) {
  if (!body) return;
  try {
    JSON.parse(body);
  } catch (_) { /* 파싱 실패는 무시 — 운영 client 가 받았다 치는 동작만 흉내 */ }
}

function pickExchangeId() {
  const dice = Math.random();
  if (dice < 0.90) return 1;        // UPBIT
  if (dice < 0.95) return 2;        // BITHUMB
  return 3;                         // BINANCE
}

function durationToSeconds(s) {
  const m = /^(\d+)([smh])$/.exec(s);
  if (!m) throw new Error(`bad duration: ${s}`);
  const n = parseInt(m[1], 10);
  switch (m[2]) {
    case 's': return n;
    case 'm': return n * 60;
    case 'h': return n * 3600;
  }
}
