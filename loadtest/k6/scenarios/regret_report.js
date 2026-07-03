// trypto 투자 복기 리포트 조회 부하 테스트 — 읽기 전용 GET /api/rounds/{roundId}/regret.
//
// 한 iteration = 한 라운드의 복기 리포트 1요청. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
// roundId 는 1..REGRET_ROUNDS 에서 매번 랜덤으로 뽑는다 — 특정 리포트 한 행만 buffer pool 에
// hot 하게 박히는 비현실적 측정을 피한다. base 시드상 userId == roundId == n 이라
// userId 도 같은 n 을 넣어 소유자 검증(ROUND_ACCESS_DENIED)을 통과시킨다.
// exchangeId 는 1(UPBIT) 고정 — regret 시드가 exchange 1 기준으로만 깔린다.
//
// 조회 경로: 라운드 소유자 검증 → exchange 1 지갑 존재 검증 → exchange 메타 → investment_rule →
//   regret_report(+rule_impact+violation_detail OneToMany) 로드 → coin 심볼 해소.
// regret 시드(regret.sql.tmpl)가 위 산출물을 미리 깔아둬야 200 이 나온다.
//
// SLO: 실패 0, p99 < 100ms (배치 산출물 단건 조회 경로).

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const reportLatency = new Trend('regret_report_latency', true);
const reportFailed  = new Rate('regret_report_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '100', 10); // 초당 요청수(TPS)

// 시드와 맞춘다. reset.sh 의 REGRET_ROUNDS 와 동일해야 한다.
const REGRET_ROUNDS = Number.parseInt(__ENV.REGRET_ROUNDS || '1000', 10);
const EXCHANGE_ID   = 1;

function pickRoundId() {
  return Math.floor(Math.random() * REGRET_ROUNDS) + 1; // 1 .. REGRET_ROUNDS
}

export const options = {
  scenarios: {
    regret_report: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'queryReport',
    },
  },
  thresholds: {
    'http_req_failed{scenario:regret_report}':   ['rate==0'],
    'http_req_duration{scenario:regret_report}': ['p(99)<100'],
  },
};

export function queryReport() {
  const roundId = pickRoundId();
  const userId = roundId; // base 시드: userId == roundId == n

  const url =
    `${API}/api/rounds/${roundId}/regret?userId=${userId}&exchangeId=${EXCHANGE_ID}`;

  const res = http.get(url, { tags: { scenario: 'regret_report' } });

  reportLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  reportFailed.add(!ok);
  check(res, { 'regret report ok': () => ok });
}
