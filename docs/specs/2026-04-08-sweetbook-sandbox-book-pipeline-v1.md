# 오늘 우리 Sweetbook Sandbox 도서 제작 파이프라인 명세 v1

작성일: 2026-04-08
상태: Implemented in scaffold
대상: `backend/book-api-work-backend`
연결 문서:
- `README.md`
- `docs/specs/2026-04-05-today-our-mvp-v2.md`
- `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md`
- `docs/specs/2026-04-07-cross-env-handoff-v1.md`
- `docs/specs/2026-04-07-today-us-front-progress-v1.md`
- `docs/specs/2026-04-07-today-us-front-alignment-v1.md`
- `docs/plans/2026-04-05-today-our-mvp-execution-blueprint.md`

---

## 1. 이 문서의 목적

이 문서는 `오늘 우리` 백엔드에 `Sweetbook sandbox` 기준 도서 제작 파이프라인을 실제 구현하기 전에,
무엇을 어떤 경계로 만들지 잠그는 기준 문서다.

이번 라운드 목표는 세 가지다.

1. `.env`와 `.env.example` 중심으로 외부 연동 비밀값 관리 구조를 고정한다.
2. `Cloudflare R2 direct upload + Sweetbook sandbox book build + 수동 주문 생성`까지 실제 동작 가능한 백엔드 경로를 만든다.
3. 나중에 `Toss 결제 confirm`만 끼워 넣으면 주문 생성 트리거를 바꿀 수 있게 서비스 경계를 분리한다.

이번 라운드에서는 결제를 구현하지 않는다.
주문 생성은 내부 수동 주문 API로 먼저 열고, 결제 API가 들어오면 그 자리를 교체한다.

---

## 2. 이번 라운드에서 잠그는 결정

### 2.1 Sweetbook 연동 범위

- 모든 Sweetbook API 호출은 `sandbox` 기준으로 구현한다.
- 브라우저는 Sweetbook를 직접 호출하지 않는다.
- Sweetbook 책 규격(`book spec`)과 템플릿(`template`)은 `1종 고정`으로 간다.
- 고정값은 코드 상수가 아니라 환경변수로 관리한다.

### 2.2 업로드 경계

- 사용자가 올린 이미지는 `브라우저 -> R2 direct upload`로 간다.
- Spring 서버는 presigned upload intent와 업로드 완료 등록만 담당한다.
- `브라우저 -> Spring multipart -> 서버가 R2 재업로드` 방식은 쓰지 않는다.

이유:

- 사진 업로드 트래픽을 Spring 서버가 먹지 않게 한다.
- 서버는 도메인 로직, Sweetbook orchestration, 상태 전이에 집중한다.
- 이후 결제 연동이 붙어도 업로드 경계는 바뀌지 않는다.

### 2.3 주문 트리거

- 이번 라운드의 주문 생성은 `수동 주문 API`로 연다.
- 주문 생성 시 서버는 snapshot 상태를 검사한 뒤 Sweetbook sandbox 주문을 만든다.
- 나중에 결제 confirm이 구현되면 `OrderService` 호출 트리거만 바꾼다.

### 2.4 현재 관계 기준 원칙 유지

- 책 후보 계산, snapshot 생성, 주문 생성은 모두 `현재 ACTIVE 관계` 기준으로만 동작한다.
- archived 관계 기록은 snapshot/order 계산에 절대 섞지 않는다.
- reconnect 뒤의 새 관계는 새 `couple_id` 기준으로 별도 계산한다.

---

## 3. 환경변수 관리 원칙

### 3.1 파일 운영

- `.env`는 로컬 비밀값 전용 파일로 두고 git ignore 한다.
- `.env.example`는 필요한 키 목록과 예시 값만 관리한다.
- Spring은 `application.yml`, `application-local.yml`, `application-prod.yml`, `application-test.yml`에서 환경변수를 읽는다.
- 실제 키 값은 문서나 repo에 저장하지 않는다.

### 3.2 이번 라운드에 필요한 환경변수

```text
TODAY_US_DB_URL
TODAY_US_DB_USERNAME
TODAY_US_DB_PASSWORD
TODAY_US_DB_DRIVER

SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI
TODAY_US_SUPABASE_PROJECT_URL

TODAY_US_R2_ACCOUNT_ID
TODAY_US_R2_ACCESS_KEY_ID
TODAY_US_R2_SECRET_ACCESS_KEY
TODAY_US_R2_BUCKET
TODAY_US_R2_PUBLIC_BASE_URL
TODAY_US_R2_UPLOAD_PREFIX
TODAY_US_R2_PRESIGN_TTL_SECONDS

TODAY_US_SWEETBOOK_BASE_URL
TODAY_US_SWEETBOOK_API_KEY
TODAY_US_SWEETBOOK_BOOK_SPEC_ID
TODAY_US_SWEETBOOK_TEMPLATE_ID
TODAY_US_SWEETBOOK_WEBHOOK_SECRET
```

### 3.3 프로필 정책

- `local`
  - auth disabled
  - H2
  - R2 / Sweetbook는 실제 키가 없으면 비활성 또는 stub-safe 검증 모드
- `prod`
  - auth enabled
  - Postgres
  - R2 / Sweetbook sandbox 실연동 가능
- `test`
  - 기본은 H2
  - R2는 fake signer 또는 stub
  - Sweetbook outbound는 mock client
  - Postgres Testcontainers는 실제 schema/concurrency 검증 전용

---

## 4. 프론트 목업 기준 요구 정리

### 4.1 `/make-book` 화면이 기대하는 것

프론트 기준 `MakeBook`은 두 구간으로 나뉜다.

1. 성장 구간
- 최근 30일 기록 수
- 주문 가능까지 남은 일수
- 책 후보가 될 기록 미리보기

2. 주문 실행 구간
- 어떤 기록이 이번 책 후보에 들어가는지 선택
- 배송지 입력
- 주문 요청 완료

### 4.2 백엔드가 제공해야 할 상태

- `bookProgress`
  - `growing`, `eligible`, `snapshot-building`, `ready-to-order`, `ordered`
- `candidateMoments`
  - 최근 30일 내 기록 후보 목록
- `snapshot`
  - 생성된 주문 후보의 불변 버전
- `order`
  - 최신 주문 상태 요약

### 4.3 첫 SKU 고정

프론트/제품 문서 기준 첫 상품은 `A4 24p deterministic` 방향을 따른다.
이번 구현에서는 이를 Sweetbook의 특정 `bookSpecId`, `templateId` 1종으로 고정한다.

---

## 5. 데이터 모델

### 5.1 `uploaded_assets`

목적:
- R2에 직접 업로드된 사용자 이미지 메타데이터를 저장한다.

핵심 필드:
- `id`
- `owner_user_id`
- `couple_id`
- `r2_object_key`
- `public_url`
- `content_type`
- `file_size`
- `upload_status`
- `created_at`

주의:
- `day_card`와 1:1로 바로 묶지 않는다.
- 업로드 완료 후 day card 저장 시 asset을 연결한다.

### 5.2 `book_snapshots`

목적:
- 현재 30일 창의 주문 후보를 불변 버전으로 고정한다.

핵심 필드:
- `id`
- `couple_id`
- `status`
- `window_start_date`
- `window_end_date`
- `recorded_days`
- `selected_item_count`
- `sweetbook_book_id` nullable
- `build_started_at`
- `build_completed_at`
- `failure_code` nullable
- `failure_message` nullable
- `created_at`

규칙:
- snapshot 생성 이후 day card가 수정돼도 기존 snapshot은 바뀌지 않는다.
- 같은 관계에 대해 `READY_TO_ORDER` 또는 `ORDERED` snapshot이 이미 있으면 새 build를 막는다.

### 5.3 `book_snapshot_items`

목적:
- snapshot에 포함된 기록 단위 목록을 보관한다.

핵심 필드:
- `id`
- `snapshot_id`
- `day_card_id`
- `local_date`
- `my_entry_json`
- `partner_entry_json`
- `photo_asset_id` nullable
- `page_order`

### 5.4 `sweetbook_books`

목적:
- 내부 snapshot과 Sweetbook book uid를 연결한다.

핵심 필드:
- `id`
- `snapshot_id`
- `sweetbook_book_uid`
- `book_spec_id`
- `template_id`
- `status`
- `created_request_id`
- `finalized_at` nullable
- `failure_code` nullable
- `failure_message` nullable
- `created_at`
- `updated_at`

### 5.5 `sweetbook_uploaded_assets`

목적:
- 어떤 asset이 어떤 Sweetbook book으로 업로드됐는지 추적한다.

핵심 필드:
- `id`
- `sweetbook_book_id`
- `uploaded_asset_id`
- `sweetbook_file_name`
- `status`
- `uploaded_at` nullable
- `failure_code` nullable

### 5.6 `orders`

목적:
- 내부 주문 상태와 Sweetbook 주문 상태를 함께 관리한다.

핵심 필드:
- `id`
- `snapshot_id`
- `ordering_user_id`
- `recipient_name`
- `recipient_phone`
- `recipient_address`
- `status`
- `sweetbook_order_uid` nullable
- `sweetbook_order_status` nullable
- `requested_at`
- `submitted_at` nullable
- `completed_at` nullable
- `failure_code` nullable
- `failure_message` nullable
- `created_at`
- `updated_at`

### 5.7 `order_events`

목적:
- 주문 상태 전이와 webhook 이력을 남긴다.

핵심 필드:
- `id`
- `order_id`
- `event_type`
- `dedupe_key`
- `payload_json`
- `created_at`

---

## 6. 상태 모델

### 6.1 BookSnapshotStatus

- `BUILDING`
- `READY_TO_ORDER`
- `ORDERED`
- `FAILED`

### 6.2 SweetbookBookStatus

- `CREATED`
- `ASSETS_UPLOADED`
- `FINALIZED`
- `FAILED`

### 6.3 UploadStatus

- `PENDING`
- `UPLOADED`
- `ATTACHED`
- `FAILED`

### 6.4 OrderStatus

- `REQUESTED`
- `SUBMITTED`
- `CONFIRMED`
- `IN_PRODUCTION`
- `PRODUCTION_COMPLETE`
- `SHIPPED`
- `DELIVERED`
- `FAILED`
- `CANCELED`

---

## 7. 서비스 경계

### 7.1 `UploadIntentService`

책임:
- R2 presigned PUT URL 발급
- 업로드 세션 메타데이터 계산

하지 않을 일:
- day card 저장
- Sweetbook 호출

### 7.2 `UploadedAssetService`

책임:
- 업로드 완료 등록
- 사용자/관계 소유권 검증
- asset 조회

### 7.3 `BookSnapshotService`

책임:
- 현재 관계 기준 candidate 계산
- snapshot row / items 생성
- 기존 snapshot 중복 가능성 검증

### 7.4 `SweetbookClient`

책임:
- Sweetbook sandbox API HTTP 호출
- 요청/응답 매핑
- 에러를 내부 예외 타입으로 정규화

### 7.5 `SweetbookBookService`

책임:
- blank book 생성
- snapshot asset 업로드
- template/contents 적용
- finalization

하지 않을 일:
- 컨트롤러 응답 조립
- 배송지 검증

### 7.6 `OrderService`

책임:
- 수동 주문 생성
- snapshot 상태 검증
- Sweetbook 주문 생성
- 내부 주문 상태 조회

### 7.7 `SweetbookWebhookService`

책임:
- webhook 검증
- dedupe 처리
- 내부 주문 상태 반영

### 7.8 `TodayUsContractService`

책임:
- 기존 home/archive/day-card/book summary 조립만 유지

하지 않을 일:
- Sweetbook API 직접 호출
- snapshot/order orchestration

---

## 8. API surface

### 8.1 `POST /api/v1/uploads/intents`

목적:
- 브라우저가 R2에 직접 업로드할 수 있게 presigned intent를 발급한다.

요청:

```json
{
  "fileName": "photo.jpg",
  "contentType": "image/jpeg",
  "fileSize": 231231
}
```

응답:

```json
{
  "assetId": "asset_123",
  "objectKey": "uploads/local-user-1/2026/04/08/uuid-photo.jpg",
  "uploadUrl": "https://...",
  "publicUrl": "https://cdn.example.com/uploads/...",
  "expiresInSeconds": 900
}
```

### 8.2 `POST /api/v1/uploads/complete`

목적:
- 브라우저 업로드 완료 후 asset을 서버에 등록한다.

요청:

```json
{
  "assetId": "asset_123"
}
```

### 8.3 `PUT /api/v1/day-cards/{localDate}/entry`

확장:
- 기존 `photoUrl` 기반 저장은 유지 또는 점진 폐기한다.
- 이번 라운드에서는 `uploadedAssetId`를 우선 경로로 추가한다.

요청 예시:

```json
{
  "emotionCode": "calm",
  "memo": "오늘 같이 걸은 길이 좋았어요.",
  "uploadedAssetId": "asset_123"
}
```

### 8.4 `GET /api/v1/book-snapshots/current`

목적:
- 성장 구간과 주문 가능 구간을 같은 응답에서 렌더링한다.

현재 계약을 유지하되 아래를 실제 값으로 채운다.

- `candidateMoments`
- `snapshot`
- `order`

### 8.5 `POST /api/v1/book-snapshots/current/build`

목적:
- 현재 관계 기준 snapshot을 생성하고 Sweetbook sandbox 책 빌드를 시작한다.

동작:
- 30일/20일 자격 검증
- snapshot 생성
- blank book 생성
- asset 업로드
- template/finalization
- 성공 시 `READY_TO_ORDER`

### 8.6 `GET /api/v1/book-snapshots/{snapshotId}`

목적:
- 특정 snapshot 상세와 build 상태를 조회한다.

### 8.7 `POST /api/v1/orders`

목적:
- 결제 없이 수동 주문을 생성한다.

요청:

```json
{
  "snapshotId": 1001,
  "recipientName": "김지우",
  "recipientPhone": "010-1234-5678",
  "postalCode": "06101",
  "address1": "서울시 강남구 테헤란로 123",
  "address2": "4층 401호",
  "shippingMemo": "부재 시 경비실에 맡겨주세요."
}
```

동작:
- snapshot이 `READY_TO_ORDER`인지 검증
- 내부 order 생성
- Sweetbook sandbox 주문 생성

### 8.8 `GET /api/v1/orders/{orderId}`

목적:
- 최신 내부 주문 상태를 조회한다.

### 8.9 `POST /api/v1/webhooks/sweetbook`

목적:
- Sweetbook 상태 변경 webhook을 수신한다.

동작:
- webhook secret 검증
- `X-Webhook-Delivery` 기준 dedupe
- dedupe key 저장
- 대상 order / sweetbook book 상태 반영

---

## 9. 제작 플로우

### 9.1 업로드

1. 프론트가 `POST /uploads/intents`
2. 브라우저가 R2에 직접 PUT
3. 프론트가 `POST /uploads/complete`
4. day card 저장 시 `uploadedAssetId` 연결

### 9.2 snapshot 생성과 책 빌드

1. 프론트가 현재 candidate를 본다.
2. `POST /book-snapshots/current/build`
3. 서버가 snapshot/items를 고정한다.
4. 서버가 Sweetbook blank book을 만든다.
5. snapshot에 포함된 이미지 asset을 Sweetbook book에 업로드한다.
6. cover/contents/template를 적용하고 finalization 한다.
7. 성공 시 snapshot 상태를 `READY_TO_ORDER`로 바꾼다.

### 9.3 수동 주문

1. 프론트가 배송지를 입력한다.
2. `POST /orders`
3. 서버가 내부 주문을 `REQUESTED`로 생성한다.
4. 서버가 Sweetbook sandbox 주문을 생성한다.
5. 성공 시 `SUBMITTED` 또는 `CONFIRMED`로 올린다.

### 9.4 상태 동기화

1. Sweetbook webhook이 오면 `POST /webhooks/sweetbook`가 받는다.
2. 내부 주문 상태를 반영한다.
3. webhook 누락 가능성에 대비해 `GET /orders/{orderId}`나 운영 sync job으로 fallback 조회를 할 수 있게 한다.

---

## 10. 에러 처리 원칙

- Sweetbook 원본 에러 메시지를 그대로 사용자에게 노출하지 않는다.
- 내부 `code`, `title`, `detail`로 정규화한다.
- 재시도 가능 여부를 상태 필드와 failure code로 구분한다.

예:

- `upload_intent_failed`
- `snapshot_ineligible`
- `snapshot_build_failed`
- `sweetbook_book_create_failed`
- `sweetbook_asset_upload_failed`
- `sweetbook_finalization_failed`
- `order_submission_failed`
- `sweetbook_webhook_invalid`

---

## 11. 테스트 원칙

### 11.1 단위 테스트

- snapshot eligibility 계산
- snapshot item 생성 규칙
- order 상태 전이
- webhook dedupe 처리

### 11.2 통합 테스트

- R2 intent 발급 응답 shape
- snapshot build 성공/실패
- 수동 주문 생성
- webhook 상태 반영

### 11.3 Postgres 회귀 테스트

- 신규 사용자 첫 read 요청
- 동일 사용자 동시 invite 생성
- snapshot/order schema mapping

### 11.4 외부 연동 테스트 전략

- Sweetbook 실호출은 `sandbox` 환경에서 profile 또는 flag로 제한한다.
- 기본 테스트 스위트는 mock client 기반으로 빠르게 돈다.
- 별도 integration test에서만 sandbox outbound를 선택적으로 연다.

---

## 12. 이번 라운드 범위 밖

- Toss 결제 confirm
- 자동 환불/보상 플로우
- 운영 관리자 retry/refund UI
- 복수 SKU 지원
- 이미지 리사이즈/워터마크/고급 후처리
- 장기 job queue / scheduler / watchdog

---

## 13. 이번 문서의 결론

이번 라운드는 `환경변수 관리 -> R2 direct upload -> snapshot 불변화 -> Sweetbook sandbox book build -> 수동 주문 생성 -> webhook 상태 반영`까지를
하나의 얇고 분리된 백엔드 파이프라인으로 구현하는 것이 목표다.

핵심 원칙은 세 가지다.

1. 업로드, snapshot, 주문, 외부 API 호출의 책임을 서비스별로 분리한다.
2. 현재 관계 기준과 snapshot 불변 원칙을 끝까지 유지한다.
3. 결제는 나중에 붙이되, 지금 만드는 `OrderService` 경계는 그대로 재사용 가능해야 한다.
