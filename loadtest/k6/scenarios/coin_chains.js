// trypto 코인 체인 조회 부하 테스트 — 읽기 전용 GET /api/exchanges/{exchangeId}/coins/{coinId}/chains.
//
// 한 iteration = 한 (거래소, 코인)의 지원 체인 목록 1요청. arrival-rate 라 TARGET_RATE 가 곧 TPS.
// exchangeId 는 1(UPBIT) 고정, coinId 는 2..11 에서 매번 랜덤으로 뽑는다 — coin-chains 시드가
// exchange_coin 1~10(coin 2~11)에 체인 1~2개를 깔아둬 빈 배열이 아닌 의미 있는 응답이 나온다.
// 특정 코인 한 건만 buffer pool 에 hot 하게 박히는 비현실적 측정을 피하려 매번 랜덤이다.
//
// 조회 경로: exchange_coin_chain 을 exchange_coin 에 조인해 (exchange_id, coin_id)로 좁혀 chain 을 모은다.
//   소규모 조회라 인덱스 없이도 빠르다 — 읽기 경로의 기본 지연을 측정하는 게 목적.
//
// SLO: 실패 0, p99 < 50ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const chainsLatency = new Trend('coin_chains_latency', true);
const chainsFailed  = new Rate('coin_chains_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '200', 10); // 초당 요청수(TPS)

// 시드와 맞춘다. base.sql 의 exchange 1=UPBIT, coin 2~11(=exchange_coin 1~10)과 동일해야 한다.
const EXCHANGE_ID = Number.parseInt(__ENV.EXCHANGE_ID || '1', 10);
const COIN_MIN    = Number.parseInt(__ENV.COIN_MIN || '2', 10);
const COIN_MAX    = Number.parseInt(__ENV.COIN_MAX || '11', 10);

function pickCoinId() {
  return COIN_MIN + Math.floor(Math.random() * (COIN_MAX - COIN_MIN + 1)); // COIN_MIN .. COIN_MAX
}

export const options = {
  scenarios: {
    coin_chains: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'queryChains',
    },
  },
  thresholds: {
    'http_req_failed{scenario:coin_chains}':   ['rate==0'],
    'http_req_duration{scenario:coin_chains}': ['p(99)<50'],
  },
};

export function queryChains() {
  const coinId = pickCoinId();

  const url = `${API}/api/exchanges/${EXCHANGE_ID}/coins/${coinId}/chains`;

  const res = http.get(url, { tags: { scenario: 'coin_chains' } });

  chainsLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  chainsFailed.add(!ok);
  check(res, { 'coin chains ok': () => ok });
}
