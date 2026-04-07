# 오늘 우리 MVP 실행 청사진

작성일: 2026-04-05
상태: Reviewed Draft
연결 스펙: `docs/specs/2026-04-05-today-our-mvp-v2.md`

## 1. 목적

이 문서는 `오늘 우리` MVP를 실제 구현 가능한 수준으로 내리기 위한 실행 청사진이다.

주의:

- 이 문서는 2026-04-05 기준 청사진이다.
- 2026-04-07 기준 활성 프론트 구현은 `today-us/today-us-front`에서 진행 중이다.
- `frontend/book-api-work-frontend`는 레거시 참고용으로 남아 있다.
- backend는 여전히 `backend/book-api-work-backend` 기준으로 잡는다.

이 문서는 원래 workspace 루트에서 작성됐지만, 지금은 각 구현 repo의 `docs/` 아래에 사본이 있다.
실제 코드 디렉터리는 아래처럼 봐야 한다.

- `backend/book-api-work-backend`
- `today-us/today-us-front` ← 현재 프론트 기준
- `frontend/book-api-work-frontend` ← 레거시 참고용

문서 작성 당시에는 두 구현 레포가 비어 있었지만, 현재는 프론트만 먼저 상태 계약 정렬이 진행됐다.
따라서 이 문서는 특정 프레임워크보다 `도메인 경계`, `상태 머신`, `API 계약`, `구현 순서`를 먼저 고정하는 큰 그림 문서로 읽어야 한다.

레포 원칙:
- 장기 구조 목표는 `frontend`와 `backend` 두 저장소/구현 축으로 수렴하는 것이다.
- public/admin은 별도 제품 surface이지만, 추가 저장소로 쪼개지 않고 `frontend` repo 내부에서 관리한다.

## 2. 이번 단계에서 잠긴 것

- 제품 본체는 `커플 하루 카드 기록`
- 차별점은 `실제 주문 가능한 책 생성`
- 첫 SKU는 `PHOTOBOOK_A4_SC`, `24p`, `오늘 우리 30일 북`
- 주문 대상 기간은 `snapshot_created_at` 기준 최근 30일의 rolling window
- 주문 조건은 `최근 30일 중 기록 20일 이상`
- MVP 주문 정책은 `1회 주문 = 1권 = 1개 배송지`
- 책 생성은 `snapshot 기반 비동기 빌드`
- 주문 상태 동기화는 `Sweetbook 웹훅 + 상세 조회 fallback`
- 프론트 구현은 `디자인 시스템과 레이아웃 선행` 원칙을 따른다.

### 2.0 Design-first gate

프론트는 더 이상 `화면부터 빠르게 만든 뒤 나중에 다듬는 방식`으로 진행하지 않는다.

이 프로젝트에서 그 접근이 실패한 이유:
- API보다 먼저 흔들린 것은 디자인 언어였다.
- 화면을 먼저 만들면, 구현 속도는 빨라도 서비스 톤과 레이아웃 문법이 섞이기 쉽다.
- 이 앱은 `공개 SNS도 아니고`, `포토북 커머스도 아니고`, `SaaS 대시보드도 아니다`.

따라서 프론트 선행 조건을 이렇게 고정한다.

1. `DESIGN.md` 작성 및 승인
2. public 핵심 화면의 layout blueprint 확정
3. 그 다음에 mock screen 구현
4. 그 다음 typed contract
5. 마지막에 실제 API 연동

즉, 앞으로 `design system -> layout shell -> mock screens -> contract -> API` 순서로 간다.

### 2.1 플랫폼 스택 결정

- 프론트엔드: `React + TypeScript + Vite`
- 백엔드 API: `Spring Boot 4.0.3 + Java 21`
- 보안: `Spring Security + stateless JWT Bearer authentication`
- 데이터베이스: `Supabase Postgres + Flyway`
- 파일 저장: `Cloudflare R2`
- 인증: `Supabase Auth`
- 비동기 오케스트레이션: `Spring Boot background jobs + DB-backed job tables`
- 운영 관측성: `Spring Boot Actuator + Micrometer + Prometheus + Grafana`
- 운영 원칙:
  - 익숙하지 않은 edge/serverless 런타임보다 `온프레미스 Spring Boot`로 빠르게 구현한다.
  - OSS-only 신규 프로젝트이므로 짧은 지원 수명의 `3.5.x` 대신 최신 안정 `4.0.x` 패치를 사용한다.
  - 브라우저는 `Sweetbook`을 직접 호출하지 않는다. 모든 외부 인쇄 연동은 Spring 서버를 통해서만 수행한다.
  - 이미지 업로드는 브라우저 -> `R2 presigned PUT URL` -> `R2` 경로로 처리한다.
  - 주문/책 생성 파이프라인은 외부 워크플로 서비스 대신 DB 기반 잡과 스케줄러로 운영한다.
  - 빠른 구현을 위해 복잡한 hexagonal 구조 대신 `controller -> service -> repository/client` 중심의 layered architecture로 간다.
  - public과 admin은 path가 아니라 `1-depth 서브도메인`으로 분리한다.
  - 기본 호스트는 `bookservice.earlydreamer.dev`, `bookservice-admin.earlydreamer.dev`를 사용한다.
  - 각 호스트는 Cloudflare Tunnel로 origin에 연결한다.

### 2.2 시스템 경계

```text
Browser
  -> public web (bookservice.earlydreamer.dev)
  -> admin web (bookservice-admin.earlydreamer.dev)

Cloudflare DNS / Tunnel
  -> origin reverse proxy

Spring Boot API
  -> Supabase Auth / Postgres
  -> R2 signed upload intent
  -> background job enqueue
  -> Payment provider

Background workers
  -> Sweetbook Books API
  -> Sweetbook Orders API

Sweetbook / Payment provider
  -> Spring webhook endpoints
```

핵심:
- 정적 앱과 주문 파이프라인을 논리적으로 분리해 장애 반경을 줄인다.
- 인쇄와 결제는 모두 서버 측에서만 실행한다.
- 익숙한 Spring 스택으로 옮겨 구현 속도와 디버깅 생산성을 확보한다.
- public과 admin은 외부 호스트부터 분리해 권한 표면을 줄인다.
- 프론트엔드는 `React + TypeScript`
- 백엔드 API는 `Spring Boot`
- 긴 주문/책 생성 파이프라인은 `DB-backed jobs + scheduler`
- 파일 업로드는 `R2 signed PUT URL -> direct upload`
- 인증/DB는 `Supabase Auth + Supabase Postgres`
- 인증은 `JWT Bearer` 토큰을 기준으로 stateless하게 처리한다.

### 2.3 런타임 아키텍처

```text
bookservice.earlydreamer.dev
  ├─ public React app
  └─ public API 호출

bookservice-admin.earlydreamer.dev
  ├─ admin React app
  └─ admin API 호출

Spring Boot API
  ├─ public security filter chain
  ├─ admin security filter chain
  ├─ JWT verification / authorization
  ├─ day-card / snapshot / order public APIs
  ├─ admin ops / refund / retry APIs
  ├─ R2 signed upload URL issuance
  ├─ Toss payment confirm + webhook intake
  ├─ Sweetbook webhook intake
  └─ enqueues background jobs

Spring background workers
  ├─ Sweetbook book build
  ├─ finalization retries
  ├─ order creation orchestration
  └─ compensation / recovery jobs

Supabase
  ├─ Auth
  ├─ Postgres
  └─ views for operational reads

R2
  ├─ original uploads
  └─ derived cover candidates if needed
```

설계 원칙:
- 브라우저는 큰 파일을 API 서버를 거치지 않고 R2로 직접 업로드한다.
- 민감한 쓰기 작업은 모두 Spring API를 통해 수행한다.
- 장기 작업은 DB에 잡을 적재하고 백그라운드 워커가 처리한다.
- 운영자 화면은 public 앱 내부 숨김 라우트가 아니라 별도 admin 앱/호스트로 분리한다.
- 같은 백엔드 애플리케이션을 쓰더라도 public/admin은 host 기준으로 분리된 보안 체인과 CORS 정책을 적용한다.

### 2.4 인증 / 인가 원칙

- 인증은 `Supabase Auth`를 사용한다.
- access token은 `JWT Bearer` 토큰으로 전달한다.
- Spring Security Resource Server가 Supabase JWT/JWKS를 검증한다.
- `iss`, `aud`, `exp`, `nbf`를 모두 검증한다.
- 인가는 클라이언트 UI 상태가 아니라 `Spring API`의 서버 측 검사로 강제한다.
- Spring Security는 `SecurityFilterChain`을 public/admin surface로 분리한다.
- admin surface는 `bookservice-admin.earlydreamer.dev` 호스트에서만 노출한다.
- 운영자 권한은 최소 `OPS_ADMIN` role로 분리하고, 일반 커플 사용자와 섞지 않는다.
- `POST /api/v1/orders`, `POST /api/v1/book-snapshots/*`, admin API는 모두 인증 + 커플 소유권 또는 운영 role을 확인해야 한다.
- webhook endpoint는 user auth가 아니라 provider별 verification material 검증과 delivery dedupe로만 통과시킨다.
- 운영자 재시도, 수동 환불, 상태 보정 액션은 모두 `order_events` 또는 별도 audit event에 남긴다.
- 프론트엔드 라우트 가드는 UX용이고, 실제 권한 경계는 Spring API가 맡는다.
- Supabase RLS는 보조 안전장치일 뿐, 애플리케이션 권한의 source of truth로 의존하지 않는다.
- 운영자 권한은 role claim 하나만 믿지 않고 `admin_users` allowlist 또는 환경 설정된 관리자 식별자 목록으로 2차 검증한다.
- 커플 리소스 접근 제어는 `@EnableMethodSecurity` + ownership authorization service로 강제한다.
- JWT 기반 순수 API이므로 API surface는 `STATELESS`로 두고 CSRF는 비활성화한다.
- CORS는 `https://bookservice.earlydreamer.dev`, `https://bookservice-admin.earlydreamer.dev`만 허용한다.
- admin host는 추가로 IP allowlist 또는 Cloudflare Access를 2차 방어선으로 둘 수 있다.

### 2.5 프론트엔드 구현 선택

- 앱 구조: `public app` + `admin app`
- 번들러/개발 서버: `Vite`
- 라우팅: `React Router` declarative mode
- 서버 상태: `TanStack Query`
- 폼: `React Hook Form + Zod 4`
- 인증 클라이언트: `Supabase JS` (`PKCE`)

추가 원칙:
- public과 admin은 같은 숨김 라우트 SPA가 아니라 별도 앱으로 운영한다.
- public/admin은 별도 앱이지만 모두 같은 `frontend` 저장소 안에서 관리한다.
- 프론트는 SSR 없이 정적 SPA로 시작한다.
- public 앱은 모바일 우선, admin 앱은 데스크톱 우선으로 설계한다.
- 데이터 권한은 프론트가 판단하지 않고 항상 백엔드 응답을 따른다.
- 프론트의 역할은 `JWT 획득/전달`, 화면 상태 관리, optimistic UX 범위로 제한한다.
- 서버 상태는 `TanStack Query`로 관리하고, 비즈니스 source of truth를 전역 클라이언트 상태로 복제하지 않는다.
- local UI 상태는 React 기본 state/context로 시작하고, cross-cutting client state가 실제로 생길 때만 별도 상태 라이브러리를 검토한다.
- 폼 검증은 `React Hook Form + Zod`로 통일해 public/admin 간 DTO 검증 경험을 맞춘다.
- API 호출은 공통 typed fetch client를 통해서만 수행한다.
- access token refresh와 세션 bootstrap은 `Supabase JS`가 맡되, role/ownership 판정은 백엔드가 최종 결정한다.
- public/admin은 서로 다른 origin이므로 `세션 공유를 전제하지 않는다`. admin은 별도 로그인 surface로 취급한다.
- 두 앱은 각각 자신의 redirect URL과 Supabase auth callback을 가진다.
- 프론트는 `supabase.auth.onAuthStateChange`를 세션 동기화의 기준으로 삼고, 임의의 localStorage 포맷을 직접 source of truth로 다루지 않는다.
- API 호출용 access token은 메모리 기준으로 전달하고, session persistence는 Supabase client 기본 메커니즘에 맡긴다.
- 보안상 민감한 admin 기능은 public 세션 재사용보다 `admin host 별도 sign-in`을 우선한다.
- public/admin 모두 강한 CSP를 적용하고 inline script 의존을 피한다.
- admin 앱은 public 디자인 시스템을 공유하되, 테이블/필터/오퍼레이션 중심의 운영 UI 패턴을 우선한다.
- 초기 개발 순서는 `DESIGN.md와 layout shell 확정`, `mock data/fixture 기반 화면`, `typed contract 정리`, `실제 API 연동` 순서로 간다.
- 화면 검증이 먼저이므로 public/admin 모두 mock API adapter를 한 번 거치도록 설계한다.

선택 이유:
- React는 CRA를 deprecated했고, 공식적으로 프레임워크 또는 `Vite` 같은 build tool을 권장한다. 현재 구조는 별도 Spring 백엔드가 있으므로 `Vite + static SPA`가 가장 단순하다.
- React Router는 declarative/data/framework 모드를 제공하는데, 우리는 별도 백엔드와 `TanStack Query`를 쓰므로 declarative mode가 가장 단순하다.
- `TanStack Query`는 React의 비동기 server-state 관리에 적합하고, 주문/스냅샷/운영 조회처럼 stale/retry/loading 상태가 많은 앱에 잘 맞는다.
- Supabase는 브라우저 환경에서 기본적으로 session을 local storage에 persist하므로, 두 서브도메인 간 shared local storage를 기대하면 안 된다. admin을 별도 로그인 surface로 두는 편이 더 단순하고 안전하다.

### 2.6 백엔드 구현 선택

- 웹/API: `spring-boot-starter-web`
- 보안: `spring-boot-starter-security`
- 검증: `spring-boot-starter-validation`
- 데이터 접근: `spring-boot-starter-data-jpa`
- 마이그레이션: `Flyway`
- 외부 API 연동: `WebClient`
- 운영/메트릭: `spring-boot-starter-actuator`, `micrometer-registry-prometheus`
- API 문서: `springdoc-openapi`
- R2 연동: `AWS SDK v2 S3 client`를 R2 endpoint로 사용
- DB 연결: `HikariCP + Supabase pooler/SSL`
- 서블릿 컨테이너: Boot 기본 `Tomcat` 유지

추가 원칙:
- 구현 속도를 위해 전형적인 layered architecture를 사용한다.
- 패키지 기본 구조는 `controller(public/admin) -> service -> repository/client -> domain/dto/config`로 둔다.
- public controller와 admin controller는 패키지와 security matcher를 모두 분리한다.
- 도메인 엔티티와 CRUD는 JPA로 간다.
- 잡 큐, 재시도, 운영 조회처럼 제어가 필요한 경로는 SQL/JdbcTemplate를 허용한다.
- Sweetbook/Toss 같은 외부 호출은 모두 service 계층에서만 수행한다.
- 장기 작업은 별도 워크플로 엔진 대신 `job_queue` 테이블 + `@Scheduled` poller + `FOR UPDATE SKIP LOCKED` 패턴으로 처리한다.
- Supabase 인증은 JWT 검증에만 쓰고, 애플리케이션 데이터 권한은 Spring 서비스 계층에서 강제한다.
- 빠른 구현을 우선하므로 hexagonal/DDD over-segmentation은 1차에서 하지 않는다.

### 2.7 배포 토폴로지

```text
Client
  -> Cloudflare DNS
      ├─ bookservice.earlydreamer.dev
      └─ bookservice-admin.earlydreamer.dev

Cloudflare Tunnel
  -> origin Caddy or Nginx
      ├─ serve public React build
      ├─ serve admin React build
      ├─ reverse proxy public API -> Spring Boot
      └─ reverse proxy admin API -> Spring Boot

Spring Boot
  ├─ public API
  ├─ admin API
  ├─ Toss/Sweetbook webhook endpoints
  └─ background job workers

External managed services
  ├─ Supabase Postgres/Auth
  └─ Cloudflare R2

Ops sidecar
  ├─ Prometheus
  └─ Grafana
```

배포 원칙:
- MVP는 `Spring Boot 단일 애플리케이션`으로 시작한다.
- React 프론트는 public/admin을 별도 빌드 또는 별도 엔트리로 정적으로 서빙한다.
- 운영 대시보드는 `bookservice-admin.earlydreamer.dev`에서만 연다.
- 외부 경계는 path 기반이 아니라 서브도메인 기반으로 분리한다.
- 쿠버네티스, 서비스 분리, 별도 워커 프로세스 분리는 1차에서 하지 않는다.

## 3. 핵심 릴리스 슬라이스

### Slice 0. 디자인 시스템과 레이아웃 고정

목표:
- `frontend/book-api-work-frontend/DESIGN.md` 작성
- public 홈의 layout blueprint 확정
- public/admin 공통 shell, surface, typography, spacing, navigation 규칙 고정

완료 기준:
- `DESIGN.md`가 프론트 소스 오브 트루스로 존재한다.
- 홈은 `private social feed` 레이아웃 원칙이 문서와 mock에서 일치한다.
- public/admin의 공통 토큰과 shell 규칙이 정리된다.
- 이 단계가 끝나기 전에는 추가 화면 구현을 진행하지 않는다.

### Slice 1. 계정, 커플 연결, 하루 카드

목표:
- 2인 연결
- 하루 카드 생성/수정/마감
- 홈과 아카이브의 핵심 루프 동작

완료 기준:
- 사용자가 초대 코드로 연결할 수 있다.
- 하루 카드에 사진 또는 무드 카드, 감정, 메모를 저장할 수 있다.
- 부분 카드와 완성 카드가 날짜별로 보인다.

### Slice 2. 30일 북 후보 판정과 snapshot

목표:
- 최근 30일 rolling window 계산
- 주문 가능 여부 판정
- snapshot 생성과 고정

완료 기준:
- 기록 20일 이상이면 주문 후보가 생성된다.
- snapshot 생성 후 기존 데이터가 흔들리지 않는다.
- snapshot별 페이지 배치 결과를 조회할 수 있다.

### Slice 3. Sweetbook 책 빌드

목표:
- blank book 생성
- 사진 업로드
- cover / contents / finalization

완료 기준:
- snapshot 하나가 Sweetbook FINALIZED 책으로 변환된다.
- 빌드 실패 시 재시도 또는 실패 사유 확인이 가능하다.

### Slice 4. 주문/결제/배송지

목표:
- 배송지 입력
- Toss 결제 confirm 성공 이후 Sweetbook 주문 생성
- 실패 보상과 중복 방지

완료 기준:
- 사용자가 주문을 생성할 수 있다.
- 중복 클릭 시 이중 주문/이중 차감이 나지 않는다.
- `successUrl` 복귀만으로 주문 성공 처리하지 않는다.
- `결제 성공 -> 주문 실패`는 환불 대상 상태로 떨어진다.

### Slice 5. 주문 상태 동기화와 운영

목표:
- Sweetbook 웹훅 수신
- 상태 조회 화면
- 저잔액/실패 빌드 운영 알림

완료 기준:
- 주문 상태가 앱에서 갱신된다.
- webhook 재전송/중복 이벤트를 안전하게 처리한다.
- 운영자가 저잔액과 실패 주문을 확인할 수 있다.

## 4. 도메인 모델

### 4.1 사용자/커플

#### `users`

- `id`
- `auth_provider`
- `auth_user_id`
- `display_name`
- `profile_image_url`
- `role` (`USER`, `OPS_ADMIN`)
- `created_at`
- `deleted_at`

#### `user_settings`

- `user_id`
- `timezone`
- `locale`
- `morning_open_time`
- `notify_morning_open`
- `notify_partner_posted`
- `notify_card_completed`
- `notify_book`
- `hide_lock_screen_preview`

#### `couples`

- `id`
- `display_name`
- `anniversary_date`
- `status` (`ACTIVE`, `UNLINKED`)
- `created_at`
- `unlinked_at`

#### `couple_members`

- `couple_id`
- `user_id`
- `joined_at`

#### `couple_invites`

- `id`
- `couple_id`
- `code`
- `status` (`PENDING`, `ACCEPTED`, `CANCELLED`, `EXPIRED`)
- `created_by_user_id`
- `accepted_by_user_id`
- `expires_at`
- `created_at`
- `accepted_at`

### 4.2 하루 카드

#### `day_cards`

- `id`
- `couple_id`
- `local_date`
- `state` (`EMPTY`, `PARTIAL`, `COMPLETE`, `CLOSED`)
- `close_at_utc`
- `closed_at`
- `created_at`

유니크 키:
- `(couple_id, local_date)`

#### `card_entries`

- `id`
- `day_card_id`
- `user_id`
- `entry_type` (`PHOTO`, `MOOD`)
- `media_asset_id` nullable
- `mood_theme` nullable
- `emotion_code`
- `memo`
- `day_status_tag`
- `created_at`
- `updated_at`

유니크 키:
- `(day_card_id, user_id)`

#### `media_assets`

- `id`
- `owner_user_id`
- `storage_key`
- `size_bytes`
- `sha256` nullable
- `mime_type`
- `width`
- `height`
- `source` (`UPLOAD`, `CAMERA`)
- `status` (`PENDING`, `READY`, `FAILED`)
- `created_at`

### 4.3 책 snapshot

#### `book_snapshots`

- `id`
- `couple_id`
- `window_start_local_date`
- `window_end_local_date`
- `eligible_day_count`
- `status`
  - `INELIGIBLE`
  - `ELIGIBLE`
  - `SNAPSHOT_CREATED`
  - `BUILD_REQUESTED`
  - `BUILD_SUCCEEDED`
  - `BUILD_FAILED`
  - `READY_TO_ORDER`
  - `ORDERED`
  - `SUPERSEDED`
- `title`
- `book_spec_uid`
- `template_version`
- `cover_media_asset_id`
- `sale_price_krw`
- `cogs_price_krw`
- `shipping_fee_krw`
- `build_job_id` nullable
- `snapshot_version`
- `superseded_at` nullable
- `orderable_at` nullable
- `created_at`
- `updated_at`

#### `snapshot_pages`

- `id`
- `snapshot_id`
- `page_index`
- `layout_type` (`OPENING`, `DAY_SINGLE`, `DAY_PARTIAL`, `DAY_DOUBLE`, `ENDING`)
- `primary_day_card_id` nullable
- `secondary_day_card_id` nullable
- `render_payload_json`

설명:
- `24p` 고정 상품이라 실제 내지 조립은 `snapshot_pages` 기준으로 deterministic하게 만든다.

### 4.4 Sweetbook 연동

구현 원칙:
- Sweetbook 연동은 공식 SDK 없이 내부 `SweetbookClient`로 직접 구현한다.
- `SweetbookClient`는 Spring Boot에서 `WebClient` 기반으로 동작한다.
- 모든 쓰기 메서드는 호출자가 `idempotencyKey`를 넘길 수 있어야 한다.
- 응답/에러는 내부 표준 결과 타입으로 정규화한다.
- structured log에는 `snapshotId`, `orderId`, `sweetbookBookUid`, `sweetbookOrderUid`를 함께 남긴다.
- webhook verification과 delivery dedupe도 같은 모듈 책임으로 둔다.

#### `sweetbook_books`

- `id`
- `snapshot_id`
- `sweetbook_book_uid`
- `book_spec_uid`
- `status`
  - `BOOK_CREATED`
  - `PHOTOS_UPLOADED`
  - `COVER_CREATED`
  - `CONTENTS_CREATED`
  - `FINALIZED`
  - `FAILED`
- `idempotency_key`
- `last_error_code`
- `last_error_message`
- `created_at`
- `updated_at`

#### `sweetbook_uploaded_assets`

- `id`
- `sweetbook_book_id`
- `media_asset_id`
- `sweetbook_file_name`
- `created_at`

### 4.5 주문

#### `orders`

- `id`
- `snapshot_id`
- `ordering_user_id`
- `payment_provider`
- `payment_status`
  - `PENDING`
  - `REDIRECTED`
  - `CONFIRMING`
  - `SUCCEEDED`
  - `FAILED`
  - `REFUND_REQUIRED`
  - `REFUND_REQUESTED`
  - `REFUNDED`
  - `REFUND_FAILED`
- `payment_provider_status`
- `order_status`
  - `ORDER_REQUESTED`
  - `ORDER_SUCCEEDED`
  - `ORDER_FAILED`
- `sweetbook_order_uid` nullable
- `sweetbook_order_status` nullable
- `order_job_id` nullable
- `amount_krw`
- `recipient_name`
- `recipient_phone`
- `postal_code`
- `address1`
- `address2`
- `shipping_memo`
- `payment_reference`
- `payment_provider`
- `refund_reason` nullable
- `refunded_at` nullable
- `created_at`
- `updated_at`

유니크 키:
- `payment_reference`
- `sweetbook_order_uid` nullable unique

#### `order_events`

- `id`
- `order_id`
- `source` (`INTERNAL`, `PAYMENT_WEBHOOK`, `SWEETBOOK_WEBHOOK`, `SWEETBOOK_POLL`)
- `event_type`
- `dedupe_key`
- `payload_json`
- `created_at`

#### `admin_audit_events`

- `id`
- `admin_user_id`
- `action_type`
- `target_type`
- `target_id`
- `request_id`
- `payload_json`
- `created_at`

유니크 키:
- `dedupe_key`

### 4.6 백그라운드 잡

#### `job_queue`

- `id`
- `job_type` (`BOOK_BUILD`, `ORDER_CREATE`, `PAYMENT_REFUND`, `ORDER_STATUS_SYNC`)
- `target_type` (`SNAPSHOT`, `ORDER`)
- `target_id`
- `status` (`QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `DEAD_LETTER`)
- `dedupe_key`
- `payload_json`
- `attempt_count`
- `max_attempts`
- `available_at`
- `locked_at` nullable
- `locked_by` nullable
- `last_error_code` nullable
- `last_error_message` nullable
- `created_at`
- `updated_at`

유니크 키:
- `dedupe_key`

설명:
- 모든 장기 작업은 `job_queue`에 적재한다.
- poller는 `FOR UPDATE SKIP LOCKED`로 작업을 선점한다.
- 일정 시간 이상 `RUNNING` 상태로 멈춘 작업은 watchdog이 `QUEUED` 또는 `DEAD_LETTER`로 재분류한다.

## 5. 상태 머신

### 5.1 하루 카드

```text
EMPTY
  -> PARTIAL    (한 명이 기록)
  -> COMPLETE   (두 명이 모두 기록)
  -> CLOSED     (아무도 기록하지 않은 채 4시 마감)

PARTIAL
  -> COMPLETE   (다른 한 명이 기록)
  -> CLOSED     (4시 마감)

COMPLETE
  -> CLOSED     (4시 마감)
```

규칙:
- 과거 3일까지만 수정 허용
- `CLOSED` 이후에도 수정 가능 기간 안이면 entry 수정은 가능하지만, 이미 생성된 snapshot은 바뀌지 않음

### 5.2 snapshot / 책 빌드

```text
INELIGIBLE
  -> ELIGIBLE

ELIGIBLE
  -> SNAPSHOT_CREATED

SNAPSHOT_CREATED
  -> BUILD_REQUESTED

BUILD_REQUESTED
  -> BUILD_FAILED
  -> BUILD_SUCCEEDED

BUILD_SUCCEEDED
  -> READY_TO_ORDER

READY_TO_ORDER
  -> ORDERED
  -> SUPERSEDED

BUILD_FAILED
  -> BUILD_REQUESTED
  -> SUPERSEDED
```

규칙:
- 같은 30일 구간에는 `READY_TO_ORDER` 상태 snapshot 1개만 유지
- 사용자가 재생성하면 이전 candidate는 `SUPERSEDED`로 처리하거나 비노출

### 5.3 결제 / 주문

```text
payment_pending
  -> payment_redirected
  -> payment_failed

payment_redirected
  -> payment_confirming
  -> payment_failed

payment_confirming
  -> payment_succeeded
  -> payment_failed

payment_succeeded
  -> order_requested

order_requested
  -> order_succeeded
  -> order_failed

order_failed + payment_succeeded
  -> payment_refund_required
  -> payment_refund_requested

payment_refund_requested
  -> payment_refunded
  -> payment_refund_failed
```

핵심:
- `successUrl` 복귀만으로 `payment_succeeded`가 되면 안 된다.
- `payment_succeeded`인데 `order_succeeded`가 아니면 반드시 보상 상태로 떨어져야 한다.
- `Idempotency-Key`는 내부 주문 ID를 기반으로 생성한다.
- refund는 가능한 경우 자동 시도하고, 실패하면 운영자 큐로 승격한다.

## 6. 페이지 플래너 규칙

입력:
- 최근 30일 rolling window의 `day_cards`

출력:
- `24p` 고정의 `snapshot_pages`

규칙:
1. 오프닝 2p 예약
2. 엔딩 2p 예약
3. 본문 슬롯 20p 확보
4. 기록일이 20일보다 적으면 `INELIGIBLE`
5. 기록일이 20일이면 `1일 1p`
6. 기록일이 20일 초과면 아래 순서로 압축한다.
   - 부분 카드
   - 무드 카드
   - 메모 짧은 완성 카드
7. 압축 결과는 항상 deterministic해야 한다.
   - 같은 snapshot 입력이면 항상 같은 page plan이 나와야 함

## 7. Sweetbook API 매핑

| 단계 | 우리 개념 | Sweetbook API | 저장해야 할 값 |
|---|---|---|---|
| 1 | blank book 생성 | `POST /v1/books` | `sweetbook_book_uid`, request idempotency key |
| 2 | 책에 쓸 이미지 업로드 | `POST /v1/books/{bookUid}/photos` | `sweetbook_file_name` |
| 3 | 표지 생성 | `POST /v1/books/{bookUid}/cover` | cover 성공 여부 |
| 4 | 내지 생성 | `POST /v1/books/{bookUid}/contents` 반복 | page별 생성 결과 |
| 5 | 최종화 | `POST /v1/books/{bookUid}/finalization` | FINALIZED 여부 |
| 6 | 잔액 확인 | `GET /v1/credits` | `balance` |
| 7 | 주문 생성 | `POST /v1/orders` | `sweetbook_order_uid`, 상태 |
| 8 | 상태 실시간 수신 | `PUT /v1/webhooks/config` + webhook 수신 | delivery dedupe, 상태 전이 |
| 9 | 상세 동기화 fallback | `GET /v1/orders/{orderUid}` | 최신 상태 재동기화 |

보조 원칙:
- Node/Python SDK는 payload shape와 예시 확인용 레퍼런스로만 사용한다.
- 실제 프로덕션 런타임 의존성은 우리 `SweetbookClient` 하나로 통일한다.
- 재시도 가능 여부와 보상 필요 여부는 Sweetbook 원본 에러를 그대로 노출하지 말고 내부 분류 코드로 변환한다.

## 8. 백엔드 API 계약 초안

### 커플/온보딩

- `POST /api/v1/couples/invites`
- `POST /api/v1/couples/invites/accept`
- `GET /api/v1/me/home`
- `PATCH /api/v1/me/settings`

### 하루 카드

- `GET /api/v1/day-cards/today`
- `PUT /api/v1/day-cards/{localDate}/entry`
- `GET /api/v1/day-cards?cursor=...`
- `GET /api/v1/day-cards/{localDate}`

### 업로드

- `POST /api/v1/uploads/intents`
- `POST /api/v1/uploads/complete`

### 북

- `GET /api/v1/book-snapshots/current`
- `POST /api/v1/book-snapshots`
- `POST /api/v1/book-snapshots/{snapshotId}/build`
- `GET /api/v1/book-snapshots/{snapshotId}`

### 주문

- `POST /api/v1/orders`
- `POST /api/v1/orders/{orderId}/payments/confirm`
- `GET /api/v1/orders`
- `GET /api/v1/orders/{orderId}`
- `POST /api/v1/webhooks/toss-payments`
- `POST /api/v1/webhooks/sweetbook`

주문 규칙:
- `POST /api/v1/orders`는 내부 주문 레코드와 배송지, 금액 스냅샷만 생성한다.
- Toss `successUrl`은 React의 `/orders/:id/pending` 화면으로 복귀한다.
- pending 화면은 URL의 `paymentKey`, `orderId`, `amount`, `paymentType`를 읽고 `POST /api/v1/orders/{orderId}/payments/confirm`을 1회 호출한다.
- confirm endpoint는 idempotent해야 하며, 이미 성공/실패가 확정된 주문에는 재승인하지 않는다.
- 주문 실패 후 환불이 필요하면 자동 refund job을 enqueue하고, 실패 시 운영자 액션으로 승격한다.

### 운영자

- `GET /api/v1/ops/overview`
- `GET /api/v1/ops/orders?status=...`
- `GET /api/v1/ops/snapshots?status=...`
- `GET /api/v1/ops/credits`
- `POST /api/v1/ops/orders/{orderId}/retry`
- `POST /api/v1/ops/orders/{orderId}/refund`

권한 규칙:
- 일반 사용자 API는 `auth user == resource owner` 확인이 기본
- `couple_id`, `snapshot_id`, `order_id`는 모두 서버에서 소유권을 역조회한다.
- admin host의 운영 API는 `OPS_ADMIN` role + allowlist 검증 필수
- 운영 액션은 모두 audit event 기록 필수

## 9. 프론트엔드 화면 우선순위

1. 온보딩 + 초대 코드 연결
2. 홈
3. 기록 작성
4. 아카이브
5. 북 후보/주문
6. 내 공간

홈/북에서 필요한 상태:
- 아직 연결 안 됨
- 오늘 카드 비어 있음
- 상대가 먼저 기록함
- 부분 카드
- 완성 카드
- 주문 조건 미충족
- snapshot 생성 중
- 책 빌드 실패
- 주문 가능
- 주문 접수 후 상태 조회

## 10. 운영 필수 항목

- Sweetbook 충전금 저잔액 경고
- 빌드 실패 snapshot 목록
- `payment_succeeded`이지만 `order_failed`인 주문 목록
- `payment_refund_failed` 주문 목록
- webhook verification 실패 로그
- webhook delivery 중복 처리 로그
- build job 평균 소요 시간과 실패율
- 일정 시간 이상 멈춘 `RUNNING` job 목록
- 주문 성공률, 환불 필요 상태 개수
- Toss payment confirm 실패 로그
- Toss webhook 지연 시간
- 관리자 권한 거부/실패 로그

## 11. 구현 순서

1. `frontend/book-api-work-frontend/DESIGN.md`와 layout shell 고정
2. `frontend` repo에서 public/admin 핵심 화면을 mock data로 구현
3. 프론트에서 공통 typed API contract, query key, form schema를 고정
4. `backend` repo에서 인증/인가 경계와 `users/couples/orders` 도메인 모델 고정
5. 하루 카드 API와 홈/아카이브/북/운영 화면을 순서대로 실제 API에 연결
6. `job_queue`, 상태 enum, scheduler/poller 규칙 고정
7. snapshot 생성과 page planner 구현
8. 내부 `SweetbookClient`, `TossPaymentsGateway`, 에러 모델 구현
9. Sweetbook book build job과 refund job 구현
10. 주문/결제 API와 배송지/결제 대기 화면 구현
11. Toss/Sweetbook webhook 수신 및 상태 동기화 구현
12. 운영 대시보드 또는 최소 운영 조회 화면 추가

## 12. 지금 바로 이어서 만들 산출물

이 청사진 다음 순서는 아래 네 문서다.

1. `frontend/book-api-work-frontend/DESIGN.md`
2. `public home layout blueprint`
3. `프론트 route tree + mock screen inventory`
4. `백엔드 데이터 모델 상세`
5. `주문 상태 머신 상세`
6. `SweetbookClient 인터페이스 초안`

코드 착수는 `DESIGN.md -> layout shell -> 프론트 mock 화면 -> typed contract -> 백엔드 연결` 순서로 진행한다.

## 13. CEO 리뷰 보강

### 13.1 Premise Gate

이번 문서의 premise gate는 이미 통과한 것으로 본다.

이유:
- 현재 스펙은 다수의 제품 이터레이션을 거쳐 사용자가 직접 잠근 전제의 결과물이다.
- 특히 아래 전제는 사용자가 명시적으로 승인했다.
  - 첫 MVP도 실제 주문 가능한 책이어야 한다.
  - Sweetbook API 연동은 필수다.
  - 앱의 본체는 커플 하루 카드 기록이다.
  - 첫 SKU는 과한 프리미엄보다 접근 가능한 기록책이어야 한다.
  - 익숙한 Spring 중심 온프레미스 인프라로 간다.
  - Sweetbook SDK 대신 내부 REST client로 간다.
  - 디자인 시스템과 레이아웃을 먼저 정의한 뒤 화면 구현으로 간다.

### 13.2 Premise Challenge

수용한 전제:
- `커플 하루 카드 -> 실물 책`은 충분히 선명한 wedge다.
- `30일 북`은 반복 구매 리듬을 만들 수 있는 가장 현실적인 첫 상품이다.
- `결정적 snapshot + deterministic page planner`는 운영 가능성을 높인다.

위험 전제:
- `최근 30일 중 20일 기록`이 첫 주문 전환을 너무 늦추지 않을 것이라는 가정
- 두 사람 모두 꾸준히 남겨야만 제품 가치가 생긴다는 인상을 주지 않을 것이라는 가정
- 물리 책 주문 전까지도 충분한 가치가 느껴질 것이라는 가정
- Sweetbook 충전금, PG 수수료, CS 비용을 초기 운영에서 감당할 수 있다는 가정

대응:
- 주문 기준은 유지하되, `주문 가능 전 미리보기`와 `기록 누적 상태`를 더 강하게 보여준다.
- 부분 카드와 단독 기록일도 제품 자산으로 취급한다.
- 운영 지표에 `20일 도달률`, `snapshot 생성률`, `주문 전환률`을 넣는다.

### 13.3 What Already Exists

- 현재 레포에서 재사용 가능한 애플리케이션 코드는 없다.
- 재사용 가능한 것은 문서화된 제품 규칙과 Sweetbook API capability뿐이다.
- 따라서 이번 MVP의 진짜 leverage는 코드가 아니라 `결정된 도메인 규칙`이다.

### 13.4 CURRENT -> THIS PLAN -> 12-MONTH IDEAL

```text
CURRENT
  관계 기록은 사진첩, 메신저, 머릿속 기억으로 흩어져 있다.
  주문 가능한 커플 기록책 경험은 아직 없다.

THIS PLAN
  하루 카드로 기록을 쌓고
  rolling 30일 snapshot을 만들고
  Sweetbook API로 실제 주문 가능한 첫 상품을 만든다.

12-MONTH IDEAL
  커플 기록이 월간 책, 기념일 책, 운영 리포트, 추천 루프로 이어지는
  "관계 아카이브 OS"가 된다.
```

### 13.5 Implementation Alternatives

```text
APPROACH A: Deterministic Print-First MVP
  Summary: 지금 문서대로 A4 24p 단일 SKU와 결정적 snapshot 빌더로 바로 실주문까지 간다.
  Effort:  M
  Risk:    Med
  Pros:
    - 차별점인 실물 책이 첫 MVP부터 검증된다.
    - 주문/보상/운영 문제를 초기에 드러낼 수 있다.
    - 이후 SKU 확장이 쉬워진다.
  Cons:
    - PG/충전금/CS까지 같이 설계해야 한다.
    - 첫 주문 문턱이 다소 높을 수 있다.
  Reuses:
    - 현재 spec과 blueprint 대부분

APPROACH B: Concierge Print Ops
  Summary: 앱과 snapshot은 자동화하되, 첫 주문 몇 건은 운영자가 수동 검수 후 주문한다.
  Effort:  S
  Risk:    Low
  Pros:
    - 빠르게 검증 가능하다.
    - 실제 주문 실패 리스크를 운영으로 흡수할 수 있다.
  Cons:
    - API 기반 MVP라는 현재 목표와 어긋난다.
    - 자동화 학습이 늦어진다.
  Reuses:
    - snapshot, planner, ops dashboard

APPROACH C: Digital Preview First
  Summary: 디지털 북 후보를 먼저 띄우고, 물리 주문은 2차로 연다.
  Effort:  M
  Risk:    Med
  Pros:
    - 첫 주문 전에도 product value를 전달하기 쉽다.
    - 가격/물류 장벽을 늦출 수 있다.
  Cons:
    - 핵심 차별점 검증이 늦어진다.
    - 사용자가 "언제 책이 되지?"를 더 오래 기다리게 된다.
  Reuses:
    - preview UI, snapshot
```

추천:
- `APPROACH A` 유지
- 단, `주문 가능 전 미리보기`는 1차 안에 포함해 첫 가치 시점을 당긴다.

### 13.6 Dream State Delta

- 이 계획은 `첫 주문 가능한 제품`까지는 간다.
- 아직 `반복 구매가 자동으로 커지는 루프`는 부족하다.
- 12개월 ideal과의 가장 큰 차이는 `디지털 preview/recap 루프`와 `운영 자동화 깊이`다.

### 13.7 NOT in Scope

- 디지털 전용 상품 판매
- 프리미엄 하드커버 SKU
- 친구/가족 확장
- 공개 SNS 요소
- AI 자동 카피 생성
- 다중 권 주문, 장바구니, 쿠폰

### 13.8 제품 검증 게이트

초기 MVP는 아래 지표를 가설값으로 추적한다.

| Metric | Why it matters | Initial target |
|---|---|---|
| 연결 후 24시간 내 첫 기록 생성률 | 2인 activation이 실제로 일어나는지 본다 | `>= 60%` |
| 연결 후 10일 내 7일 이상 기록한 커플 비율 | 일상 기록 습관이 붙는지 본다 | `>= 35%` |
| 연결 후 45일 내 `ELIGIBLE` snapshot 도달률 | `20/30` 규칙이 과도하지 않은지 본다 | `>= 15%` |
| `READY_TO_ORDER -> paid order` 전환률 | 실제 책 구매 의사가 있는지 본다 | `>= 20%` |

해석 규칙:
- 첫 두 지표가 무너지면 print pipeline보다 activation loop를 먼저 고친다.
- 세 번째 지표가 낮으면 `20/30` 규칙 또는 pre-order preview가 잘못된 것이다.
- 네 번째 지표가 낮으면 SKU/가격/미리보기/주문 신뢰 UX를 다시 본다.

### 13.9 초기 유입 가설

MVP는 대중적 바이럴보다 아래 진입 계기를 먼저 본다.

- `기념일 전후`: 30일, 100일, 1주년 직전
- `롱디/바쁜 커플`: 같이 못 있는 날도 기록 가치가 있는 사용자
- `기록 성향이 더 강한 한 명`: 한 사람이 먼저 시작하고 상대를 초대하는 구조

유입 메시지 가설:
- `우리의 평범한 하루를 책으로 남긴다`
- `같이 있는 날도, 못 만난 날도 기록이 된다`
- `30일이 쌓이면 진짜 책이 된다`

## 14. 디자인 리뷰 보강

### 14.1 초기 평가

- 초기 문서의 디자인 완성도는 `6.5/10`이었다.
- 이번 재리뷰 기준 현재 점수는 `8.4/10`이다.
- 강점은 핵심 화면과 상태를 이미 알고 있다는 점이다.
- 가장 큰 보강점은 `오늘` 화면을 대시보드나 북 판매 hero가 아니라, `둘만 보는 private social feed`로 다시 정의한 것이다.
- 남은 약점은 `archive`, `record`, `book` 화면이 아직 같은 디자인 언어로 완전히 잠기지 않았다는 점이다.

10/10 기준:
- 홈, 아카이브, 북, 주문, 운영자 화면에서 사용자가 보는 정보 우선순위가 명확해야 한다.
- 각 기능의 loading/empty/error/success/partial 상태가 사용자 관점으로 정의되어 있어야 한다.
- 모바일과 접근성 규칙이 구현 전에 잠겨 있어야 한다.
- 홈의 첫 화면이 `공개 SNS`, `SaaS dashboard`, `포토북 커머스 hero`로 읽히지 않고, `사적인 커플 피드`로 단번에 읽혀야 한다.

### 14.2 DESIGN.md 상태

- 이제 `DESIGN.md`는 선택이 아니라 선행 산출물이다.
- 디자인 시스템의 소스 오브 트루스는 `frontend/book-api-work-frontend/DESIGN.md`에 둔다.
- 실행 청사진과 프론트 구현은 이 문서를 기준으로 맞춘다.
- 공통 원칙이 없는 상태에서 계속 화면을 추가하는 것은 금지한다.
- 현재 baseline `DESIGN.md`가 작성되었고, 이후 public/admin 화면은 이 문서를 먼저 갱신한 뒤 구현한다.
- `DESIGN.md`가 다루지 않는 항목은 아래 universal app UI 원칙으로만 임시 판단한다.
  - 차분한 표면
  - 강한 타이포 계층
  - 적은 색
  - 카드 남발 금지
  - 홈 첫 화면은 `대형 hero`보다 `private feed rhythm` 중심

### 14.2.2 디자인 시스템 선행 이유

- 이 서비스는 tone mismatch가 바로 제품 mismatch로 느껴진다.
- 잘못된 API보다 잘못된 화면 톤이 더 빨리 신뢰를 깬다.
- 따라서 디자인 시스템은 polish 단계가 아니라 구현 선행 조건이다.

구체적으로 먼저 잠가야 하는 것:
- type scale
- color tokens
- surface / border / radius 규칙
- public home/feed shell
- admin ops shell
- empty/error/loading 언어 톤

### 14.2.1 앱 톤 재고정

- 앱은 `공개 SNS`가 아니다.
- 하지만 홈의 읽힘은 `사적인 SNS`에 더 가깝게 가져간다.
- 즉, 제품 정체성은 `둘만의 기록장`, 레이아웃 문법은 `가볍게 스크롤되는 private feed`를 차용한다.

이 결정이 의미하는 것:
- 홈은 KPI나 페이지 수를 먼저 보여주는 대시보드가 아니다.
- 홈은 북 주문을 밀어붙이는 커머스 랜딩도 아니다.
- 홈은 `기록 남기기`, `오늘 카드`, `최근 며칠의 흐름`이 자연스럽게 이어지는 social rhythm을 가져야 한다.

홈이 차용할 것:
- compact app bar
- thumb zone 안의 composer / 기록 CTA
- 세로 스크롤 중심의 feed 구조
- 최근 활동이 이어지는 카드 리듬
- 부드러운 배지와 가벼운 상태 표기

홈이 버릴 것:
- 상단 대형 hero
- 정사각 대시보드 통계 박스
- 북 상품 판매 배너
- 공개 engagement metric
- 3열 feature grid

### 14.3 정보 구조

```text
APP
├─ Onboarding
│  ├─ 가치 설명
│  ├─ 초대 코드 연결
│  └─ 첫 기록 남기기
├─ Home
│  ├─ compact app bar
│  ├─ composer strip / 기록 CTA
│  ├─ pinned today card
│  │  ├─ 오늘 카드 상태
│  │  ├─ 내 기록
│  │  └─ 상대 기록
│  ├─ recent archive preview feed
│  └─ quiet book progress module
├─ Archive
│  ├─ 날짜순 카드 피드
│  └─ 월별 묶음
├─ Book
│  ├─ 주문 가능 여부
│  ├─ 현재 snapshot
│  ├─ 미리보기
│  └─ 주문 진입
├─ Me
│  ├─ 우리 설정
│  └─ 알림/프라이버시

ADMIN CONSOLE (`bookservice-admin.earlydreamer.dev`)
├─ ops overview
├─ credits 상태
├─ build 실패 목록
├─ 주문 실패 목록
└─ 재시도/환불 액션
```

화면 우선순위:
- 홈은 `오늘의 피드 흐름`
- 북은 `주문 가능성과 진행 상태`
- 운영은 `실패와 보상 대상`

### 14.3.1 Home first-fold hierarchy

홈 첫 화면은 아래 순서로 읽혀야 한다.

1. `지금 기록할 수 있나`
2. `오늘 카드가 어떤 상태인가`
3. `내 기록과 상대 기록이 어떻게 놓이는가`
4. `최근 며칠이 어떻게 쌓이고 있나`
5. `30일 북이 어디까지 왔나`

이 순서가 중요한 이유:
- 사용자는 홈에서 보고 싶은 정보보다 `지금 해야 할 행동`을 먼저 찾는다.
- 그 다음은 관계의 현재 상태다.
- 북은 동기 부여 요소지만, 홈의 주인공이 되면 다시 커머스처럼 보인다.

### 14.3.2 Home layout rule

- 홈의 기본 레이아웃은 `세로 피드`다.
- 상단은 작은 app bar와 한 줄 설명까지만 허용한다.
- 첫 번째 큰 surface는 `오늘 카드`여야 한다.
- `기록된 날`, `준비된 페이지` 같은 수치는 보조 배지나 작은 metric block으로만 표현한다.
- `최근 아카이브`는 홈 하단에서 2~3개 preview만 노출하고, 자세한 탐색은 Archive 탭으로 넘긴다.
- `30일 북`은 home 하단의 조용한 모듈로 두고, 상단 hero로 올리지 않는다.

### 14.4 Interaction State Coverage

| Feature | Loading | Empty | Error | Success | Partial |
|---|---|---|---|---|---|
| Home | app bar + composer + today card skeleton | 아직 기록 없음, 첫 기록 유도 CTA | 오늘 카드 로드 실패, 재시도 CTA | 오늘 카드 + recent feed preview 노출 | 상대만 기록했거나 나만 기록한 feed 상태 |
| Archive | 월 단위 skeleton | 아직 기록이 없음 | 피드 조회 실패 | 카드 피드 노출 | 부분 카드가 섞여 노출 |
| Book | snapshot 계산 중 표시 | 아직 주문 조건 미충족 | 빌드 실패, 재시도 또는 문의 | READY_TO_ORDER + 미리보기 | ELIGIBLE지만 build 전 상태 |
| Order | 배송지/결제 로딩, pending 확인 화면 | 저장된 배송지 없음 | 결제 실패, confirm 실패, 주문 실패, 보상 안내 | 주문 접수 완료 | 결제 성공, 주문 실패로 환불 대기 |
| Admin | KPI/cards skeleton | 실패 항목 없음 | 운영 데이터 조회 실패 | 문제 항목과 조치 가능 | 일부 상태만 동기화된 경우 |

### 14.4.1 Home state 표현 규칙

- loading이어도 `앱 상단`, `기록 CTA`, `today card 자리`는 사라지지 않는다.
- empty는 차갑게 비워두지 않고, `첫 카드 남기기`를 바로 유도한다.
- partial은 실패가 아니라 `이미 의미 있는 하루`로 보이게 문구를 설계한다.
- success에서도 최근 아카이브 preview를 2~3개만 보여주고 홈을 무겁게 만들지 않는다.
- error 상태는 feed 전체를 날리지 않고, 실패한 모듈 단위로만 보여준다.

### 14.5 User Journey & Emotional Arc

| Step | User does | User feels | Plan support |
|---|---|---|---|
| 1 | 초대 코드로 연결 | 둘만의 공간이 열림 | 온보딩 + 첫 기록 |
| 2 | 오늘 기록을 남김 | 부담 없이 체크인함 | 20~30초 입력 |
| 3 | 상대 반응/부분 카드 확인 | 연결감 | 홈 상태 문구 |
| 4 | 기록이 쌓이는 걸 봄 | 관계가 아카이브가 됨 | 아카이브 + 북 진행 상태 |
| 5 | 주문 조건 도달 | "진짜 책이 되네" | snapshot + 미리보기 |
| 6 | 주문 완료 | 물성이 생김 | 주문 상태 조회 |

핵심 감정 설계:
- 앱은 숙제가 아니라 조용한 체크인
- 홈은 조용하지만 정적인 일기장이 아니라, 가볍게 움직이는 private feed
- 북 화면은 구매 압박이 아니라 축적의 시각화
- 주문은 이벤트가 아니라 기록의 자연스러운 다음 단계

### 14.5.1 AI slop / layout drift 방지 규칙

- 홈에서 `3열 feature grid` 금지
- 홈 상단의 대형 marketing hero 금지
- 홈의 첫 화면을 `통계 대시보드`처럼 읽히게 하는 균일 metric 박스 금지
- 북 관련 copy가 홈의 메인 headline을 먹는 구조 금지
- 공개 SNS처럼 `좋아요 수`, `팔로워 수`, `공개 반응 수` 같은 engagement metric 금지
- 반대로 너무 일기장처럼 고요해서 `다음 액션`이 사라지는 것도 금지

핵심 균형:
- `public social network`는 아니지만
- `private social feed`처럼 읽혀야 한다.

이 한 줄이 홈 레이아웃 판단 기준이다.

### 14.6 Responsive & Accessibility

- 모바일 우선으로 설계한다.
- 홈 메인 영역은 한 손 조작 범위 안에 `오늘 남기기` CTA가 있어야 한다.
- 아카이브 카드와 운영 테이블은 모바일에서 세로 스택으로 재배치한다.
- 모든 탭/CTA는 최소 `44px` 터치 타겟을 보장한다.
- 감정 선택은 색만으로 구분하지 않고 텍스트/아이콘을 같이 사용한다.
- 운영 화면도 키보드 탐색 가능해야 한다.
- 주문 상태, 실패, 환불 대기 상태는 색만이 아니라 라벨 텍스트로도 전달한다.

### 14.7 Unresolved Design Decisions

| Decision needed | Recommendation | If deferred |
|---|---|---|
| admin console 구현 형태 | 같은 백엔드 + 별도 admin 서브도메인 유지 | same-host hidden route로 되돌리면 권한 표면과 CORS가 다시 섞인다 |
| 주문 전 미리보기 위치 | Book 탭의 상단 핵심 모듈 | 홈과 북에서 정보가 중복되고 산만해질 수 있다 |
| 홈의 recent feed preview 깊이 | 최근 2~3개 카드까지만 미리 보여주고 Archive로 넘긴다 | 홈이 다시 무거운 피드가 되거나, 반대로 아카이브 가치가 안 보일 수 있다 |
| 홈의 social affordance 수준 | 감정/상태/순서감은 가져오되 댓글형 상호작용은 넣지 않는다 | 구현이 댓글/메신저 방향으로 새기 시작한다 |

### 14.8 Toss Payments 결제위젯 UX 결정

추천은 `Toss Payments 결제위젯 v2`를 주문서 페이지 안에 직접 렌더링하는 방식이다.

이유:
- 결제수단 선택 UI와 약관 UI를 주문서 안에 넣을 수 있어 사용 흐름이 자연스럽다.
- 카드 인증, 3DS, 실패 메시지 등 민감한 결제 경험은 Toss가 맡는다.
- 우리 서버는 `amount/orderId/paymentType 검증 -> confirm -> 주문 생성` 경계에 집중할 수 있다.
- 웹훅은 정합성 보조로 두고, 정상 카드 결제는 `successUrl -> confirm` 경로로 더 단순하게 처리할 수 있다.

기본 흐름:

```text
Book 탭
  -> 주문 요약 확인
  -> 배송지 입력
  -> Toss 결제 UI + 약관 UI 렌더링
  -> [결제하기]
  -> requestPayment()
  -> Toss 결제창/인증/3DS
  -> successUrl (/orders/:id/pending?paymentKey=...&orderId=...&amount=...&paymentType=...)
  -> pending 화면이 Spring confirm endpoint 호출
  -> Spring이 payment_status 갱신
  -> Sweetbook 주문 생성
  -> /orders/:id 에 최종 상태 반영
```

사용자 관점 상태:
- 결제 전: 배송지 입력 + 최종 금액 + 주문 대상 snapshot 확인 + Toss 결제 UI 노출
- 결제 중: `결제 정보를 입력하는 중`
- 결제 후 대기: `결제를 확인하는 중`
- 결제 확인 실패: `결제 정보를 확인하지 못했어요` + 재시도 CTA
- 결제 실패: `결제가 완료되지 않았어요`
- 주문 성공: `주문이 접수되었어요`
- 결제 성공 / 주문 실패: `주문 확인이 지연되고 있어요` + 문의/안내 문구

이 UX에서 중요한 점:
- 사용자는 `successUrl 복귀 = 결제 완료`로 느끼기 쉽다.
- 실제론 `successUrl 복귀 -> amount/orderId/paymentType 검증 -> Toss confirm -> Sweetbook order create` 순서가 맞다.
- 그래서 return URL 첫 화면은 성공 단정이 아니라 `확인 중` 상태가 맞다.
- MVP 1차는 `NORMAL` 일회성 결제 기준으로 설계하고, 비동기 가상계좌는 추후 확장으로 미룬다.

### 14.9 What already exists

- 현재 public mock에는 이미 `cool blue-gray palette`, `bottom nav`, `pair entry card`, `archive feed card` 패턴이 있다.
- 이 자산은 버리지 않고, 더 `SNS-like vertical rhythm`으로 재배치하는 쪽이 맞다.
- 즉, 완전한 재디자인보다 `home 구조와 hierarchy`를 먼저 바로잡는 것이 우선이다.

### 14.10 NOT in scope

- 공개 피드
- 팔로워/좋아요/랭킹
- 스토리 원형 carousel
- 댓글 스레드
- DM/채팅
- 과한 motion-heavy marketing hero
- 북 상품 판매 배너를 홈 상단 hero로 올리는 구조

## 15. 엔지니어링 리뷰 보강

### 15.1 Architecture ASCII Diagram

```text
Browser
  │
  ├─ public React app
  │    │
  │    ├─ GET / home/archive/book views
  │    └─ fetch -> Spring API
  │
  ├─ admin React app
  │    │
  │    ├─ GET / ops views
  │    └─ fetch -> Spring API
  │
  └─ direct PUT upload -> R2 (signed URL)

Spring API
  │
  ├─ public SecurityFilterChain
  ├─ admin SecurityFilterChain
  ├─ Supabase JWT verify
  ├─ Supabase Postgres
  ├─ create snapshot
  ├─ enqueue job
  ├─ receive Sweetbook webhook
  ├─ confirm Toss payment
  └─ receive Toss webhook

Background jobs
  │
  ├─ create Sweetbook book
  ├─ upload photos
  ├─ cover/contents/finalization
  ├─ credits check
  └─ create order / compensation
```

구조 판단:
- `Spring Boot 단일 백엔드 + DB 기반 잡 워커`가 현재 팀 속도에 가장 잘 맞는다.
- 이유는 익숙한 런타임, 디버깅 단순성, 온프레미스 배포 적합성을 한 번에 확보할 수 있기 때문이다.
- 운영자 console은 path가 아니라 별도 admin 서브도메인으로 분리한다.
- 단, `job_queue`와 refund/confirm 경계를 문서 수준에서 먼저 잠가야 "단일 앱"이 단순한 시스템으로 남는다.

### 15.2 현실적 생산 장애 시나리오

- R2 업로드는 성공했지만 metadata 저장이 실패함
- snapshot은 생성됐지만 build job enqueue가 실패함
- worker가 job을 잡은 뒤 죽어서 `RUNNING` 상태가 고착됨
- Sweetbook photos 업로드는 일부만 성공함
- successUrl로 복귀했지만 Toss confirm이 실패함
- finalization은 실패했는데 사용자 결제는 성공함
- Sweetbook webhook은 왔지만 우리 저장이 실패함
- Toss webhook 재전송으로 중복 처리 위험이 생김
- 환불 API 호출은 실패했는데 주문은 이미 실패 상태로 확정됨

결론:
- `order_events.dedupe_key unique`
- `payment_reference unique`
- `build_job_id`
- `order_job_id`
- `job_queue.dedupe_key unique`
- `job_queue.locked_at watchdog`
이 여섯 가지는 필수다.

### 15.3 Failure Modes Registry

| Codepath | Failure mode | Rescued? | Test? | User sees? | Logged? |
|---|---|---|---|---|---|
| upload intent -> R2 signed URL | 만료된 세션으로 URL 발급 요청 | Y | 필요 | 업로드 불가 메시지 | Y |
| client -> R2 PUT | 대용량/네트워크 실패 | Y | 필요 | 재업로드 CTA | Y |
| snapshot creation | 30일 계산 오류, 중복 snapshot | Y | 필요 | 주문 후보 생성 실패 메시지 | Y |
| Sweetbook finalization | 템플릿 payload 오류 | Y | 필요 | 빌드 실패 상태 | Y |
| background job runner | 프로세스 종료 후 job 고착 | Y | 필요 | 사용자 무노출, 운영 노출 | Y |
| payment success -> order create | 주문 생성 실패 | Y | 필요 | 환불 대기/문의 안내 | Y |
| refund request | 환불 API 실패 | Y | 필요 | 운영 개입 안내 | Y |
| Sweetbook webhook intake | verification 불일치, delivery 중복 | Y | 필요 | 사용자 무노출 | Y |

critical gap 방지 원칙:
- 침묵 실패 금지
- 사용자 무노출이 허용되는 건 내부 재시도 가능 운영 경로뿐

### 15.4 테스트 다이어그램

```text
NEW UX FLOWS
  1. 초대 코드 연결
  2. 오늘 카드 기록 작성
  3. 부분 카드 -> 완성 카드 전이
  4. 30일 북 후보 확인
  5. 주문 생성
  6. 주문 상태 조회
  7. admin 별도 로그인
  8. 운영자 실패 주문 조회/재시도

NEW DATA FLOWS
  1. client -> Spring API -> Supabase
  2. client -> Spring signed URL -> R2
  3. Spring API -> DB job queue -> Sweetbook
  4. Toss success/fail redirect -> React -> Spring confirm
  5. Toss webhook -> Spring API
  6. Sweetbook webhook -> Spring API -> orders/order_events
  7. admin host -> Spring admin filter chain
  8. order failure -> refund job -> payment provider

NEW ERROR/RESCUE PATHS
  1. snapshot ineligible
  2. build failed
  3. Toss confirm failed after success redirect
  4. order failed after payment success
  5. webhook duplicate
  6. webhook verification invalid
  7. public token exists but admin host session 없음
  8. job stuck / watchdog recovery
  9. refund failed / manual ops escalation
```

테스트 원칙:
- snapshot eligibility / page planner는 순수 로직 unit test로 100%에 가깝게 커버
- 주문 파이프라인은 integration test
- `successUrl 복귀 -> confirm 실패`와 `결제 성공 -> 주문 실패 -> refund_required`는 회귀 테스트로 고정
- duplicate confirm, duplicate webhook, stuck job recovery도 회귀 테스트로 고정
- 초대 연결, 기록 작성, 주문 완료는 E2E 후보
- admin 로그인, host 분리, 운영자 화면은 smoke 수준이 아니라 실패 케이스 재시도까지 검증

### 15.5 배포/롤아웃 규칙

- sandbox Sweetbook 키와 live 키를 분리한다.
- 주문 생성 기능은 feature flag 또는 환경 분리로 잠근다.
- preview 환경에서는 실제 주문 대신 sandbox book build까지만 허용한다.
- production 첫 롤아웃은 운영자에게만 주문을 열고, 내부 테스트 주문을 먼저 돌린다.

### 15.6 운영 대시보드 기본 패널

- credits 잔액
- READY_TO_ORDER snapshot 수
- build 실패 수
- stuck job 수
- `payment_succeeded + order_failed` 수
- `payment_refund_failed` 수
- webhook verification 실패 수
- 평균 build 소요 시간
- 주문 상태별 개수
- 최근 admin action audit log

### 15.7 What Already Exists

- 앱 코드 없음
- 다만 이미 충분히 재사용 가능한 것은 `상태 머신`, `page planner 규칙`, `API 매핑 표`, `운영 상태 정의`
- 구현은 이 규칙을 깨지 않는 방향으로만 진행하면 된다.

### 15.8 Cross-Phase Themes

- CEO, Design, Eng를 통틀어 가장 반복된 리스크는 `API보다 먼저 디자인 기준이 흔들린다`는 점이다.
- 즉, 지금 병목은 기술 부재보다 `화면을 어떤 언어로 만들 것인지`의 부재다.
- 그래서 세 리뷰를 종합한 결론도 같다.

`design system and layout first`

## 16. Decision Audit Trail

| # | Phase | Decision | Classification | Principle | Rationale | Rejected |
|---|---|---|---|---|---|---|
| 1 | CEO | 실물 주문 가능한 MVP 유지 | Mechanical | P1 | 차별점 검증을 뒤로 미루면 제품 정체성이 약해진다 | digital-only first |
| 2 | CEO | 첫 SKU는 A4 24p deterministic 유지 | Mechanical | P3 | 현재 페이지 규칙과 주문 조건이 가장 안정적으로 맞는다 | A5 50p, Square HC |
| 3 | Design | 주문 전 미리보기는 1차에 포함 | Mechanical | P1 | 20일 도달 전에도 제품 가치가 보여야 한다 | 주문 가능 이후에만 노출 |
| 4 | Design | admin은 `bookservice-admin.earlydreamer.dev` 별도 서브도메인으로 분리 | Mechanical | P1 | 운영 권한 표면을 public과 섞지 않고 host/CORS/security chain을 분리해야 한다 | same-host hidden route |
| 5 | Eng | React 프론트와 Spring 백엔드를 분리 | Mechanical | P5 | 구현 속도와 운영 경계를 동시에 확보하려면 익숙한 Spring 중심 구성이 낫다 | Edge/serverless 중심 단일 서비스 |
| 6 | Eng | Sweetbook 연동은 내부 client 하나로 통일 | Mechanical | P5 | idempotency, retry, logging을 직접 통제해야 한다 | SDK 직접 의존 |
| 7 | Eng | Toss redirect/confirm/webhook 경계를 별도 명시 | Mechanical | P1 | 결제 원자성은 provider 성공 화면만으로 완성되지 않는다 | 동기 결제 성공 가정 |
| 8 | Eng | 운영 패널을 MVP 범위에 포함 | Mechanical | P2 | 주문/환불/실패 재시도는 blast radius 안의 필수 기능이다 | 운영 UI 후순위 |
| 9 | Eng | 백엔드 런타임은 `Spring Boot 4.0.3 + Java 21`로 고정 | Mechanical | P5 | OSS-only 신규 프로젝트에서 `3.5.x`는 지원 runway가 너무 짧고, 팀은 Spring 생태계에 더 빠르다 | Boot 3.5.x, edge/serverless 재채택 |
| 10 | Eng | 권한 source of truth는 Spring API에 두고 admin은 allowlist 2차 검증 | Mechanical | P1 | admin console과 주문/환불 액션은 claim 하나만 믿으면 위험하다 | 클라이언트 가드 중심, RLS 단독 의존 |
| 11 | Eng | 장기 작업은 `job_queue + scheduler + watchdog`으로 운영 | Mechanical | P2 | 단일 Spring 앱을 유지하면서도 빌드/주문/환불 재시도와 stuck job 복구를 제어해야 한다 | 인메모리 async, 별도 외부 workflow 엔진 |
| 12 | Design | Toss 복귀 첫 화면은 성공 단정이 아닌 `pending 확인 화면`으로 고정 | Mechanical | P1 | `successUrl`만으로는 결제 확정과 주문 확정을 말할 수 없다 | 복귀 즉시 주문 완료 화면 |
| 13 | Eng | 주문 실패 보상은 자동 refund 우선, 실패 시 ops escalation | Mechanical | P2 | `결제 성공 -> 주문 실패`를 사람 기억에 의존하면 CS와 정산 리스크가 커진다 | 수동 환불 only |
| 14 | Eng | 인증은 Spring Security 기반 stateless JWT Bearer로 고정 | Mechanical | P1 | 인증/인가 경계를 빠르게 안정화하려면 Resource Server + method security가 가장 단순하고 안전하다 | 세션 기반 auth, 프론트 상태 기반 authz |
| 15 | Eng | 백엔드는 layered architecture로 구현 | Mechanical | P5 | 팀이 가장 익숙한 구조로 빠르게 shipping하면서도 service 계층에 보안/도메인 규칙을 모을 수 있다 | hexagonal first, 과도한 모듈 분리 |
| 16 | Design | 프론트는 public/admin 별도 SPA로 유지 | Mechanical | P1 | 숨김 라우트 하나로 합치면 auth 경계와 운영 권한 표면이 다시 섞인다 | 단일 SPA + hidden admin routes |
| 17 | Eng | 프론트 auth는 `Supabase JS PKCE + host별 독립 세션`으로 고정 | Mechanical | P1 | 서로 다른 서브도메인에서 shared storage를 전제하면 auth가 취약하고 복잡해진다 | cross-subdomain 세션 공유 해킹 |
| 18 | Eng | public/admin API는 공통 typed client + TanStack Query로 조회 상태를 표준화 | Mechanical | P5 | 주문/운영 화면은 loading/error/retry 상태가 많아 명시적 server-state 계층이 필요하다 | ad-hoc fetch scattered calls |
| 19 | CEO | 프론트는 화면 구현보다 디자인 시스템을 먼저 잠근다 | Mechanical | P1 | 이 제품은 구현 속도보다 tone mismatch 비용이 더 크다 | screen-first mock iteration |
| 20 | Design | 홈의 레이아웃 기준은 `private social feed`로 고정한다 | Mechanical | P5 | 공개 SNS와 대시보드 사이에서 흔들리면 서비스 정체성이 흐려진다 | quiet diary-only, metric dashboard |
| 21 | Eng | frontend repo의 `DESIGN.md`를 구현 선행 gate로 둔다 | Mechanical | P2 | 문서화된 기준 없이는 이후 mock, contract, API 단계마다 UI drift가 반복된다 | implicit style decisions |

## 17. GSTACK REVIEW REPORT

| Review | Trigger | Why | Runs | Status | Findings |
|--------|---------|-----|------|--------|----------|
| CEO Review | `/autoplan` | Scope & strategy | 3 | narrowed | 1 |
| Design Review | `/autoplan` | UI/UX gaps | 3 | narrowed | 2 |
| Eng Review | `/autoplan` | Architecture & tests | 3 | narrowed | 4 |

**VERDICT:** READY WITH CONCERNS

concerns:
- print pipeline보다 먼저 무너질 수 있는 건 `20/30` 기록 도달률이므로, pre-order preview와 activation 지표 계측을 같이 구현해야 한다.
- Toss success/fail redirect, confirm, webhook, refund job을 함께 묶은 order pipeline E2E와 replay test가 구현 전 필수다.
- 운영자 admin console 권한 모델을 early MVP에서 소홀히 하면 내부 도구가 바로 취약점이 된다.
- SPA + JWT Bearer 조합은 브라우저 보안면에서 완벽하지 않으므로, admin 분리 로그인, 짧은 JWT 수명, 강한 CSP를 같이 가져가야 한다.

review notes:
- 이번 rerun은 `Spring Boot 4.0.3 + 온프레미스` 전환 이후, 프론트 정책 잠금 전 `public/admin 분리 + auth 경계`를 다시 검토한 3차 라운드다.
- 외부 Codex voice 세션은 repo trust/plugin auth 문제로 유효한 산출물을 만들지 못해, 이번 round는 문서 직접 검토와 로컬 패치 중심으로 마무리했다.
- 테스트 계획 아티팩트는 `/home/jakka4/.gstack/projects/book-api-work/jakka4-unknown-spring-onprem-test-plan-20260405-075238.md`에 기록했다.
