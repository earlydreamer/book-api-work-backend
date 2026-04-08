# 오늘 우리 백엔드

`backend/book-api-work-backend`는 현재 백엔드 구현 대상 디렉터리다.

2026-04-07 기준으로 이 repo에는 `Spring Boot + JPA contract scaffold`가 들어와 있다.
지금 단계의 핵심은 프론트에서 잠근 상태 계약을 backend API, JPA 모델, migration으로 정확히 옮기는 것이다.

---

## 1. 먼저 읽을 문서

1. `AGENTS.md`
2. `docs/README.md`
3. `docs/specs/2026-04-07-cross-env-handoff-v1.md`
4. `docs/specs/2026-04-05-today-our-mvp-v2.md`
5. `docs/plans/2026-04-05-today-our-mvp-execution-blueprint.md`
6. `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md`
7. `docs/specs/2026-04-07-today-us-front-progress-v1.md`

---

## 2. 현재 상태

- Spring Boot 4.0.5 기반 Gradle Kotlin DSL skeleton 생성 완료
- contract endpoint, DTO, ProblemDetail, security toggle, OpenAPI 설정 추가
- JPA entity / repository / Flyway core migration 구현 완료
- auth boundary는 Spring Security Resource Server 기준으로 연결 완료
- profile 분리 완료: 기본은 secure-by-default, `local`/`test`만 auth-disabled + H2
- Postgres Testcontainers smoke test 추가 완료, 현재 환경에서는 Docker 없으면 skip
- `.env.example` 기준 env 관리, R2 direct upload intent/complete 구현 완료
- Sweetbook sandbox 책 생성, asset 업로드, finalization 구현 완료
- 수동 주문 생성, 주문 조회, Sweetbook webhook 상태 동기화 구현 완료
- frontend는 이미 `현재 관계 / 이전 관계` 분리 정책을 기준으로 구현됨
- backend는 그 정책을 그대로 수용하는 응답 shape부터 잠근 상태

---

## 3. 현재 잠긴 핵심 정책

### 관계 기록

- 연결을 끊어도 이전 기록은 삭제하지 않음
- 이전 기록은 archive/history로 유지
- 다시 연결하면 기록 수와 책 진행도는 `0일부터 다시 시작`
- 피드는 현재 관계만 보여줌
- 보관함은 현재 관계 / 이전 관계를 구분해서 보여줌

### 책 자격

- 최근 30일 중 기록 20일 이상

### backend 해석 초안

- `couples`는 영구 pair identity가 아니라 `관계 인스턴스`로 본다
- reconnect 시 기존 `couple_id` 재사용이 아니라 새 `couple_id` 생성

이 해석은 현재 backend 계약 초안의 핵심 가정이다.
구현 전에 다시 한 번 확인하고 들어가는 게 좋다.

---

## 4. 목표 스택

- `Spring Boot 4.0.x`  현재 scaffold는 `4.0.5`
- `Java 21`
- `Spring Security`
- `Supabase Auth + Postgres`
- `Flyway`
- `Cloudflare R2`
- `WebClient`
- `DB-backed jobs`

세부 근거는 실행 청사진 문서를 따른다.

---

## 5. 로컬 비밀값 관리

- Spring은 `application.yml`에서 `.env`, `.env.local`을 순서대로 읽는다.
- `.env`와 `.env.local`은 git에 올리지 않는다.
- 새 키를 추가할 때는 `.env.example`을 먼저 갱신하고, 그다음 설정 파일을 맞춘다.
- 실제 값은 `.env`에 두고, 개인별 오버라이드가 필요하면 `.env.local`을 쓰되 추적하지 않는다.

가장 먼저 할 일은 이거다.

```bash
cp .env.example .env
```

---

## 6. 바로 시작할 때의 추천 순서

1. frontend에서 `uploads -> day-card -> book-snapshot -> orders` 실연동 연결
2. 운영 환경에 Supabase JWK 설정과 현재 사용자 hydrate를 붙이기
3. Sweetbook sandbox 실키와 webhook delivery 설정을 운영값으로 맞추기
4. 결제 confirm이 들어오면 현재 수동 주문 생성 트리거를 교체하기

핵심은 `order`보다 먼저 `relationship state`를 안정적으로 표현하는 거다.

---

## 7. 실행 모드

### local

- 목적: 프론트 계약 확인, 로컬 stub-like 개발
- 인증: 비활성
- DB: H2

```bash
cp .env.example .env
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

### default

- 목적: secure-by-default 확인
- 인증: 활성
- DB: 외부 설정 필요

```bash
TODAY_US_DB_URL=jdbc:h2:mem:todayus \
TODAY_US_DB_USERNAME=sa \
TODAY_US_DB_DRIVER=org.h2.Driver \
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=https://example.invalid/auth/v1/.well-known/jwks.json \
./gradlew bootRun
```

### prod

- 목적: 운영 Postgres + JWT 검증
- 인증: 활성
- DB: Supabase Postgres

```bash
SPRING_PROFILES_ACTIVE=prod \
TODAY_US_DB_URL='jdbc:postgresql://aws-0-ap-northeast-2.pooler.supabase.com:5432/postgres?sslmode=require' \
TODAY_US_DB_USERNAME='postgres.<project-ref>' \
TODAY_US_DB_PASSWORD=... \
TODAY_US_DB_DRIVER=org.postgresql.Driver \
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=https://<supabase-project>/auth/v1/.well-known/jwks.json \
TODAY_US_SECURITY_ALLOWED_ORIGINS=https://today-us.earlydreamer.dev \
./gradlew bootRun
```

운영에서는 `spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect`를 `prod` 프로필에 명시해 Supabase Postgres 기준 방언을 고정해 둔다.

---

## 8. 환경 변수 요약

### 데이터베이스 / 인증

- `TODAY_US_DB_URL`
- `TODAY_US_DB_USERNAME`
- `TODAY_US_DB_PASSWORD`
- `TODAY_US_DB_DRIVER`
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI`
- `TODAY_US_SUPABASE_PROJECT_URL`
- `TODAY_US_SECURITY_ALLOWED_ORIGINS`
- `TODAY_US_SECURITY_ALLOWED_ORIGIN_PATTERNS`

### Cloudflare R2

- `TODAY_US_R2_ACCOUNT_ID`
- `TODAY_US_R2_ACCESS_KEY_ID`
- `TODAY_US_R2_SECRET_ACCESS_KEY`
- `TODAY_US_R2_BUCKET`
- `TODAY_US_R2_PUBLIC_BASE_URL`
- `TODAY_US_R2_UPLOAD_PREFIX`
- `TODAY_US_R2_PRESIGN_TTL_SECONDS`

### Sweetbook sandbox

- `TODAY_US_SWEETBOOK_BASE_URL`
- `TODAY_US_SWEETBOOK_API_KEY`
- `TODAY_US_SWEETBOOK_BOOK_SPEC_ID`
- `TODAY_US_SWEETBOOK_TEMPLATE_ID`
- `TODAY_US_SWEETBOOK_WEBHOOK_SECRET`

### 실행 감각

- `local`은 H2 + auth 비활성으로 돌린다.
- `default`는 secure-by-default라서 DB/JWT 값을 직접 넣어야 한다.
- `prod`는 Postgres/JWT/R2/Sweetbook 값을 모두 환경 변수로 받는다.
- 프론트 배포 도메인은 `TODAY_US_SECURITY_ALLOWED_ORIGINS`에 정확한 origin으로 넣는다.
- Cloudflare Pages preview까지 열어야 하면 `TODAY_US_SECURITY_ALLOWED_ORIGIN_PATTERNS`에 `https://*.pages.dev` 같은 패턴을 추가한다.
- `test`는 `src/test/resources/application-test.yml`의 고정값으로 돌아간다.

---

## 9. 아직 열린 결정

1. reconnect 시 새 `couple_id` 생성 가정을 최종 확정할지
2. `GET /api/v1/me/home`를 BFF 응답으로 둘지, resource 조합형으로 둘지
3. archive 응답을 sectioned view model로 줄지, raw resource로 줄지
4. admin과 public을 실제로 어느 시점에 분리할지

---

## 10. 지금 있는 핵심 파일

- `build.gradle.kts`
- `src/main/java/dev/earlydreamer/todayus/TodayUsBackendApplication.java`
- `src/main/java/dev/earlydreamer/todayus/controller/`
- `src/main/java/dev/earlydreamer/todayus/service/TodayUsContractService.java`
- `src/main/java/dev/earlydreamer/todayus/service/OrderService.java`
- `src/main/java/dev/earlydreamer/todayus/service/SweetbookBookService.java`
- `src/main/java/dev/earlydreamer/todayus/service/SweetbookWebhookService.java`
- `src/main/java/dev/earlydreamer/todayus/service/CurrentUserProvider.java`
- `src/main/java/dev/earlydreamer/todayus/service/JwtCurrentUserProvider.java`
- `src/main/java/dev/earlydreamer/todayus/service/LocalCurrentUserProvider.java`
- `src/main/java/dev/earlydreamer/todayus/entity/`
- `src/main/java/dev/earlydreamer/todayus/repository/`
- `src/main/java/dev/earlydreamer/todayus/dto/`
- `src/main/resources/db/migration/`
- `src/main/resources/application-local.yml`
- `src/main/resources/application-prod.yml`
- `src/main/java/dev/earlydreamer/todayus/support/`
- `src/test/java/dev/earlydreamer/todayus/controller/ContractApiIntegrationTest.java`
- `src/test/java/dev/earlydreamer/todayus/controller/OrderControllerIntegrationTest.java`
- `src/test/java/dev/earlydreamer/todayus/controller/SweetbookWebhookControllerIntegrationTest.java`
- `src/test/java/dev/earlydreamer/todayus/security/`
- `src/test/java/dev/earlydreamer/todayus/repository/PostgresSchemaIntegrationTest.java`
- `docs/specs/2026-04-08-sweetbook-sandbox-book-pipeline-v1.md`
- `docs/plans/2026-04-08-sweetbook-sandbox-book-pipeline-implementation-plan.md`
- `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md`
- `docs/plans/2026-04-07-today-us-backend-contract-scaffold-plan.md`

## 11. 주의

- 이 repo는 독립 nested git repo다.
- 공통 문서와 프론트 참고 문서는 이 repo의 `docs/` 아래에 로컬 사본으로 보관한다.
- 프론트 활성 구현은 `/mnt/d/Projects/book-api-work/today-us/today-us-front` 쪽이다.
- 예전 `frontend/book-api-work-frontend`는 레거시 참고용이다.
