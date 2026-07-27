#!/usr/bin/env bash
#
# EC2에서 실행되는 배포 스크립트.
#
# 새 이미지를 올린 뒤 컨테이너가 healthy 가 될 때까지 기다리고,
# 실패하면 직전 배포에 쓰였던 설정(.env.rollback)으로 되돌린다.
#
# 워크플로가 미리 넘겨주는 것:
#   - docker-compose.yml
#   - .env.new  (새 APP_IMAGE 와 DB 접속 정보)
#
set -euo pipefail

APP_CONTAINER="withme-app"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-180}"   # healthy 대기 상한(초)
KEEP_IMAGES="${KEEP_IMAGES:-3}"           # 로컬에 보관할 앱 이미지 개수
LOCK_FILE="${LOCK_FILE:-/tmp/withme-deploy.lock}"
LOCK_WAIT="${LOCK_WAIT:-600}"             # 선행 배포 대기 상한(초)

log() { echo "[deploy] $*"; }

# 배포 잠금.
# 워크플로의 concurrency 로 Actions 실행끼리는 이미 직렬화되지만,
# 서버에서 수동으로 스크립트를 돌리는 경우까지는 막지 못한다.
# .env 를 교체하고 헬스체크하는 구간이 겹치면 롤백 지점이 덮어써져
# 어느 이미지로 되돌려야 하는지 알 수 없게 되므로 여기서 한 번 더 잠근다.
#
# flock 이 있으면 그쪽을 쓴다(프로세스 종료 시 커널이 자동 해제).
# 없는 환경에서는 mkdir 의 원자성을 이용하되, 잠금을 쥔 프로세스가
# 이미 죽었다면 잠금을 회수해 배포가 영구히 막히지 않게 한다.
LOCK_DIR="${LOCK_FILE}.d"

acquire_lock() {
  if command -v flock >/dev/null 2>&1; then
    exec 9>"$LOCK_FILE"
    flock -n 9 && return 0
    log "다른 배포가 진행 중이다. 최대 ${LOCK_WAIT}초 대기한다."
    flock -w "$LOCK_WAIT" 9 && return 0
    return 1
  fi

  local waited=0 holder
  while ! mkdir "$LOCK_DIR" 2>/dev/null; do
    holder="$(cat "$LOCK_DIR/pid" 2>/dev/null || true)"
    if [ -n "$holder" ] && ! kill -0 "$holder" 2>/dev/null; then
      log "죽은 프로세스(pid ${holder})가 남긴 잠금을 회수한다."
      rm -rf "$LOCK_DIR"
      continue
    fi
    if [ "$waited" -eq 0 ]; then
      log "다른 배포가 진행 중이다. 최대 ${LOCK_WAIT}초 대기한다."
    fi
    [ "$waited" -ge "$LOCK_WAIT" ] && return 1
    sleep 2
    waited=$((waited + 2))
  done

  echo $$ > "$LOCK_DIR/pid"
  trap 'rm -rf "$LOCK_DIR"' EXIT
  return 0
}

if ! acquire_lock; then
  log "선행 배포가 ${LOCK_WAIT}초 안에 끝나지 않았다. 이번 배포를 중단한다."
  exit 1
fi
log "배포 잠금 획득"

# 컨테이너가 healthy 가 될 때까지 대기한다.
# 도중에 컨테이너가 죽으면 상한을 기다리지 않고 즉시 실패로 처리한다.
wait_healthy() {
  local elapsed=0 status running

  while [ "$elapsed" -lt "$HEALTH_TIMEOUT" ]; do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' \
      "$APP_CONTAINER" 2>/dev/null || echo missing)"

    case "$status" in
      healthy)
        log "컨테이너 healthy (${elapsed}초 경과)"
        return 0
        ;;
      unhealthy)
        log "컨테이너가 unhealthy 상태로 판정됨"
        return 1
        ;;
      none)
        log "healthcheck 가 정의되어 있지 않다. compose 설정을 확인하라."
        return 1
        ;;
    esac

    running="$(docker inspect --format '{{.State.Running}}' "$APP_CONTAINER" 2>/dev/null || echo false)"
    if [ "$running" = "false" ] && [ "$elapsed" -gt 10 ]; then
      log "컨테이너가 실행 중이 아니다 (기동 실패로 판단)"
      return 1
    fi

    sleep 5
    elapsed=$((elapsed + 5))
  done

  log "healthy 대기 시간 초과 (${HEALTH_TIMEOUT}초)"
  return 1
}

dump_diagnostics() {
  log "----- 컨테이너 상태 -----"
  docker compose ps || true
  log "----- 애플리케이션 로그 (최근 100줄) -----"
  docker compose logs --tail=100 app || true
  log "----- healthcheck 결과 -----"
  docker inspect --format '{{json .State.Health}}' "$APP_CONTAINER" 2>/dev/null || true
}

# 배포에 성공한 이미지만 남기고 오래된 앱 이미지를 정리한다.
# 롤백 대상(직전 이미지)은 반드시 보존해야 하므로 최소 KEEP_IMAGES 개를 남긴다.
cleanup_old_images() {
  local repo
  repo="$(grep -E '^APP_IMAGE=' .env | cut -d= -f2- | cut -d: -f1)"
  [ -n "$repo" ] || return 0

  docker images "$repo" --format '{{.ID}} {{.CreatedAt}}' \
    | sort -k2 -r \
    | tail -n +$((KEEP_IMAGES + 1)) \
    | awk '{print $1}' \
    | xargs -r docker rmi 2>/dev/null || true

  docker image prune -f >/dev/null 2>&1 || true
}

if [ ! -f .env.new ]; then
  log ".env.new 가 없다. 워크플로가 파일을 전송했는지 확인하라."
  exit 1
fi

# 현재 동작 중인 설정을 롤백용으로 보관한 뒤 새 설정으로 교체한다.
if [ -f .env ]; then
  cp .env .env.rollback
  chmod 600 .env.rollback
  log "롤백 지점 저장: $(grep -E '^APP_IMAGE=' .env.rollback | cut -d= -f2-)"
else
  rm -f .env.rollback
  log "이전 배포 기록이 없다. 최초 배포로 진행한다 (롤백 불가)."
fi

mv .env.new .env
chmod 600 .env
log "배포 대상: $(grep -E '^APP_IMAGE=' .env | cut -d= -f2-)"

docker compose pull
docker compose up -d --remove-orphans

if wait_healthy; then
  log "배포 성공"
  cleanup_old_images
  exit 0
fi

log "배포 검증 실패. 롤백을 시도한다."
dump_diagnostics

if [ ! -f .env.rollback ]; then
  log "롤백할 이전 배포가 없다. 실패 상태로 종료한다."
  exit 1
fi

cp .env.rollback .env
chmod 600 .env
log "롤백 대상: $(grep -E '^APP_IMAGE=' .env | cut -d= -f2-)"

docker compose pull
docker compose up -d --remove-orphans

if wait_healthy; then
  log "롤백 성공. 서버는 직전 이미지로 서비스 중이다."
else
  log "롤백마저 실패했다. 수동 조치가 필요하다."
  dump_diagnostics
fi

# 롤백 성공 여부와 무관하게 이번 배포는 실패이므로 워크플로를 실패 처리한다.
exit 1
