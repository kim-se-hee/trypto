#!/usr/bin/env bash
# Computes content-addressable image tags for trypto-api and trypto-collector,
# checks Docker Hub, builds+pushes only when the tag is missing.
# Writes to stdout:
#   API_TAG=<tag>
#   COLLECTOR_TAG=<tag>
set -euo pipefail

ORG="kimsehee98"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

log() { echo "[build-images] $*" >&2; }

# Tag = HEAD short sha (+ dirty hash if there are uncommitted changes or untracked source files)
compute_tag() {
  local dir="$1"
  cd "$REPO_ROOT/$dir"
  local head
  head=$(git rev-parse --short=12 HEAD)
  local changes
  changes=$({
    git diff HEAD --binary
    while IFS= read -r f; do
      [ -f "$f" ] && { printf 'U:%s\n' "$f"; cat "$f"; }
    done < <(git ls-files --others --exclude-standard)
  } | sha1sum | cut -c1-8)
  # sha1sum of empty input is da39a3ee — means clean
  if [ "$changes" = "da39a3ee" ]; then
    echo "$head"
  else
    echo "${head}-${changes}"
  fi
}

ensure_image() {
  local repo_dir="$1" image_name="$2" tag="$3"
  local full="${ORG}/${image_name}:${tag}"

  if docker manifest inspect "$full" >/dev/null 2>&1; then
    log "$full already on Hub, skipping build"
    return 0
  fi

  log "building $full"
  cd "$REPO_ROOT/$repo_dir"
  docker buildx build \
    --platform linux/amd64 \
    --push \
    --tag "$full" \
    .
}

API_TAG=$(compute_tag trypto-api)
COLLECTOR_TAG=$(compute_tag trypto-collector)

ensure_image trypto-api       trypto-api       "$API_TAG"
ensure_image trypto-collector trypto-collector "$COLLECTOR_TAG"

echo "API_TAG=$API_TAG"
echo "COLLECTOR_TAG=$COLLECTOR_TAG"
