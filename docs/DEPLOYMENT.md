# 배포 가이드

## 구성

- CI: PR(`main`, `develop`) 시 빌드 + 테스트 (H2 인메모리)
- CD: `develop` push 또는 수동 실행 시 GHCR 이미지 빌드 → EC2 배포
- 배포 방식: Blue-Green 무중단. nginx(8080)가 `withme-app-blue` / `withme-app-green` 중
  한쪽만 바라보고, 새 색이 healthy 가 된 뒤 upstream 을 전환한다. 자세한 내용은 [BLUE_GREEN.md](./BLUE_GREEN.md)
- 배포 검증: `/actuator/health` 기반 컨테이너 healthcheck. 실패하면 전환하지 않으므로 라이브는 무손상

## GitHub 설정

### Secrets

| 이름 | 설명                                                       |
|---|----------------------------------------------------------|
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인                                        |
| `EC2_USER` | `ubuntu` 또는 `ec2-user`                                   |
| `EC2_SSH_KEY` | `.pem` 파일 전체 내용                                          |
| `DB_URL` | **완전한 JDBC URL** (`jdbc:postgresql://<host>:5432/withme`) |
| `DB_USERNAME` | DB 계정                                                    |
| `DB_PASSWORD` | DB 비밀번호                                                  |
| `JWT_SECRET` | JWT 서명 키                                                 |
| `MAIL_USERNAME` | 인증코드 발송 계정 (Gmail 주소)                                  |
| `MAIL_PASSWORD` | 해당 계정의 앱 비밀번호. 일반 로그인 비밀번호로는 SMTP 인증이 안 된다.        |
| `KAKAO_CLIENT_ID` | 카카오 앱 REST API 키                                          |
| `KAKAO_CLIENT_SECRET` | 카카오 앱 Client Secret                                      |
| `KAKAO_REDIRECT_URI` | 카카오 로그인 Redirect URI. 카카오 개발자 콘솔에 등록한 값과 정확히 같아야 한다.      |
| `DISCORD_WEBHOOK_URL` | (선택) 5xx 알림용 Discord 웹훅 URL. 미등록이면 알림만 꺼지고 배포는 정상 진행. |
| `DISCORD_WEBHOOK_ENABLED` | (선택) 미등록이면 `true`. 웹훅 URL 은 두고 알림만 끄고 싶을 때 `false` 로 등록. |

`GITHUB_TOKEN` 은 자동 제공되므로 등록하지 않는다.

### Variables

| 이름 | 기본값 | 용도 |
|---|---|---|
| `DDL_AUTO` | `validate` | 최초 스키마 생성 시에만 `update` |
| `FLYWAY_ENABLED` | `true` | 최초 스키마 생성 시에만 `false` |

## 최초 배포 절차

기존 마이그레이션은 대상 테이블이 **이미 존재한다고 가정**한다
(`V1` → `user_token`, `V2` → `categories`).
따라서 빈 DB 에 Flyway 를 먼저 돌리면
`relation "user_token" does not exist` 로 기동에 실패한다.

순서를 지켜야 한다.

### 1단계 — Hibernate 로 테이블 생성

Variables 에 다음을 설정하고 CD 실행:

```
DDL_AUTO=update
FLYWAY_ENABLED=false
```

배포 후 테이블 생성 확인:

```bash
psql -h <DB호스트> -U <user> -d withme -c '\dt'
```

### 2단계 — Flyway 로 전환

Variables 를 다음으로 변경하고 CD 재실행:

```
DDL_AUTO=validate      (또는 변수 삭제)
FLYWAY_ENABLED=true    (또는 변수 삭제)
```

`baseline-on-migrate: true` + `baseline-version: 0` 설정에 의해
Flyway 가 기존 스키마를 버전 0 으로 baseline 한 뒤 `V1` 부터 순서대로 적용한다.

> `baseline-version` 을 지정하지 않으면 Flyway 기본값이 `1` 이라
> baseline 이 `V1` 을 "이미 적용됨"으로 표시하고 건너뛴다. 반드시 `0` 이어야 한다.

적용 결과 확인:

```bash
psql -h <DB호스트> -U <user> -d withme -c 'SELECT version, description, success FROM flyway_schema_history;'
psql -h <DB호스트> -U <user> -d withme -c '\di uq_user_token_active'
psql -h <DB호스트> -U <user> -d withme -c "SELECT conname, condeferrable, condeferred FROM pg_constraint WHERE conname = 'uk_category_user_sort_order';"
```

`uk_category_user_sort_order` 는 `condeferrable`/`condeferred` 가 모두 `t` 여야 한다.
카테고리 재정렬은 목록 전체의 `sort_order` 를 다시 매기는 과정에서
일시적으로 중복 값을 거치므로, 즉시 검사하는 제약이면 정상 재정렬이 실패한다.

> `V2` 는 제약을 걸기 전에 기존 `sort_order` 를 사용자별로 `0..n-1` 로 정규화한다.
> 사용자별 잠금이 없던 시절의 경합으로 중복/구멍이 남아 있을 수 있기 때문이다.
> 순서 자체는 보존되지만 값이 바뀌므로, 적용 전 `categories` 백업을 권장한다.

### 3단계 — 이후 스키마 변경

`DDL_AUTO` 는 `validate` 로 고정하고, 스키마 변경은 `db/migration` 에
`V2__*.sql`, `V3__*.sql` 을 추가해서 관리한다.
운영에서 `update` 를 다시 켜면 Flyway 이력과 실제 스키마가 어긋난다.

## EC2 사전 준비

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-v2
sudo usermod -aG docker $USER   # 재로그인 필요
mkdir -p ~/withme
```

보안그룹: 8080 인바운드 오픈(nginx). DB 인스턴스는 EC2 사설 IP 에서만 5432 허용.

인스턴스 타입은 **최소 2GB(`t3.small`)** 여야 한다.
Blue-Green 은 전환 구간에 JVM 두 개가 동시에 떠 있으므로 `t3.micro`(1GB)에서는 성립하지 않는다.
(`docker-compose.yml` 의 `mem_limit: 700m` 이 각 앱의 상한이다.)

## 롤백

Blue-Green 이므로 직전 버전 컨테이너가 반대 색으로 그대로 살아 있다.
전환은 nginx upstream 한 줄이라 1초 미만이다.

```bash
cd ~/withme
./scripts/switch.sh          # 현재 라이브 색 확인
./scripts/switch.sh blue     # 직전 색으로 되돌리기 (또는 green)
```

배포 **중** 검증에 실패한 경우에는 되돌릴 것이 없다.
새 색이 healthy 가 된 뒤에야 트래픽이 넘어가므로 라이브는 한 번도 중단되지 않는다.
`deploy.sh` 는 실패한 대기 색만 정지시킨다.

특정 커밋으로 되돌리려면 `.env` 의 대기 색 변수(`APP_IMAGE_BLUE` 또는 `APP_IMAGE_GREEN`)를
해당 SHA 로 바꾼 뒤 그 색을 띄우고 전환한다:

```bash
docker compose pull app-green && docker compose up -d app-green
./scripts/switch.sh green
```

## 로컬 실행

```bash
# dev (기본 프로필)
DB_URL=jdbc:postgresql://localhost:5432/withme \
DB_USERNAME=postgres DB_PASSWORD=postgres \
JWT_SECRET=local-dev-secret \
./gradlew bootRun
```

배포된 DB 가 사설망에만 열려 있다면 SSH 터널을 사용한다:

```bash
ssh -i key.pem -L 5432:<DB사설IP>:5432 ubuntu@<EC2퍼블릭IP>
```
