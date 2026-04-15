#!/usr/bin/env bash
set -euo pipefail

# Usage: launch-k6.sh <scenario-path>
#   scenario-path: relative to /home/ubuntu/trypto/ on loadgen,
#                  e.g. loadtest/k6/scenarios/match_pending.js

if [ $# -ne 1 ]; then
  echo "Usage: $0 <scenario-path>" >&2
  exit 1
fi

SCENARIO="$1"

KEY="${SSH_KEY:-$HOME/.ssh/trypto-key-pair.pem}"
SSH_OPTS="-i $KEY -o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR -o ConnectTimeout=10"

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOCAL_LOADTEST="$REPO_ROOT/loadtest"

cd "$REPO_ROOT/terraform"
LG_IP=$(terraform output -raw loadgen_public_ip)
SUT_PRIVATE=$(terraform output -raw sut_private_ip)

REMOTE_SCENARIO="/home/ubuntu/trypto/${SCENARIO}"
LOG="/home/ubuntu/k6.log"

if ssh $SSH_OPTS ubuntu@"$LG_IP" 'pgrep -x k6 >/dev/null'; then
  echo "[launch-k6] ERROR: k6 already running on loadgen. Run /loadtest-down or wait." >&2
  exit 2
fi

echo "[launch-k6] syncing local loadtest/ -> loadgen"
tar -cz -C "$LOCAL_LOADTEST" . | \
  ssh $SSH_OPTS ubuntu@"$LG_IP" \
    'rm -rf /home/ubuntu/trypto/loadtest && mkdir -p /home/ubuntu/trypto/loadtest && tar -xz -C /home/ubuntu/trypto/loadtest'

echo "[launch-k6] launching k6 on loadgen ($LG_IP)"
echo "[launch-k6] scenario: $REMOTE_SCENARIO"

# shellcheck disable=SC2029
ssh $SSH_OPTS ubuntu@"$LG_IP" \
  "test -f $REMOTE_SCENARIO || { echo 'scenario not found: $REMOTE_SCENARIO' >&2; exit 3; }
   rm -f $LOG
   nohup env \
     K6_WEB_DASHBOARD=true \
     K6_WEB_DASHBOARD_HOST=0.0.0.0 \
     K6_WEB_DASHBOARD_PORT=5665 \
     K6_WEB_DASHBOARD_EXPORT=/home/ubuntu/report.html \
     k6 run $REMOTE_SCENARIO \
       --env API_TARGET=http://${SUT_PRIVATE}:8080 \
       --env COLLECTOR_TARGET=http://${SUT_PRIVATE}:8081 \
     >$LOG 2>&1 </dev/null &
   disown || true
   sleep 1
   pgrep -x k6 >/dev/null && echo 'k6 started' || { echo 'k6 failed to start, see log:' >&2; tail -50 $LOG >&2; exit 4; }"

echo "[launch-k6] launched. log: ssh ubuntu@$LG_IP 'tail -f $LOG'"
