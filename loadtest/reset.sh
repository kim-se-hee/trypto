#!/bin/bash
# 부하테스트 재실행용 상태 원복 스크립트
# SUT(또는 로컬)에서 실행:
#   ./loadtest/reset.sh                       # 기본 프로파일
#   ./loadtest/reset.sh ticker_websocket.js   # loadtest 오버라이드 같이 적용
#
# 두 가지 경로:
# - cold path  — 컨테이너 처음 띄우거나 mysql 이 살아있지 않을 때. down -v + pull + up
# - warm path  — 트레이딩 시나리오(place_order/match_pending) + mysql 이 healthy 상태로 살아있을 때
#                전체 재기동 대신 거래 흔적만 비우고 engine 만 재시작 (~30s, cold 의 약 1/3)
set -e

cd "$(dirname "$0")/.."

SCENARIO="${1:-}"
COMPOSE_ARGS=("-f" "docker-compose.yml")
case "$SCENARIO" in
  ticker_websocket.js)
    # loadtest 오버라이드 + 호스트 메트릭(node-exporter/cadvisor) 둘 다 켬
    COMPOSE_ARGS+=("-f" "docker-compose.loadtest.yml" "--profile" "metrics")
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

# cold path — 처음 띄우거나 컨테이너 죽어있을 때
echo "[1/4] compose down -v (모든 볼륨 제거)"
docker compose "${COMPOSE_ARGS[@]}" down -v

echo "[2/4] compose pull (.env 의 새 이미지 태그로 Hub 에서 가져오기)"
docker compose "${COMPOSE_ARGS[@]}" pull

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

echo "=== 준비 완료. k6 실행 가능 ==="
