# Blue-Green 무중단 배포 계획

## 왜 필요한가

현재 배포(`scripts/deploy.sh`)는 `docker compose up -d` 로 **같은 컨테이너를 교체**한다.
새 이미지가 healthy 가 될 때까지 40~90초 동안 8080 이 죽어 있고,
롤백은 직전 이미지를 다시 pull → 기동 → 헬스체크 하는 과정이라 다시 1~2분이 더 걸린다.

Blue-Green 은 **새 색을 띄워 healthy 를 확인한 다음에** 트래픽을 넘긴다.
- 다운타임: 0 (nginx reload 는 기존 연결을 끊지 않는다)
- 롤백: upstream 되돌리고 reload → 1초 미만 (구버전 컨테이너가 그대로 살아 있으므로)

## 구조

```
        :8080
          │
   ┌──────▼───────┐        withme-app-blue   :8080   ← 현재 라이브
   │ withme-nginx │ ──┬──▶ (upstream.conf 가 가리키는 쪽만)
   └──────────────┘   └──▶ withme-app-green  :8080   ← 대기/직전 버전
                                  │
                          withme-redis / 외부 PostgreSQL (공유)
```

- 외부에 포트를 여는 컨테이너는 **nginx 하나뿐**. 앱 컨테이너는 compose 내부 네트워크에서만 접근한다.
  보안그룹은 8080 그대로라 변경 없음.
- 라이브 색의 단일 진실 공급원은 `~/withme/nginx/upstream.conf` 한 줄이다.
  별도 상태 파일을 두면 nginx 설정과 어긋날 수 있으므로 파일 자체를 읽는다.
- 서비스 디스커버리(consul, traefik) 없음. 색이 두 개뿐인데 도입할 이유가 없다.

## 배포 흐름

1. `upstream.conf` 를 읽어 현재 라이브 색 판정 → 반대색을 `TARGET` 으로
2. `.env` 의 `APP_IMAGE_<TARGET>` 만 새 이미지로 갱신 (반대색 변수는 손대지 않는다)
3. `docker compose up -d app-$TARGET` — 라이브 색 컨테이너는 건드리지 않는다
4. `docker inspect` 로 `withme-app-$TARGET` 이 healthy 가 될 때까지 대기 (기존 `wait_healthy` 재사용)
5. 성공 → `upstream.conf` 를 TARGET 으로 rewrite → `nginx -s reload` → 전환 완료
6. 실패 → TARGET 컨테이너만 `docker compose stop app-$TARGET`, nginx 는 손대지 않음.
   **라이브는 애초에 한 번도 중단되지 않았으므로 롤백할 것이 없다.**
7. 직전 색 컨테이너는 **끄지 않고 그대로 둔다.** 다음 배포 때 덮어쓴다.
   이게 1초 롤백의 대가.

전환 후 롤백(전환은 됐는데 운영 중 문제 발견):

```bash
cd ~/withme && ./scripts/switch.sh blue   # 또는 green
```

## 변경 파일

### 1. `docker-compose.yml`

`app` 하나를 `app-blue` / `app-green` 두 개로 쪼갠다. 나머지 설정은 동일하므로 YAML 앵커로 공유한다.

```yaml
x-app: &app
  restart: unless-stopped
  stop_grace_period: 30s
  environment: ...        # 기존 app.environment 그대로
  depends_on:
    redis: { condition: service_healthy }
  healthcheck: ...        # 기존 그대로
  # 두 JVM 이 동시에 뜨므로 상한을 명시한다. 없으면 각자 호스트 메모리의 75% 를 잡으려 든다.
  mem_limit: 700m

services:
  app-blue:
    <<: *app
    image: ${APP_IMAGE_BLUE:-}
    container_name: withme-app-blue
  app-green:
    <<: *app
    image: ${APP_IMAGE_GREEN:-}
    container_name: withme-app-green

  nginx:
    image: nginx:1.27-alpine
    container_name: withme-nginx
    restart: unless-stopped
    ports: ["8080:80"]
    volumes:
      - ./nginx:/etc/nginx/conf.d:ro
```

앱 서비스의 `ports` 는 **삭제**한다. 외부 노출은 nginx 만 담당한다.

> `image: ${APP_IMAGE_BLUE:-}` 의 기본값 빈 문자열은 최초 배포에서 한쪽 색 변수가 아직
> 없을 때 compose 가 파싱 단계에서 죽지 않게 하려는 것. 실제로 그 색을 `up` 하지 않으면 문제없다.

### 2. `nginx/withme.conf` (신규, 리포지토리에 커밋)

```nginx
upstream withme {
    server withme-app-blue:8080;   # switch.sh 가 이 줄만 바꾼다
}

server {
    listen 80;

    location / {
        proxy_pass http://withme;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }
}
```

> 이 파일은 리포지토리 버전이 **초기값**이고, 이후에는 서버의 파일이 라이브 상태를 담는다.
> 따라서 CD 의 scp 대상에서 제외하고, 서버에 없을 때만 배포 스크립트가 복사한다.
> 그러지 않으면 매 배포마다 upstream 이 blue 로 리셋된다. **이 계획에서 제일 밟기 쉬운 지뢰다.**

### 3. `scripts/switch.sh` (신규)

```bash
#!/usr/bin/env bash
# 라이브 색 전환. 인자 없으면 현재 색 출력.
set -euo pipefail
CONF=nginx/withme.conf
current() { grep -oE 'withme-app-(blue|green)' "$CONF" | head -1 | sed 's/withme-app-//'; }
[ $# -eq 0 ] && { current; exit 0; }
sed -i "s/withme-app-[a-z]*:8080/withme-app-$1:8080/" "$CONF"
docker exec withme-nginx nginx -t && docker exec withme-nginx nginx -s reload
echo "[switch] -> $1"
```

`nginx -t` 로 먼저 검증한다. 깨진 설정으로 reload 하면 nginx 는 옛 설정을 유지하지만,
그 사실을 모르고 "전환됐다"고 판단하는 게 더 위험하다.

### 4. `scripts/deploy.sh` 수정

기존 구조(잠금 / 필수값 검증 / `wait_healthy` / 진단 덤프 / 이미지 정리)는 그대로 쓴다. 바뀌는 것만:

| 지점 | 변경 |
|---|---|
| `APP_CONTAINER` 고정값 | `withme-app-$TARGET` 로 계산 |
| `.env` 통째 교체 → `.env.rollback` | 색별 이미지 변수만 갱신. `.env.rollback` / 그 관련 롤백 분기는 **삭제** (nginx 가 롤백 지점이다) |
| `compose_up` | `docker compose up -d app-$TARGET` |
| 필수값 검증 루프 | `APP_IMAGE` → `APP_IMAGE_$TARGET` |
| 성공 경로 | `switch.sh $TARGET` 추가 |
| 실패 경로 | `docker compose stop app-$TARGET` 후 exit 1 (라이브는 무손상) |
| 최초 실행 | `nginx/withme.conf` 가 없으면 리포지토리 사본 복사 + `docker compose up -d nginx redis` |

배포 잠금(`flock`/`mkdir`)은 **그대로 유지한다.** 두 배포가 겹치면 같은 색을 동시에 덮어쓴다.

### 5. `.github/workflows/cd.yml` 수정

- `write_env APP_IMAGE` → 색은 서버가 정하므로 **워크플로는 색을 모른다.**
  `IMAGE_REF` 만 환경변수로 넘기고, `.env` 의 색별 변수 갱신은 `deploy.sh` 가 한다.
- scp `source` 에 `nginx/withme.conf`, `scripts/switch.sh` 추가.
  단, `withme.conf` 는 라이브 상태를 담으므로 `nginx/withme.conf.default` 라는 이름으로 보내고
  `deploy.sh` 가 없을 때만 복사하게 한다. (2번 항목의 지뢰 회피)
- 나머지(stale guard, concurrency, 개행 검증, `$` 이스케이프)는 손대지 않는다.

### 6. `docs/DEPLOYMENT.md` 수정

"롤백" 절을 `switch.sh` 기준으로 교체. Secrets/Variables 표는 변경 없음.

## 반드시 지켜야 할 제약

**1. 전환 구간에 신·구 버전이 동시에 같은 DB 를 본다.**
Flyway 마이그레이션은 구버전도 살아남을 수 있어야 한다.
컬럼 삭제·이름 변경·NOT NULL 추가는 한 번에 하면 안 되고 2단계로 나눈다
(추가 → 양쪽 배포 → 다음 배포에서 제거). 이건 Blue-Green 을 쓰는 순간 생기는 영구 규칙이다.

**2. EC2 메모리.**
JVM 2개 + Redis + nginx. `t3.micro`(1GB)에서는 성립하지 않는다. **최소 2GB**(`t3.small`) 필요.
현 인스턴스 타입 확인이 이 계획의 유일한 선행 조건이다.
당장 올릴 수 없다면 `mem_limit: 400m` 으로 낮추고 스왑을 잡는 방법이 있으나 권장하지 않는다.

**3. Redis / 세션.**
두 색이 같은 Redis 를 공유하므로 전환 시 세션·리프레시 토큰은 그대로 유지된다.
Redis 에 저장하는 값의 직렬화 포맷을 바꾸면 구버전이 읽지 못한다. 포맷 변경도 위 1번과 같은 2단계 규칙.

**4. `@Scheduled` 작업.**
현재 코드에 없음(확인함). 추가하는 순간 두 색이 동시에 실행하므로,
그때는 Redis 락 또는 ShedLock 이 필요하다. **지금은 하지 않는다.**

## 작업 순서

1. EC2 인스턴스 메모리 확인 (선행 조건)
2. `nginx/withme.conf`, `scripts/switch.sh` 추가 + `docker-compose.yml` 분리
3. `deploy.sh` 수정
4. `cd.yml` 수정
5. 서버 최초 전환: 현재 `withme-app` 정지 → 새 compose 로 `nginx` + `app-blue` 기동
   (**이 한 번만 다운타임 발생.** 이후로는 0)
6. 검증: `develop` 에 커밋 push → 배포 중 `while true; do curl -s -o /dev/null -w '%{http_code}\n' http://<EC2>:8080/actuator/health; sleep 0.2; done`
   가 200 만 찍는지 확인. `scripts/k6/load.js` 를 배포와 동시에 돌리면 더 확실하다.
7. 롤백 리허설: `./scripts/switch.sh <직전색>` 이 1초 내 동작하는지 확인

6번을 통과하지 못하면 무중단이 아니라 "다운타임이 짧아진 배포"일 뿐이다. 반드시 측정한다.