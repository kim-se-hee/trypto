// trypto 캔들 무한스크롤 부하 테스트 — 읽기 전용 GET /api/candles.
//
// 한 사람(VU) = 차트를 열고 과거로 쉴 새 없이 스크롤하는 closed-loop 세션.
//   - 차트 열기: cursor 없이 최신 LIMIT 개 조회.
//   - 스크롤 1회 = 요청 1회: 직전 응답의 가장 오래된 캔들(data[0]) 시각을 cursor 로 다음 뭉치 조회.
//   - 끝(LIMIT 미만 응답) 도달 시 이 사이클 종료 → 다음 iteration 이 새 코인으로 최신부터 재시작.
// think time 없음 — 최대 부하. ramping-vus 라 TARGET_VUS 가 곧 동시 사용자 수다.
//
// RAIL: 스크롤/드래그는 Response(100ms)가 아니라 Animation 범주(프레임당 16ms)다.
//   "물 흐르듯 연속 스크롤"을 보장하는 기준은 한 프레임 = 16ms 이므로 SLO 는 p99 < 16ms.
//   (web.dev/articles/rail — 스크롤은 Response 의 입력 처리 규칙에서 명시적으로 제외)
//
// 시드: loadtest/seed/gen-candles.sh 가 1분봉 CANDLE_DAYS 일치를 InfluxDB 에 적재.
//   coin 태그는 서버 변환과 동일한 "SYM/KRW" 형식. 한 바퀴(최신→끝) ≈ DAYS*1440/LIMIT 요청.
//
// SLO: 실패 0, p99 < 16ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const candleLatency = new Trend('candle_latency', true);
const candleFailed  = new Rate('candle_failed');
const scrollDepth   = new Trend('candle_scroll_depth'); // 리셋 전까지 연속 스크롤 횟수 (한 바퀴 길이)

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR   = __ENV.RAMP_DUR   || '1m';
const HOLD_DUR   = __ENV.HOLD_DUR   || '3m';
const TARGET_VUS = parseInt(__ENV.TARGET_VUS || '200', 10); // 동시 사용자 수 (최대치)

const LIMIT    = parseInt(__ENV.LIMIT || '120', 10); // 1분봉 프론트 candleCount 와 동일
const INTERVAL = __ENV.INTERVAL || '1m';
const EXCHANGE = __ENV.EXCHANGE || 'UPBIT';

const COINS = ['BTC', 'ETH', 'XRP', 'SOL', 'ADA', 'DOGE', 'AVAX', 'DOT', 'LINK', 'POL'];

function pickCoin() {
  return COINS[Math.floor(Math.random() * COINS.length)];
}

function buildUrl(coin, cursor) {
  let url = `${API}/api/candles?exchange=${EXCHANGE}&coin=${coin}&interval=${INTERVAL}&limit=${LIMIT}`;
  if (cursor) url += `&cursor=${encodeURIComponent(cursor)}`;
  return url;
}

export const options = {
  scenarios: {
    candle_scroll: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: RAMP_DUR, target: TARGET_VUS },
        { duration: HOLD_DUR, target: TARGET_VUS },
      ],
      exec: 'scrollCandles',
    },
  },
  thresholds: {
    'http_req_failed{scenario:candle_scroll}':   ['rate==0'],
    'http_req_duration{scenario:candle_scroll}': ['p(99)<16'],
  },
};

// 한 iteration = 한 차트를 열고 가장 오래된 캔들까지 쉴 새 없이 스크롤하는 1 사이클.
// 사이클이 끝나면 k6 가 같은 VU 로 즉시 새 iteration(새 코인, 최신부터)을 돌린다.
export function scrollCandles() {
  const coin = pickCoin();
  let cursor = null;
  let depth = 0;

  for (;;) {
    const res = http.get(buildUrl(coin, cursor), { tags: { scenario: 'candle_scroll' } });
    candleLatency.add(res.timings.duration);

    const ok = res.status >= 200 && res.status < 300;
    candleFailed.add(!ok);
    check(res, { 'candle ok': () => ok });
    if (!ok) return;

    let data;
    try {
      data = res.json('data');
    } catch (e) {
      data = null;
    }
    const n = Array.isArray(data) ? data.length : 0;

    // 끝 도달 — 더 과거가 없다. 이 사이클 종료.
    if (n < LIMIT) {
      scrollDepth.add(depth);
      return;
    }

    // 가장 오래된 캔들(오름차순이라 data[0]) 시각을 다음 cursor 로 — 더 과거로.
    cursor = data[0].time;
    depth += 1;
  }
}
