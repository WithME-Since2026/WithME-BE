#!/usr/bin/env bash
#
# 라이브 색 전환. 인자가 없으면 현재 색을 출력한다.
#
#   ./scripts/switch.sh          # blue | green 출력
#   ./scripts/switch.sh green    # green 으로 전환
#
set -euo pipefail

cd "$(dirname "$0")/.."
CONF=nginx/withme.conf

current() { sed -n 's/.*withme-app-\([a-z]*\):8080.*/\1/p' "$CONF" | head -1; }

if [ ! -f "$CONF" ]; then
  echo "[switch] $CONF 가 없다. 배포가 아직 한 번도 성공하지 않았다." >&2
  exit 1
fi

if [ $# -eq 0 ]; then
  current
  exit 0
fi

# sed 로 파일에 직접 써넣기 전에 값을 가둔다.
case "$1" in
  blue|green) ;;
  *) echo "[switch] blue 또는 green 만 허용된다: $1" >&2; exit 1 ;;
esac

# nginx -t 로 먼저 검증한다. 깨진 설정으로 reload 하면 nginx 는 옛 설정을 유지하지만,
# 그 사실을 모르고 "전환됐다"고 판단하는 게 더 위험하다.
# 검증에 실패하면 파일도 되돌린다. 그러지 않으면 설정 파일과 실제 라이브 색이 어긋난다.
cp "$CONF" "$CONF.bak"
sed -i "s/withme-app-[a-z]*:8080/withme-app-$1:8080/" "$CONF"

if docker exec withme-nginx nginx -t && docker exec withme-nginx nginx -s reload; then
  rm -f "$CONF.bak"
  echo "[switch] -> $1"
else
  mv "$CONF.bak" "$CONF"
  echo "[switch] nginx 검증/reload 실패. 설정을 원복했다." >&2
  exit 1
fi
