# 배포 가이드

## 구성

- CI: PR(`main`, `develop`) 시 빌드 + 테스트 (H2 인메모리)
- CD: `develop` push 또는 수동 실행 시 GHCR 이미지 빌드 → EC2 배포
- 배포 검증: `/actuator/health` 기반 컨테이너 healthcheck, 실패 시 직전 이미지로 자동 롤백

## GitHub 설정

### Secrets

| 이름 | 설명 |
|---|---|
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 |
| `EC2_USER` | `ubuntu` 또는 `ec2-user` |
| `EC2_SSH_KEY` | `.pem` 파일 전체 내용 |
| `DB_URL` | **완전한 JDBC URL** (`jdbc:postgresql://<host>:5432/withme`) |
| `DB_USERNAME` | DB 계정 |
| `DB_PASSWORD` | DB 비밀번호 |
| `JWT_SECRET` | JWT 서명 키 |

`GITHUB_TOKEN` 은 자동 제공되므로 등록하지 않는다.

### Variables

| 이름 | 기본값 | 용도 |
|---|---|---|
| `DDL_AUTO` | `validate` | 최초 스키마 생성 시에만 `update` |
| `FLYWAY_ENABLED` | `true` | 최초 스키마 생성 시에만 `false` |

## 최초 배포 절차

Flyway 마이그레이션 `V1__add_partial_unique_index.sql` 은 `user_token` 테이블이
**이미 존재한다고 가정**한다. 따라서 빈 DB 에 Flyway 를 먼저 돌리면
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
Flyway 가 기존 스키마를 버전 0 으로 baseline 한 뒤 `V1` 을 적용한다.

> `baseline-version` 을 지정하지 않으면 Flyway 기본값이 `1` 이라
> baseline 이 `V1` 을 "이미 적용됨"으로 표시하고 건너뛴다. 반드시 `0` 이어야 한다.

적용 결과 확인:

```bash
psql -h <DB호스트> -U <user> -d withme -c 'SELECT version, description, success FROM flyway_schema_history;'
psql -h <DB호스트> -U <user> -d withme -c '\di uq_user_token_active'
```

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

보안그룹: 8080 인바운드 오픈. DB 인스턴스는 EC2 사설 IP 에서만 5432 허용.

## 롤백

배포 실패 시 `scripts/deploy.sh` 가 자동으로 직전 이미지로 되돌린다.
수동 롤백이 필요하면 EC2 에서:

```bash
cd ~/withme
cp .env.rollback .env
docker compose up -d
```

특정 커밋으로 되돌리려면 `.env` 의 `APP_IMAGE` 태그를 해당 SHA 로 바꾼 뒤
`docker compose pull && docker compose up -d`.

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
