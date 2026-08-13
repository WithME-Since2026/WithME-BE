#!/usr/bin/env bash
#
# EC2에서 실행되는 Blue-Green 배포 스크립트.
#
# 라이브가 아닌 색으로 새 이미지를 띄우고, healthy 를 확인한 뒤에야
# nginx upstream 을 넘긴다. 검증에 실패하면 라이브는 애초에 건드리지 않았으므로
# 되돌릴 것이 없다(대기 색만 정지시킨다).
#
# 워크플로가 미리 넘겨주는 것:
#   - docker-compose.yml, scripts/switch.sh, nginx/withme.conf.default
#   - .env.new    (색과 무관한 설정값. APP_IMAGE 는 들어 있지 않다)
#   - IMAGE_REF   (환경변수. 색은 서버가 정하므로 워크플로는 색을 모른다)
#
set -euo pipefail

CONF="nginx/withme.conf"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-180}"   # healthy 대기 상한(초)
KEEP_IMAGES="${KEEP_IMAGES:-3}"           # 로컬에 보관할 앱 이미지 개수(양쪽 색 + 여유)
LOCK_FILE="${LOCK_FILE:-/tmp/withme-deploy.lock}"
LOCK_WAIT="${LOCK_WAIT:-600}"             # 선행 배포 대기 상한(초)

log() { echo "[deploy] $*"; }

# 배포 잠금.
# 워크플로의 concurrency 로 Actions 실행끼리는 이미 직렬화되지만,
# 서버에서 수동으로 스크립트를 돌리는 경우까지는 막지 못한다.
# 두 배포가 겹치면 같은 색을 동시에 덮어써 라이브가 통째로 날아가므로
# 여기서 한 번 더 잠근다.
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
  docker compose logs --tail=100 "app-${TARGET}" || true
  log "----- healthcheck 결과 -----"
  docker inspect --format '{{json .State.Health}}' "$APP_CONTAINER" 2>/dev/null || true
}

# 배포에 성공한 이미지만 남기고 오래된 앱 이미지를 정리한다.
# 두 색이 각자 다른 이미지를 붙들고 있으므로 최소 KEEP_IMAGES 개를 남긴다.
cleanup_old_images() {
  local repo
  repo="$(grep -E '^APP_IMAGE_' .env | head -1 | cut -d= -f2- | cut -d: -f1)"
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

if [ -z "${IMAGE_REF:-}" ]; then
  log "IMAGE_REF 가 비어 있다. 워크플로가 환경변수를 전달했는지 확인하라."
  exit 1
fi

# GitHub Secret 이 비어 있어도 .env 는 `KEY=` 형태로 생성되므로 파일 존재만으로는 알 수 없다.
# 값이 비면 컨테이너가 기동 직후 죽어 헬스체크 타임아웃까지 기다리게 되므로 여기서 먼저 걸러낸다.
missing=""
for key in DB_URL DB_USERNAME DB_PASSWORD JWT_SECRET; do
  value="$(grep -E "^${key}=" .env.new | cut -d= -f2- || true)"
  [ -n "$value" ] || missing="${missing} ${key}"
done
if [ -n "$missing" ]; then
  log "필수 환경변수가 비어 있다:${missing}"
  log "GitHub Secrets/Variables 설정을 확인하라. 배포를 중단한다."
  exit 1
fi

# 라이브 색은 nginx 설정 파일 자체에서 읽는다(인자 없는 switch.sh 가 현재 색을 출력한다).
# 별도 상태 파일을 두면 nginx 가 실제로 보고 있는 값과 어긋날 수 있다.
if [ -f "$CONF" ]; then
  LIVE="$(bash scripts/switch.sh)"
  case "$LIVE" in
    blue)  TARGET=green ;;
    green) TARGET=blue ;;
    *)
      log "$CONF 에서 라이브 색을 읽지 못했다. 파일을 확인하라."
      exit 1
      ;;
  esac
  log "현재 라이브: ${LIVE} -> 배포 대상: ${TARGET}"
else
  # 최초 배포. 설정 파일이 가리키는 색으로 띄운다.
  # 반대 색으로 띄우면 nginx 가 존재하지 않는 upstream 을 resolve 하지 못해 기동에 실패한다.
  cp nginx/withme.conf.default "$CONF"
  LIVE=""
  TARGET="$(bash scripts/switch.sh)"
  log "nginx 설정이 없다. 최초 배포로 ${TARGET} 을(를) 띄운다."
fi

APP_CONTAINER="withme-app-${TARGET}"

# 색별 이미지 변수만 갱신하고, 반대 색 변수는 그대로 살려 둔다.
# 살아 있는 구버전 컨테이너가 곧 롤백 지점이므로 그 이미지 참조를 잃으면 안 된다.
case "$TARGET" in
  blue)  TARGET_KEY=APP_IMAGE_BLUE;  OTHER_KEY=APP_IMAGE_GREEN ;;
  green) TARGET_KEY=APP_IMAGE_GREEN; OTHER_KEY=APP_IMAGE_BLUE ;;
  *)     log "배포 대상 색이 blue/green 이 아니다: '${TARGET}'"; exit 1 ;;
esac

other_image="$(grep -E "^${OTHER_KEY}=" .env 2>/dev/null | cut -d= -f2- || true)"
# 최초 배포에는 반대 색 이미지가 없다. compose 가 빈 image 로 파싱 단계에서
# 걸리지 않도록 같은 이미지로 채워 둔다(그 색을 up 하지는 않는다).
[ -n "$other_image" ] || other_image="$IMAGE_REF"

umask 077
{
  cat .env.new
  printf '%s=%s\n%s=%s\n' "$TARGET_KEY" "$IMAGE_REF" "$OTHER_KEY" "$other_image"
} > .env
chmod 600 .env
rm -f .env.new
log "배포 대상 이미지: ${IMAGE_REF}"

# compose 명령을 조건문 안에서 실행한다. 그냥 나열하면 set -e 가
# 실패 시 즉시 종료시켜 진단 출력도 대기 색 정리도 하지 못한다.
# nginx 기동과 전환도 같은 이유로 then 블록이 아니라 조건절에 둔다.
# (nginx 는 최초 배포에서만 실제로 기동한다. 이미 떠 있으면 no-op.
#  대상 앱이 healthy 가 된 뒤에 올려야 upstream resolve 에 실패하지 않는다.)
if docker compose pull "app-${TARGET}" \
  && docker compose up -d --remove-orphans "app-${TARGET}" \
  && wait_healthy \
  && docker compose up -d nginx \
  && bash scripts/switch.sh "$TARGET"; then
  log "배포 성공. 라이브: ${TARGET}"
  cleanup_old_images
  exit 0
fi

log "배포 또는 검증 실패."
dump_diagnostics

# 라이브 색은 이번 배포에서 한 번도 건드리지 않았으므로 되돌릴 것이 없다.
# 실패한 대기 색만 정지시킨다.
docker compose stop "app-${TARGET}" || true
log "라이브(${LIVE:-없음, 최초 배포})는 무손상이다. ${TARGET} 만 정지했다."

exit 1
