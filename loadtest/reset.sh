#!/bin/bash
# 부하테스트 재실행용 상태 원복 스크립트
# SUT(또는 로컬)에서 실행:
#   ./loadtest/reset.sh                       # 기본 프로파일
#   ./loadtest/reset.sh ticker_websocket.js   # loadtest 오버라이드 같이 적용
#
# 세 가지 경로:
# - cold path  — 컨테이너 처음 띄우거나 mysql 이 살아있지 않을 때. down -v + pull + up.
#                조회 시나리오면 healthy 후 추가 시드(랭킹/보유/캔들/현재가)를 한 번 적재한다.
# - warm path  — 트레이딩 시나리오(place_order/match_pending) + mysql 이 healthy 상태로 살아있을 때
#                전체 재기동 대신 거래 흔적만 비우고 engine 만 재시작 (~30s, cold 의 약 1/3)
# - read-warm  — 조회 시나리오(ranking_list/my_holdings/candle_scroll) + mysql healthy 일 때.
#                조회는 데이터를 바꾸지 않으므로 재시드 없이 backend 만 새 코드로 재시작한다
#                (ddl-auto=none, sql.init=never → 테이블·데이터 보존).
set -e

cd "$(dirname "$0")/.."

SCENARIO="${1:-}"
COMPOSE_ARGS=("-f" "docker-compose.yml")
case "$SCENARIO" in
  ticker_websocket.js)
    # loadtest 오버라이드 + 호스트 메트릭(node-exporter/cadvisor) 둘 다 켬
    COMPOSE_ARGS+=("-f" "docker-compose.loadtest.yml" "--profile" "metrics")
    ;;
  place_order.js|match_pending*.js)
    # backend 를 loadtest 프로파일로 띄워서 market-meta-sync 를 끈다 → loadtest.sql 의 coin_id=1=KRW 가정이 유효
    COMPOSE_ARGS+=("-f" "docker-compose.loadtest.yml")
    ;;
  ranking_list.js|my_holdings.js|candle_scroll.js|ranker_portfolio.js)
    # 조회 시나리오도 loadtest 프로파일로 띄운다 — loadtest.sql 로 기본 시드(coin/user/wallet) 적재 + market-meta-sync off
    COMPOSE_ARGS+=("-f" "docker-compose.loadtest.yml")
    ;;
esac

# warm path 가능 여부 — 트레이딩 시나리오 + mysql 이 healthy 상태로 떠있어야 함
WARM_PATH=0
case "$SCENARIO" in
  place_order.js|match_pending*.js)
    MYSQL_CID=$(docker compose "${COMPOSE_ARGS[@]}" ps -q mysql 2>/dev/null || true)
    if [ -n "$MYSQL_CID" ] \
       && [ "$(docker inspect -f '{{.State.Health.Status}}' "$MYSQL_CID" 2>/dev/null)" = "healthy" ]; then
      WARM_PATH=1
    fi
    ;;
esac

# 조회 시나리오 판별 — 데이터를 바꾸지 않아 재측정 사이클에서 재시드가 불필요하다.
READ_SCENARIO=0
case "$SCENARIO" in
  ranking_list.js|my_holdings.js|candle_scroll.js|ranker_portfolio.js) READ_SCENARIO=1 ;;
esac

if [ "$WARM_PATH" = 1 ]; then
  echo "[warm] 트레이딩 시나리오 fast path: 거래 테이블 truncate + influx bucket 재생성 + backend/engine 재시작"

  echo "[warm 1/6] mysql 거래 테이블 truncate"
  docker compose "${COMPOSE_ARGS[@]}" exec -T mysql mysql -uroot -p1234 trypto -e "
    SET FOREIGN_KEY_CHECKS = 0;
    TRUNCATE TABLE orders;
    TRUNCATE TABLE order_fill_failure;
    TRUNCATE TABLE rule_violation;
    TRUNCATE TABLE holding;
    TRUNCATE TABLE outbox;
    TRUNCATE TABLE wallet_balance;
    SET FOREIGN_KEY_CHECKS = 1;
  "

  echo "[warm 2/6] wallet_balance KRW 시드 재삽입 (1000 행)"
  docker compose "${COMPOSE_ARGS[@]}" exec -T mysql mysql -uroot -p1234 trypto <<'SQL'
SET SESSION cte_max_recursion_depth = 5000;
INSERT INTO wallet_balance (balance_id, wallet_id, coin_id, available, locked)
WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000)
SELECT n, n, 1, 10000000000.00000000, 0.00000000 FROM seq;
SQL

  echo "[warm 3/6] influx ticker bucket 재생성"
  docker compose "${COMPOSE_ARGS[@]}" exec -T influxdb \
    influx bucket delete --name ticker --org trypto --token trypto-collector-token >/dev/null 2>&1 || true
  docker compose "${COMPOSE_ARGS[@]}" exec -T influxdb \
    influx bucket create --name ticker --org trypto --token trypto-collector-token >/dev/null

  echo "[warm 4/6] rabbitmq 큐 purge (이전 회차 미처리 메시지 제거)"
  docker compose "${COMPOSE_ARGS[@]}" exec -T rabbitmq sh -c '
    rabbitmqctl list_queues -q name | while read q; do
      [ -z "$q" ] && continue
      rabbitmqctl purge_queue "$q" >/dev/null 2>&1 || true
    done
  '

  echo "[warm 5/6] backend + engine 재시작 (인메모리 상태 + WAL 정리)"
  docker compose "${COMPOSE_ARGS[@]}" stop engine backend
  docker compose "${COMPOSE_ARGS[@]}" rm -f engine backend
  docker volume rm trypto_engine-wal >/dev/null 2>&1 || true
  docker compose "${COMPOSE_ARGS[@]}" up -d backend engine

  echo "[warm 6/6] backend + engine healthy 대기"
  deadline=$(( $(date +%s) + 180 ))
  while :; do
    bs=$(docker compose "${COMPOSE_ARGS[@]}" ps backend --format '{{.Health}}' 2>/dev/null)
    es=$(docker compose "${COMPOSE_ARGS[@]}" ps engine --format '{{.Health}}' 2>/dev/null)
    [ "$bs" = "healthy" ] && [ "$es" = "healthy" ] && break
    if [ "$(date +%s)" -gt "$deadline" ]; then
      echo "[ERROR] healthy 대기 타임아웃 (backend=$bs engine=$es)" >&2
      docker compose "${COMPOSE_ARGS[@]}" ps >&2
      exit 1
    fi
    sleep 3
  done

  echo "=== 준비 완료 (warm path). k6 실행 가능 ==="
  exit 0
fi

# read-warm path — 조회 시나리오 + mysql healthy. 데이터를 그대로 두고 backend 만 새 코드로 재시작한다.
# ddl-auto=none + sql.init=never 라 Hibernate 가 테이블을 날리거나 loadtest.sql 을 재적재하지 않아
# cold 에서 깔아둔 랭킹/보유/캔들 시드가 보존된다.
if [ "$READ_SCENARIO" = 1 ]; then
  MYSQL_CID=$(docker compose "${COMPOSE_ARGS[@]}" ps -q mysql 2>/dev/null || true)
  if [ -n "$MYSQL_CID" ] \
     && [ "$(docker inspect -f '{{.State.Health.Status}}' "$MYSQL_CID" 2>/dev/null)" = "healthy" ]; then
    echo "[read-warm] 조회 fast path: 데이터 보존, backend 만 새 코드로 재시작 (ddl-auto=none, sql.init=never)"
    HIBERNATE_DDL_AUTO=none SQL_INIT_MODE=never \
      docker compose "${COMPOSE_ARGS[@]}" up -d --no-deps --force-recreate backend

    echo "[read-warm] backend healthy 대기"
    deadline=$(( $(date +%s) + 300 ))
    while :; do
      bs=$(docker compose "${COMPOSE_ARGS[@]}" ps backend --format '{{.Health}}' 2>/dev/null)
      [ "$bs" = "healthy" ] && break
      if [ "$(date +%s)" -gt "$deadline" ]; then
        echo "[ERROR] backend healthy 대기 타임아웃 (backend=$bs)" >&2
        docker compose "${COMPOSE_ARGS[@]}" ps >&2
        exit 1
      fi
      sleep 3
    done

    echo "=== 준비 완료 (read-warm path). k6 실행 가능 ==="
    exit 0
  fi
fi

# cold path — 처음 띄우거나 컨테이너 죽어있을 때
echo "[1/4] compose down -v (모든 볼륨 제거)"
docker compose "${COMPOSE_ARGS[@]}" down -v

# 이미지 확보: 기본은 Hub pull (AWS SUT 는 prebuilt 이미지를 받는다).
# RESET_BUILD_LOCAL=1 이면 로컬 소스로 빌드한다 (로컬 performance-tune — Hub 의존 없이 작업트리 코드를 검증).
if [ "${RESET_BUILD_LOCAL:-0}" = "1" ]; then
  echo "[2/4] compose build (RESET_BUILD_LOCAL=1 — 로컬 소스로 이미지 빌드)"
  docker compose "${COMPOSE_ARGS[@]}" build
else
  echo "[2/4] compose pull (.env 의 새 이미지 태그로 Hub 에서 가져오기)"
  docker compose "${COMPOSE_ARGS[@]}" pull
fi

echo "[3/4] compose up -d"
docker compose "${COMPOSE_ARGS[@]}" up -d

echo "[4/4] 전체 healthy 대기"
deadline=$(( $(date +%s) + 900 ))
while :; do
  unhealthy=$(docker compose "${COMPOSE_ARGS[@]}" ps --format '{{.Service}}|{{.Health}}' \
    | awk -F'|' '$2 != "" && $2 != "healthy" {print $1}')
  if [ -z "$unhealthy" ]; then
    break
  fi
  if [ "$(date +%s)" -gt "$deadline" ]; then
    echo "[ERROR] healthy 대기 타임아웃, 상태:" >&2
    docker compose "${COMPOSE_ARGS[@]}" ps >&2
    exit 1
  fi
  echo "  대기 중: $unhealthy"
  sleep 5
done

# 조회 시나리오: loadtest.sql 이 채우지 않는 추가 데이터를 한 번 적재한다 (이후 read-warm 으로 보존).
if [ "$READ_SCENARIO" = 1 ]; then
  WALLET_COUNT="${SEED_WALLETS:-1000}"
  RANKING_DAYS="${RANKING_DAYS:-30}"
  CANDLE_DAYS="${CANDLE_DAYS:-30}"
  SNAPSHOT_USERS="${SNAPSHOT_USERS:-100}"
  SNAPSHOT_DAYS="${SNAPSHOT_DAYS:-30}"
  case "$SCENARIO" in
    ranking_list.js)
      echo "[read-seed] ranking 적재 (period 3 × ${RANKING_DAYS}일 × ${WALLET_COUNT}명)"
      sed "s/@WALLET_COUNT@/${WALLET_COUNT}/g; s/@RANKING_DAYS@/${RANKING_DAYS}/g" loadtest/seed/ranking.sql.tmpl \
        | docker compose "${COMPOSE_ARGS[@]}" exec -T mysql mysql -uroot -p1234 trypto
      ;;
    my_holdings.js)
      echo "[read-seed] holding 적재 (${WALLET_COUNT} 지갑 × 3코인) + Redis 현재가"
      sed "s/@WALLET_COUNT@/${WALLET_COUNT}/g" loadtest/seed/holdings.sql.tmpl \
        | docker compose "${COMPOSE_ARGS[@]}" exec -T mysql mysql -uroot -p1234 trypto
      docker compose "${COMPOSE_ARGS[@]}" exec -T redis redis-cli SET 'ticker:UPBIT:BTC/KRW' '{"lastPrice":50000000}' >/dev/null
      docker compose "${COMPOSE_ARGS[@]}" exec -T redis redis-cli SET 'ticker:UPBIT:ETH/KRW' '{"lastPrice":5200000}'  >/dev/null
      docker compose "${COMPOSE_ARGS[@]}" exec -T redis redis-cli SET 'ticker:UPBIT:XRP/KRW' '{"lastPrice":4800}'      >/dev/null
      ;;
    candle_scroll.js)
      echo "[read-seed] candle_1m 적재 (UPBIT × 10코인 × ${CANDLE_DAYS}일)"
      bash loadtest/seed/gen-candles.sh "${CANDLE_DAYS}" \
        | docker compose "${COMPOSE_ARGS[@]}" exec -T influxdb influx write \
            --bucket ticker --org trypto --token trypto-collector-token --precision s
      ;;
    ranker_portfolio.js)
      echo "[read-seed] ranking 적재 (period 3 × ${RANKING_DAYS}일 × ${WALLET_COUNT}명) — rank=user_id 라 1~100 이 열람 가능 상위 구간"
      sed "s/@WALLET_COUNT@/${WALLET_COUNT}/g; s/@RANKING_DAYS@/${RANKING_DAYS}/g" loadtest/seed/ranking.sql.tmpl \
        | docker compose "${COMPOSE_ARGS[@]}" exec -T mysql mysql -uroot -p1234 trypto
      echo "[read-seed] portfolio_snapshot(+detail) 적재 (상위 ${SNAPSHOT_USERS}명 × ${SNAPSHOT_DAYS}일 × 코인 5종)"
      sed "s/@SNAPSHOT_USERS@/${SNAPSHOT_USERS}/g; s/@SNAPSHOT_DAYS@/${SNAPSHOT_DAYS}/g" loadtest/seed/ranker-portfolio.sql.tmpl \
        | docker compose "${COMPOSE_ARGS[@]}" exec -T mysql mysql -uroot -p1234 trypto
      ;;
  esac
fi

echo "=== 준비 완료. k6 실행 가능 ==="
