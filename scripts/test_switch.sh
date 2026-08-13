#!/usr/bin/env bash
#
# switch.sh 의 색 판정/치환 왕복 검증. docker 는 스텁으로 대체한다.
#   bash scripts/test_switch.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

mkdir -p "$TMP/scripts" "$TMP/nginx" "$TMP/bin"
cp "$ROOT/scripts/switch.sh" "$TMP/scripts/"
cp "$ROOT/nginx/withme.conf.default" "$TMP/nginx/withme.conf"
printf '#!/bin/sh\nexit 0\n' > "$TMP/bin/docker"
chmod +x "$TMP/bin/docker"
export PATH="$TMP/bin:$PATH"

switch() { bash "$TMP/scripts/switch.sh" "$@"; }

[ "$(switch)" = "blue" ] || { echo "FAIL: 초기값이 blue 여야 한다"; exit 1; }

switch green >/dev/null
[ "$(switch)" = "green" ] || { echo "FAIL: green 전환 후 green 이어야 한다"; exit 1; }

switch blue >/dev/null
[ "$(switch)" = "blue" ] || { echo "FAIL: blue 로 되돌아와야 한다"; exit 1; }

# 임의 문자열이 설정 파일에 써지면 안 된다.
if switch "green:8080; rm -rf /" >/dev/null 2>&1; then
  echo "FAIL: 허용되지 않은 인자가 통과했다"; exit 1
fi
[ "$(switch)" = "blue" ] || { echo "FAIL: 거부된 인자가 파일을 바꿨다"; exit 1; }

# nginx -t 실패 시 설정이 원복되어야 한다.
printf '#!/bin/sh\ncase "$*" in *nginx\\ -t*) exit 1;; esac\nexit 0\n' > "$TMP/bin/docker"
if switch green >/dev/null 2>&1; then
  echo "FAIL: nginx -t 실패인데 성공으로 끝났다"; exit 1
fi
[ "$(switch)" = "blue" ] || { echo "FAIL: 검증 실패 후 원복되지 않았다"; exit 1; }

echo "OK"
