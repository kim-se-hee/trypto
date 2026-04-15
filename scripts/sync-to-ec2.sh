#!/usr/bin/env bash
# Rsyncs the outer-repo-tracked workspace files (compose/SQL/infra config/loadtest)
# to the SUT, then writes .env with the image tags from build-images.sh.
#
# Usage:
#   API_TAG=abc COLLECTOR_TAG=def ./scripts/sync-to-ec2.sh
#   ./scripts/build-images.sh | ./scripts/sync-to-ec2.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KEY="${SSH_KEY:-$HOME/.ssh/trypto-key-pair.pem}"
SSH_OPTS="-i $KEY -o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=/dev/null -o LogLevel=ERROR -o ConnectTimeout=10"

log() { echo "[sync-to-ec2] $*" >&2; }

# Accept API_TAG/COLLECTOR_TAG via stdin lines when piped.
if [ ! -t 0 ]; then
  while IFS= read -r line; do
    case "$line" in
      API_TAG=*|COLLECTOR_TAG=*) export "$line" ;;
    esac
  done
fi

: "${API_TAG:?API_TAG is required}"
: "${COLLECTOR_TAG:?COLLECTOR_TAG is required}"

cd "$REPO_ROOT/terraform"
SUT_IP=$(terraform output -raw sut_public_ip)

cd "$REPO_ROOT"
TMPLIST=$(mktemp)
trap 'rm -f "$TMPLIST"' EXIT

# Take everything the outer repo tracks, except local orchestration files that
# only live on the developer machine.
git ls-files \
  | grep -v -E '^(scripts/|terraform/|\.claude/|\.gitignore$)' \
  > "$TMPLIST"

log "rsync workspace -> SUT ($SUT_IP) ($(wc -l < "$TMPLIST") files)"
rsync -avz \
  --rsh="ssh $SSH_OPTS" \
  --files-from="$TMPLIST" \
  ./ ubuntu@"$SUT_IP":/home/ubuntu/trypto/

log "writing /home/ubuntu/trypto/.env (image tags)"
ssh $SSH_OPTS ubuntu@"$SUT_IP" "cat > /home/ubuntu/trypto/.env" <<EOF
API_IMAGE=kimsehee98/trypto-api:${API_TAG}
COLLECTOR_IMAGE=kimsehee98/trypto-collector:${COLLECTOR_TAG}
EOF

log "done — API=$API_TAG, COLLECTOR=$COLLECTOR_TAG"
