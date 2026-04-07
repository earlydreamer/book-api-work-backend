# Sweetbook Sandbox 도서 제작 파이프라인 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 환경변수 관리, R2 direct upload, Sweetbook sandbox 책 생성/최종화, 수동 주문 생성, webhook 상태 동기화까지 실제 동작 가능한 백엔드 경로를 만든다.

**Architecture:** 기존 `controller -> service -> repository -> entity` 구조는 유지하되, 업로드, snapshot, Sweetbook 외부 연동, 주문, webhook을 각각 별도 서비스로 분리한다. `TodayUsContractService`는 홈/아카이브/오늘 카드 조립 역할만 유지하고, 책 제작과 주문은 전담 서비스로 옮긴다. 결제는 구현하지 않고, 현재 수동 주문 API를 나중에 결제 confirm 트리거로 교체할 수 있게 `OrderService` 경계를 유지한다.

**Tech Stack:** Spring Boot 4.0.x, Spring Security OAuth2 Resource Server, JPA, Flyway, PostgreSQL, H2, Testcontainers, WebClient, Cloudflare R2, Sweetbook Sandbox API

---

## 파일 책임

- Create: `/.env.example`
- Modify: `/.gitignore`
- Modify: `/README.md`
- Modify: `/src/main/resources/application.yml`
- Modify: `/src/main/resources/application-local.yml`
- Modify: `/src/main/resources/application-prod.yml`
- Modify: `/src/test/resources/application-test.yml`
- Create: `/src/main/java/dev/earlydreamer/todayus/config/R2Properties.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/config/SweetbookProperties.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/config/WebClientConfig.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/entity/UploadedAssetEntity.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/entity/UploadStatus.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/entity/BookSnapshotEntity.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/entity/BookSnapshotItemEntity.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/entity/BookSnapshotStatus.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/entity/SweetbookBookEntity.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/entity/SweetbookBookStatus.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/entity/SweetbookUploadedAssetEntity.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/entity/OrderEntity.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/entity/OrderStatus.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/entity/OrderEventEntity.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/repository/*Snapshot*Repository.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/repository/UploadedAssetRepository.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/repository/SweetbookBookRepository.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/repository/SweetbookUploadedAssetRepository.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/repository/OrderRepository.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/repository/OrderEventRepository.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/integration/storage/R2Presigner.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/integration/sweetbook/SweetbookClient.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/integration/sweetbook/dto/*.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/service/UploadIntentService.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/service/UploadedAssetService.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/service/BookSnapshotService.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/service/SweetbookBookService.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/service/OrderService.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/service/SweetbookWebhookService.java`
- Modify: `/src/main/java/dev/earlydreamer/todayus/service/TodayUsContractService.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/controller/UploadController.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/controller/WebhookController.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/controller/OrderController.java`
- Modify: `/src/main/java/dev/earlydreamer/todayus/controller/BookSnapshotController.java`
- Modify: `/src/main/java/dev/earlydreamer/todayus/controller/DayCardController.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/dto/upload/*.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/dto/books/*.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/dto/orders/*.java`
- Create: `/src/main/java/dev/earlydreamer/todayus/dto/webhooks/*.java`
- Create: `/src/main/resources/db/migration/V2__create_book_pipeline_tables.sql`
- Modify: `/src/test/java/dev/earlydreamer/todayus/controller/ContractApiIntegrationTest.java`
- Create: `/src/test/java/dev/earlydreamer/todayus/controller/UploadControllerIntegrationTest.java`
- Create: `/src/test/java/dev/earlydreamer/todayus/controller/BookSnapshotControllerIntegrationTest.java`
- Create: `/src/test/java/dev/earlydreamer/todayus/controller/OrderControllerIntegrationTest.java`
- Create: `/src/test/java/dev/earlydreamer/todayus/controller/SweetbookWebhookControllerIntegrationTest.java`
- Create: `/src/test/java/dev/earlydreamer/todayus/service/BookSnapshotServiceTest.java`
- Create: `/src/test/java/dev/earlydreamer/todayus/service/OrderServiceTest.java`

---

### Task 1: `.env` 기반 설정과 외부 연동 프로퍼티 정리

**Files:**
- Create: `.env.example`
- Modify: `.gitignore`
- Modify: `README.md`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-local.yml`
- Modify: `src/main/resources/application-prod.yml`
- Modify: `src/test/resources/application-test.yml`
- Create: `src/main/java/dev/earlydreamer/todayus/config/R2Properties.java`
- Create: `src/main/java/dev/earlydreamer/todayus/config/SweetbookProperties.java`
- Create: `src/main/java/dev/earlydreamer/todayus/config/WebClientConfig.java`

- [ ] **Step 1: `.env` import와 env example 기준을 테스트로 먼저 고정**

```java
@SpringBootTest(properties = {
	"spring.config.import=optional:file:.env[.properties]",
	"TODAY_US_SWEETBOOK_BASE_URL=https://api-sandbox.sweetbook.com",
	"TODAY_US_SWEETBOOK_BOOK_SPEC_ID=PHOTOBOOK_A4_SC"
})
class ExternalPropertiesBindingTest {

	@Autowired
	private SweetbookProperties sweetbookProperties;

	@Test
	void bindsSweetbookAndR2PropertiesFromEnvironment() {
		assertThat(sweetbookProperties.baseUrl()).isEqualTo("https://api-sandbox.sweetbook.com");
	}
}
```

- [ ] **Step 2: `.gitignore`에 `.env`를 추가하고 `.env.example`를 만든다**

```text
.env
.env.local
!.env.example
```

```text
TODAY_US_DB_URL=jdbc:postgresql://localhost:5432/todayus
TODAY_US_DB_USERNAME=postgres
TODAY_US_DB_PASSWORD=change-me
TODAY_US_DB_DRIVER=org.postgresql.Driver
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=https://project.supabase.co/auth/v1/.well-known/jwks.json
TODAY_US_R2_ACCOUNT_ID=your-account-id
TODAY_US_R2_ACCESS_KEY_ID=your-r2-key
TODAY_US_R2_SECRET_ACCESS_KEY=your-r2-secret
TODAY_US_R2_BUCKET=today-us-assets
TODAY_US_R2_PUBLIC_BASE_URL=https://cdn.example.com
TODAY_US_R2_UPLOAD_PREFIX=uploads
TODAY_US_R2_PRESIGN_TTL_SECONDS=900
TODAY_US_SWEETBOOK_BASE_URL=https://api-sandbox.sweetbook.com
TODAY_US_SWEETBOOK_API_KEY=your-sandbox-api-key
TODAY_US_SWEETBOOK_BOOK_SPEC_ID=PHOTOBOOK_A4_SC
TODAY_US_SWEETBOOK_TEMPLATE_ID=tpl_replace_me
TODAY_US_SWEETBOOK_WEBHOOK_SECRET=replace-me
```

- [ ] **Step 3: 외부 연동 프로퍼티 record와 WebClient bean을 추가한다**

```java
@ConfigurationProperties(prefix = "today-us.r2")
public record R2Properties(
	String accountId,
	String accessKeyId,
	String secretAccessKey,
	String bucket,
	String publicBaseUrl,
	String uploadPrefix,
	int presignTtlSeconds
) {
}
```

```java
@ConfigurationProperties(prefix = "today-us.sweetbook")
public record SweetbookProperties(
	String baseUrl,
	String apiKey,
	String bookSpecId,
	String templateId,
	String webhookSecret
) {
}
```

- [ ] **Step 4: 설정 파일에 env placeholder와 `.env` import를 연결한다**

```yaml
spring:
  config:
    import: optional:file:.env[.properties]

today-us:
  sweetbook:
    base-url: ${TODAY_US_SWEETBOOK_BASE_URL:}
    api-key: ${TODAY_US_SWEETBOOK_API_KEY:}
```

- [ ] **Step 5: 검증**

Run: `./gradlew test --tests '*ExternalPropertiesBindingTest'`
Expected: `.env`/환경변수 기반 프로퍼티 바인딩 통과

---

### Task 2: R2 direct upload intent와 asset 등록 구현

**Files:**
- Create: `src/main/java/dev/earlydreamer/todayus/entity/UploadedAssetEntity.java`
- Create: `src/main/java/dev/earlydreamer/todayus/entity/UploadStatus.java`
- Create: `src/main/java/dev/earlydreamer/todayus/repository/UploadedAssetRepository.java`
- Create: `src/main/java/dev/earlydreamer/todayus/integration/storage/R2Presigner.java`
- Create: `src/main/java/dev/earlydreamer/todayus/service/UploadIntentService.java`
- Create: `src/main/java/dev/earlydreamer/todayus/service/UploadedAssetService.java`
- Create: `src/main/java/dev/earlydreamer/todayus/controller/UploadController.java`
- Create: `src/main/java/dev/earlydreamer/todayus/dto/upload/CreateUploadIntentRequest.java`
- Create: `src/main/java/dev/earlydreamer/todayus/dto/upload/CreateUploadIntentResponse.java`
- Create: `src/main/java/dev/earlydreamer/todayus/dto/upload/CompleteUploadRequest.java`
- Modify: `src/main/resources/db/migration/V2__create_book_pipeline_tables.sql`
- Create: `src/test/java/dev/earlydreamer/todayus/controller/UploadControllerIntegrationTest.java`

- [ ] **Step 1: upload intent 응답 shape를 테스트로 먼저 고정**

```java
mockMvc.perform(post("/api/v1/uploads/intents")
		.contentType(APPLICATION_JSON)
		.content("""
			{
			  "fileName": "photo.jpg",
			  "contentType": "image/jpeg",
			  "fileSize": 12345
			}
			"""))
	.andExpect(status().isOk())
	.andExpect(jsonPath("$.assetId").exists())
	.andExpect(jsonPath("$.uploadUrl").exists())
	.andExpect(jsonPath("$.publicUrl").exists());
```

- [ ] **Step 2: `uploaded_assets` 테이블과 엔티티를 추가한다**

```sql
create table uploaded_assets (
  id varchar(100) primary key,
  owner_user_id varchar(100) not null,
  couple_id varchar(100) null,
  r2_object_key varchar(255) not null unique,
  public_url varchar(2000) not null,
  content_type varchar(100) not null,
  file_size bigint not null,
  upload_status varchar(30) not null,
  created_at timestamp with time zone not null
);
```

- [ ] **Step 3: presign 발급기와 service를 최소 구현한다**

```java
public interface R2Presigner {
	PresignedUpload issue(String objectKey, String contentType);
}
```

```java
public record PresignedUpload(String uploadUrl, String publicUrl, Instant expiresAt) {
}
```

- [ ] **Step 4: 업로드 완료 등록 API를 구현한다**

```java
@PostMapping("/complete")
public ResponseEntity<Void> complete(@Valid @RequestBody CompleteUploadRequest request) {
	uploadedAssetService.markUploaded(request.assetId());
	return ResponseEntity.ok().build();
}
```

- [ ] **Step 5: 검증**

Run: `./gradlew test --tests '*UploadControllerIntegrationTest'`
Expected: intent 발급과 complete 등록 회귀 통과

---

### Task 3: snapshot 도메인과 현재 책 요약 확장

**Files:**
- Create: `src/main/java/dev/earlydreamer/todayus/entity/BookSnapshotEntity.java`
- Create: `src/main/java/dev/earlydreamer/todayus/entity/BookSnapshotItemEntity.java`
- Create: `src/main/java/dev/earlydreamer/todayus/entity/BookSnapshotStatus.java`
- Create: `src/main/java/dev/earlydreamer/todayus/repository/BookSnapshotRepository.java`
- Create: `src/main/java/dev/earlydreamer/todayus/repository/BookSnapshotItemRepository.java`
- Create: `src/main/java/dev/earlydreamer/todayus/service/BookSnapshotService.java`
- Modify: `src/main/java/dev/earlydreamer/todayus/service/TodayUsContractService.java`
- Modify: `src/main/java/dev/earlydreamer/todayus/controller/BookSnapshotController.java`
- Create: `src/test/java/dev/earlydreamer/todayus/service/BookSnapshotServiceTest.java`
- Create: `src/test/java/dev/earlydreamer/todayus/controller/BookSnapshotControllerIntegrationTest.java`

- [ ] **Step 1: snapshot build 전후 책 요약 상태를 테스트로 잠근다**

```java
mockMvc.perform(get("/api/v1/book-snapshots/current"))
	.andExpect(status().isOk())
	.andExpect(jsonPath("$.bookProgress.state").value("eligible"))
	.andExpect(jsonPath("$.snapshot").isEmpty());
```

```java
mockMvc.perform(post("/api/v1/book-snapshots/current/build"))
	.andExpect(status().isOk())
	.andExpect(jsonPath("$.snapshot.status").value("snapshot-building"));
```

- [ ] **Step 2: snapshot / snapshot_items 테이블과 엔티티를 추가한다**

```sql
create table book_snapshots (
  id bigint generated by default as identity primary key,
  couple_id varchar(100) not null,
  status varchar(30) not null,
  window_start_date date not null,
  window_end_date date not null,
  recorded_days int not null,
  selected_item_count int not null
);
```

- [ ] **Step 3: 순수 계산 로직을 `BookSnapshotService`로 뺀다**

```java
public BookSnapshotEntity createCurrentSnapshot(String userId) {
	// 현재 ACTIVE couple 조회
	// 최근 30일 window 계산
	// 20일 자격 검증
	// snapshot + item 생성
}
```

- [ ] **Step 4: `/book-snapshots/current` 응답을 실제 snapshot/order 상태로 채운다**

```java
return new CurrentBookSnapshotResponse(progress, candidateMoments, snapshotView, orderView);
```

- [ ] **Step 5: 검증**

Run: `./gradlew test --tests '*BookSnapshotServiceTest' --tests '*BookSnapshotControllerIntegrationTest'`
Expected: snapshot eligibility와 current summary 확장 회귀 통과

---

### Task 4: Sweetbook sandbox client와 책 빌드 orchestration 구현

**Files:**
- Create: `src/main/java/dev/earlydreamer/todayus/entity/SweetbookBookEntity.java`
- Create: `src/main/java/dev/earlydreamer/todayus/entity/SweetbookBookStatus.java`
- Create: `src/main/java/dev/earlydreamer/todayus/entity/SweetbookUploadedAssetEntity.java`
- Create: `src/main/java/dev/earlydreamer/todayus/repository/SweetbookBookRepository.java`
- Create: `src/main/java/dev/earlydreamer/todayus/repository/SweetbookUploadedAssetRepository.java`
- Create: `src/main/java/dev/earlydreamer/todayus/integration/sweetbook/SweetbookClient.java`
- Create: `src/main/java/dev/earlydreamer/todayus/integration/sweetbook/dto/*.java`
- Create: `src/main/java/dev/earlydreamer/todayus/service/SweetbookBookService.java`
- Modify: `src/main/java/dev/earlydreamer/todayus/controller/BookSnapshotController.java`
- Create: `src/test/java/dev/earlydreamer/todayus/service/SweetbookBookServiceTest.java`

- [ ] **Step 1: Sweetbook client 없이도 orchestration 상태 전이를 테스트로 먼저 고정**

```java
@Test
void buildSnapshotCreatesBookUploadsAssetsAndFinalizes() {
	// client mock
	// snapshot status BUILDING -> READY_TO_ORDER
}
```

- [ ] **Step 2: `sweetbook_books`, `sweetbook_uploaded_assets` 테이블을 추가한다**

```sql
create table sweetbook_books (
  id bigint generated by default as identity primary key,
  snapshot_id bigint not null unique,
  sweetbook_book_uid varchar(100) not null unique,
  book_spec_id varchar(100) not null,
  template_id varchar(100) not null,
  status varchar(30) not null
);
```

- [ ] **Step 3: official Sweetbook sandbox API 기준 최소 client를 구현한다**

```java
SweetbookCreateBookResponse createBook(String title, String bookSpecUid, String externalRef);
SweetbookUploadPhotoResponse uploadPhoto(String bookUid, Resource resource);
void createCover(...);
void createContents(...);
SweetbookFinalizeBookResponse finalizeBook(String bookUid);
```

- [ ] **Step 4: `POST /book-snapshots/current/build`를 실제 orchestration으로 연결한다**

```java
bookSnapshotService.createCurrentSnapshot(...);
sweetbookBookService.buildSnapshot(...);
```

- [ ] **Step 5: 검증**

Run: `./gradlew test --tests '*SweetbookBookServiceTest' --tests '*BookSnapshotControllerIntegrationTest'`
Expected: Sweetbook sandbox build 흐름 상태 전이 통과

---

### Task 5: 수동 주문 API와 Sweetbook webhook 구현

**Files:**
- Create: `src/main/java/dev/earlydreamer/todayus/entity/OrderEntity.java`
- Create: `src/main/java/dev/earlydreamer/todayus/entity/OrderStatus.java`
- Create: `src/main/java/dev/earlydreamer/todayus/entity/OrderEventEntity.java`
- Create: `src/main/java/dev/earlydreamer/todayus/repository/OrderRepository.java`
- Create: `src/main/java/dev/earlydreamer/todayus/repository/OrderEventRepository.java`
- Create: `src/main/java/dev/earlydreamer/todayus/service/OrderService.java`
- Create: `src/main/java/dev/earlydreamer/todayus/service/SweetbookWebhookService.java`
- Create: `src/main/java/dev/earlydreamer/todayus/controller/OrderController.java`
- Create: `src/main/java/dev/earlydreamer/todayus/controller/WebhookController.java`
- Create: `src/main/java/dev/earlydreamer/todayus/dto/orders/*.java`
- Create: `src/main/java/dev/earlydreamer/todayus/dto/webhooks/*.java`
- Create: `src/test/java/dev/earlydreamer/todayus/service/OrderServiceTest.java`
- Create: `src/test/java/dev/earlydreamer/todayus/controller/OrderControllerIntegrationTest.java`
- Create: `src/test/java/dev/earlydreamer/todayus/controller/SweetbookWebhookControllerIntegrationTest.java`

- [ ] **Step 1: 주문 생성과 webhook 상태 반영을 테스트로 먼저 고정**

```java
mockMvc.perform(post("/api/v1/orders")
		.contentType(APPLICATION_JSON)
		.content("""
			{
			  "snapshotId": 1,
			  "recipientName": "김지우",
			  "recipientPhone": "010-1234-5678",
			  "recipientAddress": "서울시 ..."
			}
			"""))
	.andExpect(status().isOk())
	.andExpect(jsonPath("$.status").value("submitted"));
```

```java
mockMvc.perform(post("/api/v1/webhooks/sweetbook")
		.header("X-Sweetbook-Signature", "...")
		.contentType(APPLICATION_JSON)
		.content("{...}"))
	.andExpect(status().isOk());
```

- [ ] **Step 2: `orders`, `order_events` 테이블을 추가한다**

```sql
create table orders (
  id bigint generated by default as identity primary key,
  snapshot_id bigint not null,
  ordering_user_id varchar(100) not null,
  status varchar(30) not null,
  sweetbook_order_uid varchar(100) null unique
);
```

- [ ] **Step 3: 수동 주문 생성 service를 구현한다**

```java
public OrderEntity createManualOrder(CreateOrderRequest request, String userId) {
	// snapshot READY_TO_ORDER 검증
	// 내부 order 생성
	// Sweetbook order 생성
	// 상태 저장
}
```

- [ ] **Step 4: webhook dedupe와 상태 반영을 구현한다**

```java
if (orderEventRepository.existsByDedupeKey(dedupeKey)) {
	return;
}
```

- [ ] **Step 5: 검증**

Run: `./gradlew test --tests '*OrderServiceTest' --tests '*OrderControllerIntegrationTest' --tests '*SweetbookWebhookControllerIntegrationTest'`
Expected: 주문 생성과 webhook 상태 반영 회귀 통과

---

### Task 6: 문서와 전체 회귀 정리

**Files:**
- Modify: `README.md`
- Modify: `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md`
- Modify: `docs/specs/2026-04-07-cross-env-handoff-v1.md`
- Modify: `docs/specs/2026-04-08-sweetbook-sandbox-book-pipeline-v1.md`

- [ ] **Step 1: `.env.example`와 실행 방법을 README에 반영한다**

```bash
cp .env.example .env
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

- [ ] **Step 2: API 계약 문서를 upload/snapshot/order/webhook 기준으로 갱신한다**

```md
- POST /api/v1/uploads/intents
- POST /api/v1/book-snapshots/current/build
- POST /api/v1/orders
- POST /api/v1/webhooks/sweetbook
```

- [ ] **Step 3: 전체 회귀를 돌린다**

Run: `./gradlew clean test`
Expected: 전체 테스트 통과

- [ ] **Step 4: build를 다시 확인한다**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add .
git commit -m "feat: Sweetbook sandbox 도서 제작 파이프라인 추가"
```
