#!/usr/bin/env bash
# 프로덕션 배포 (반복 실행용).
#
#   bash deploy/deploy.sh                # 빌드/푸시 + 서버 반영
#   bash deploy/deploy.sh --build-only   # 이미지 빌드/푸시까지만 (서버 없이도 가능)
#
# 동작: 모듈별 소스 콘텐츠 해시로 태그를 만들고(코드 같으면 태그 같음),
# Docker Hub 에 그 태그가 이미 있으면 빌드/푸시를 통째로 스킵한다.
# 이미지는 linux/arm64 로 굽는다 (오라클 A1). 빌드 스테이지는 Dockerfile 의
# --platform=$BUILDPLATFORM 덕에 네이티브로 돌아서 에뮬레이션 비용이 없다.
#
# 최초 1회 부트스트랩(인플럭스 시드, 인증서, DB 생성 등)은 deploy/README.md 참고.
# 그 절차는 이 스크립트가 하지 않는다.
set -euo pipefail
cd "$(dirname "$0")/.."

ENV_FILE=.env.prod
SSH_KEY="$HOME/.ssh/trypto_oracle"
REMOTE=/opt/trypto
HUB=kimsehee98
MODE="${1:-}"

log() { echo "[deploy] $*"; }
die() { echo "[deploy][ERROR] $*" >&2; exit 1; }

[ -f "$ENV_FILE" ] || die "$ENV_FILE 없음 (.env.prod.example 을 복사해 채운다)"
docker info >/dev/null 2>&1 || die "docker 데몬이 안 떠 있다"
# Windows 자격증명 저장소를 쓰면 docker info 에 Username 이 안 나오므로 config.json 의 로그인 흔적으로 확인한다
grep -q 'index.docker.io' "$HOME/.docker/config.json" 2>/dev/null \
  || docker info 2>/dev/null | grep -q "Username: $HUB" \
  || die "docker login 필요 (Hub 계정: $HUB)"

# ---------- 콘텐츠 해시 태그 ----------
java_hash() {
  local m=$1
  {
    find "$m/src" -type f -print0 | sort -z | xargs -0 sha256sum
    sha256sum "$m/Dockerfile" "$m/build.gradle" "$m/settings.gradle"
  } | sha256sum | cut -c1-12
}

front_hash() {
  {
    find frontend/src frontend/public -type f -print0 2>/dev/null | sort -z | xargs -0 sha256sum
    sha256sum frontend/Dockerfile frontend/nginx.conf frontend/package.json \
      frontend/package-lock.json frontend/index.html frontend/vite.config.ts
    # VITE_ 값은 번들에 박히므로 값이 바뀌면 태그도 바뀌어야 한다
    grep '^VITE_' "$ENV_FILE"
  } | sha256sum | cut -c1-12
}

API_TAG="prod-$(java_hash api)"
COL_TAG="prod-$(java_hash collector)"
ENG_TAG="prod-$(java_hash engine)"
FRT_TAG="prod-$(front_hash)"

# ---------- Hub 존재 체크 → 없는 것만 arm64 빌드/푸시 ----------
need() { ! docker manifest inspect "$1" >/dev/null 2>&1; }

VITE_ARGS=$(grep '^VITE_' "$ENV_FILE" | sed 's/^/--build-arg /' | tr '\n' ' ')

build_push() { # $1=이미지:태그 $2=컨텍스트 $3=추가인자
  log "빌드/푸시: $1"
  # shellcheck disable=SC2086
  docker buildx build --platform linux/arm64 --push -t "$1" $3 "$2"
}

if need "$HUB/trypto-api:$API_TAG";       then build_push "$HUB/trypto-api:$API_TAG" ./api ""; else log "api 변경 없음 → 스킵"; fi
if need "$HUB/trypto-collector:$COL_TAG"; then build_push "$HUB/trypto-collector:$COL_TAG" ./collector ""; else log "collector 변경 없음 → 스킵"; fi
if need "$HUB/trypto-engine:$ENG_TAG";    then build_push "$HUB/trypto-engine:$ENG_TAG" ./engine ""; else log "engine 변경 없음 → 스킵"; fi
if need "$HUB/trypto-frontend:$FRT_TAG";  then build_push "$HUB/trypto-frontend:$FRT_TAG" ./frontend "$VITE_ARGS"; else log "frontend 변경 없음 → 스킵"; fi

# ---------- .env.prod 의 이미지 태그 갱신 ----------
sed -i "s|^API_IMAGE=.*|API_IMAGE=$HUB/trypto-api:$API_TAG|" "$ENV_FILE"
sed -i "s|^COLLECTOR_IMAGE=.*|COLLECTOR_IMAGE=$HUB/trypto-collector:$COL_TAG|" "$ENV_FILE"
sed -i "s|^ENGINE_IMAGE=.*|ENGINE_IMAGE=$HUB/trypto-engine:$ENG_TAG|" "$ENV_FILE"
sed -i "s|^FRONTEND_IMAGE=.*|FRONTEND_IMAGE=$HUB/trypto-frontend:$FRT_TAG|" "$ENV_FILE"
log "태그: api=$API_TAG collector=$COL_TAG engine=$ENG_TAG frontend=$FRT_TAG"

if [ "$MODE" = "--build-only" ]; then
  log "빌드만 완료 (--build-only)"
  exit 0
fi

# ---------- 서버 반영 ----------
SERVER_IP="${SERVER_IP:-$(terraform -chdir=terraform/prod output -raw app_public_ip 2>/dev/null || true)}"
[ -n "$SERVER_IP" ] || die "서버 IP 를 알 수 없다. 인스턴스 생성 전이면 --build-only 로 실행한다"

SSH="ssh -i $SSH_KEY -o StrictHostKeyChecking=accept-new ubuntu@$SERVER_IP"
log "파일 동기화 → $SERVER_IP:$REMOTE"
$SSH "mkdir -p $REMOTE"
scp -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new docker-compose.prod.yml "ubuntu@$SERVER_IP:$REMOTE/"
scp -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new "$ENV_FILE" "ubuntu@$SERVER_IP:$REMOTE/.env"
scp -r -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new docker "ubuntu@$SERVER_IP:$REMOTE/"
scp -r -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new deploy "ubuntu@$SERVER_IP:$REMOTE/"

log "이미지 pull + 컨테이너 교체"
$SSH "cd $REMOTE && docker compose -f docker-compose.prod.yml pull -q && docker compose -f docker-compose.prod.yml up -d --no-build"

log "컨테이너 상태:"
$SSH "cd $REMOTE && docker compose -f docker-compose.prod.yml ps"
log "완료. backend 는 헬시까지 최대 4분 걸린다. 로그 보기:"
log "  ssh -i $SSH_KEY ubuntu@$SERVER_IP 'cd $REMOTE && docker compose -f docker-compose.prod.yml logs -f backend'"
