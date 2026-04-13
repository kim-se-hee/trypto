#!/bin/bash
# 부하테스트 재실행용 상태 원복 스크립트
# SUT(또는 로컬)에서 실행: ./loadtest/reset.sh
set -e

cd "$(dirname "$0")/.."

echo "[1/3] compose down -v (모든 볼륨 제거)"
docker compose down -v

echo "[2/3] compose up -d"
docker compose up -d

echo "[3/3] 전체 healthy 대기"
deadline=$(( $(date +%s) + 900 ))
while :; do
  unhealthy=$(docker compose ps --format '{{.Service}}|{{.Health}}' \
    | awk -F'|' '$2 != "" && $2 != "healthy" {print $1}')
  if [ -z "$unhealthy" ]; then
    break
  fi
  if [ "$(date +%s)" -gt "$deadline" ]; then
    echo "[ERROR] healthy 대기 타임아웃, 상태:" >&2
    docker compose ps >&2
    exit 1
  fi
  echo "  대기 중: $unhealthy"
  sleep 5
done

echo "=== 준비 완료. k6 실행 가능 ==="
