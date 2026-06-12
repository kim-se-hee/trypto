// trypto 송금 내역 조회 부하 테스트 — 읽기 전용 GET /api/wallets/{walletId}/transfers.
//
// 한 iteration = 한 지갑의 송금 내역 1요청. arrival-rate 라 TARGET_RATE 가 곧 초당 요청수(TPS)다.
// walletId 는 1..WALLET_COUNT 에서 매번 랜덤으로 뽑는다 — 특정 지갑 한 행만 buffer pool 에
// hot 하게 박히는 비현실적 측정을 피한다. WALLET_COUNT 는 시드의 지갑 수와 같아야 한다.
//
// type 분포: ALL 60% / DEPOSIT 20% / WITHDRAW 20% (전체 보기를 가장 자주).
// cursor: 50% 첫 페이지(cursor 생략), 50% 1..(WALLET_COUNT*TRANSFERS_PER_WALLET) 랜덤 —
//   깊은 페이지의 커서 스캔까지 밟히게 한다. 송금 테이블엔 from/to 보조 인덱스가 없어
//   ALL 조회는 (from=? OR to=?) 풀스캔 + filesort 로 떨어진다 — 이 비용을 측정하는 게 목적.
//
// SLO: 실패 0, p99 < 50ms.

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const historyLatency = new Trend('transfer_history_latency', true);
const historyFailed  = new Rate('transfer_history_failed');

const API = __ENV.API_TARGET || 'http://localhost:8080';

const RAMP_DUR    = __ENV.RAMP_DUR    || '1m';
const HOLD_DUR    = __ENV.HOLD_DUR    || '3m';
const TARGET_RATE = Number.parseInt(__ENV.TARGET_RATE || '200', 10); // 초당 요청수(TPS)

// 시드와 맞춘다. reset.sh 의 SEED_WALLETS / TRANSFERS_PER_WALLET 와 동일해야 한다.
const WALLET_COUNT        = Number.parseInt(__ENV.WALLET_COUNT || '1000', 10);
const TRANSFERS_PER_WALLET = Number.parseInt(__ENV.TRANSFERS_PER_WALLET || '150', 10);
const MAX_TRANSFER_ID     = WALLET_COUNT * TRANSFERS_PER_WALLET;

const SIZE = Number.parseInt(__ENV.PAGE_SIZE || '20', 10);

const TYPES = [
  { value: 'ALL',      weight: 0.6 },
  { value: 'DEPOSIT',  weight: 0.2 },
  { value: 'WITHDRAW', weight: 0.2 },
];

function pickType() {
  const dice = Math.random();
  let acc = 0;
  for (const t of TYPES) {
    acc += t.weight;
    if (dice < acc) return t.value;
  }
  return TYPES[TYPES.length - 1].value;
}

function pickWalletId() {
  return Math.floor(Math.random() * WALLET_COUNT) + 1; // 1 .. WALLET_COUNT
}

// 50% 첫 페이지(null), 50% 깊은 페이지 진입용 랜덤 커서.
function pickCursor() {
  if (Math.random() < 0.5) return null;
  return Math.floor(Math.random() * MAX_TRANSFER_ID) + 1;
}

export const options = {
  scenarios: {
    transfer_history: {
      executor: 'ramping-arrival-rate',
      startRate: 0,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: 1000,
      stages: [
        { duration: RAMP_DUR, target: TARGET_RATE },
        { duration: HOLD_DUR, target: TARGET_RATE },
      ],
      exec: 'queryHistory',
    },
  },
  thresholds: {
    'http_req_failed{scenario:transfer_history}':   ['rate==0'],
    'http_req_duration{scenario:transfer_history}': ['p(99)<50'],
  },
};

export function queryHistory() {
  const walletId = pickWalletId();
  const type = pickType();
  const cursor = pickCursor();

  let url = `${API}/api/wallets/${walletId}/transfers?size=${SIZE}&type=${type}&userId=${walletId}`;
  if (cursor !== null) url += `&cursorTransferId=${cursor}`;

  const res = http.get(url, { tags: { scenario: 'transfer_history' } });

  historyLatency.add(res.timings.duration);

  const ok = res.status >= 200 && res.status < 300;
  historyFailed.add(!ok);
  check(res, { 'transfer history ok': () => ok });
}
