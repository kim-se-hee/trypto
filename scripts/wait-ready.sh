#!/usr/bin/env bash
set -euo pipefail

KEY="${SSH_KEY:-$HOME/.ssh/trypto-key-pair.pem}"
SSH_OPTS="-i $KEY -o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR -o ConnectTimeout=10"

cd "$(dirname "$0")/../terraform"
SUT_IP=$(terraform output -raw sut_public_ip)
LG_IP=$(terraform output -raw loadgen_public_ip)

DEADLINE=$(( $(date +%s) + 1200 ))

wait_ready_file() {
  local host=$1 label=$2
  echo "[wait-ready] $label READY 파일 대기 ($host)"
  while :; do
    if ssh $SSH_OPTS ubuntu@"$host" '[ -f /home/ubuntu/READY ]' 2>/dev/null; then
      echo "[wait-ready] $label READY ✓"
      return 0
    fi
    [ "$(date +%s)" -gt "$DEADLINE" ] && { echo "[wait-ready] timeout: $label" >&2; return 1; }
    sleep 10
  done
}

wait_actuator() {
  local host=$1
  echo "[wait-ready] SUT API actuator/health UP 확인"
  while :; do
    status=$(ssh $SSH_OPTS ubuntu@"$host" \
      "curl -fsS http://localhost:8080/actuator/health 2>/dev/null | grep -o '\"status\":\"UP\"' || true")
    if [ -n "$status" ]; then
      echo "[wait-ready] SUT API UP ✓"
      return 0
    fi
    [ "$(date +%s)" -gt "$DEADLINE" ] && { echo "[wait-ready] timeout: actuator UP" >&2; return 1; }
    sleep 10
  done
}

wait_ready_file "$LG_IP" "loadgen"
wait_ready_file "$SUT_IP" "SUT"
wait_actuator "$SUT_IP"
echo "[wait-ready] 전체 준비 완료"
