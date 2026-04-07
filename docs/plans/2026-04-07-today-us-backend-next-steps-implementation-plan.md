# 오늘 우리 백엔드 다음 단계 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** JPA core slice 위에 실제 인증, 운영 DB, 책/주문 도메인, 업로드/외부 연동까지 순서대로 올려서 프론트와 실연동 가능한 백엔드로 만든다.

**Architecture:** 현재의 `controller -> service -> repository -> entity` 구조는 유지하되, 이제부터는 `local contract mode`와 `production integration mode`를 분리해서 키운다. 우선순위는 `인증 경계 -> 운영 Postgres 정렬 -> book/order 확장 -> 업로드/외부 연동 -> 운영 안정화` 순서로 간다.

**Tech Stack:** Spring Boot 4.0.x, Spring Security OAuth2 Resource Server, JPA, Flyway, PostgreSQL, H2, Testcontainers, Cloudflare R2, WebClient

---

## 현재 기준 파일 책임

- `src/main/java/dev/earlydreamer/todayus/config/SecurityConfig.java`
  현재 인증 on/off 토글과 resource server 진입점
- `src/main/java/dev/earlydreamer/todayus/service/CurrentUserProvider.java`
  현재 사용자 식별. 지금은 local header fallback과 JWT subject/name 해석이 같이 들어가 있음
- `src/main/java/dev/earlydreamer/todayus/service/TodayUsContractService.java`
  홈, 보관함, 연결, day card, book summary 조립이 한 클래스에 모여 있음
- `src/main/java/dev/earlydreamer/todayus/entity/*.java`
  현재 core 모델은 `users`, `couples`, `day_cards`, `card_entries`
- `src/main/resources/db/migration/V1__create_core_contract_tables.sql`
  core 스키마 baseline
- `src/test/java/dev/earlydreamer/todayus/controller/ContractApiIntegrationTest.java`
  현재 contract 회귀 테스트
- `src/test/resources/db/test/*.sql`
  H2 seed/reset 기준 데이터

## 권장 우선순위

1. Supabase JWT 검증 연결
2. 운영 Postgres profile 분리와 Postgres 기준 테스트 추가
3. `book_snapshots`, `orders` 도메인 모델과 write path 구현
4. R2 upload intent와 외부 제작/결제 연동 추가
5. 운영/테스트/문서 안정화

---

### Task 1: 인증 경계 실구현으로 전환

**Files:**
- Create: `src/main/java/dev/earlydreamer/todayus/config/SupabaseJwtProperties.java`
- Create: `src/main/java/dev/earlydreamer/todayus/config/SupabaseJwtAuthenticationConverter.java`
- Modify: `src/main/java/dev/earlydreamer/todayus/config/SecurityConfig.java`
- Modify: `src/main/java/dev/earlydreamer/todayus/config/SecurityProperties.java`
- Modify: `src/main/java/dev/earlydreamer/todayus/service/CurrentUserProvider.java`
- Modify: `src/main/resources/application.yml`
- Create: `src/test/java/dev/earlydreamer/todayus/security/SecurityIntegrationTest.java`

- [ ] **Step 1: 인증 실패/성공 기준을 테스트로 먼저 고정**

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

	@Test
	void authEnabledWithoutTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/me/home"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void authEnabledJwtUsesSubjectAsCurrentUserId() throws Exception {
		mockMvc.perform(get("/api/v1/me/home").with(jwt().jwt((jwt) -> jwt
			.subject("supabase-user-1")
			.claim("name", "지우"))))
			.andExpect(status().isOk());
	}
}
```

- [ ] **Step 2: Supabase JWT 설정 프로퍼티를 추가**

```java
@ConfigurationProperties(prefix = "today-us.security.supabase")
public record SupabaseJwtProperties(
	String issuerUri,
	String jwkSetUri,
	String audience
) {
}
```

- [ ] **Step 3: `SecurityConfig`에서 auth-enabled면 Supabase JWT 검증을 타게 연결**

```java
http.oauth2ResourceServer((oauth2) -> oauth2.jwt((jwt) -> jwt
	.jwtAuthenticationConverter(authenticationConverter)));
```

- [ ] **Step 4: `CurrentUserProvider`에서 local header fallback과 JWT 모드를 명확히 분리**

```java
if (!securityProperties.authEnabled()) {
	return resolveLocalUser();
}
return resolveJwtUser();
```

- [ ] **Step 5: 검증**

Run: `./gradlew test --tests dev.earlydreamer.todayus.security.SecurityIntegrationTest`
Expected: local mode와 auth-enabled mode의 현재 사용자 해석이 각각 통과

---

### Task 2: H2 개발 모드와 운영 Postgres 모드를 분리

**Files:**
- Create: `src/main/resources/application-local.yml`
- Create: `src/main/resources/application-prod.yml`
- Modify: `src/main/resources/application.yml`
- Create: `src/test/java/dev/earlydreamer/todayus/repository/PostgresSchemaIntegrationTest.java`
- Modify: `build.gradle.kts`
- Modify: `README.md`

- [ ] **Step 1: profile별 datasource 분리를 테스트로 고정**

```java
@Testcontainers
@DataJpaTest
class PostgresSchemaIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
}
```

- [ ] **Step 2: 공통 설정은 `application.yml`, 로컬은 `application-local.yml`, 운영은 `application-prod.yml`로 분리**

```yaml
spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: jdbc:h2:mem:todayus
```

- [ ] **Step 3: Postgres 컨테이너 기준으로 Flyway + JPA validate가 통과하는지 확인**

```java
@Autowired
private UserRepository userRepository;
```

- [ ] **Step 4: README에 실행 규칙을 분명히 적기**

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

- [ ] **Step 5: 검증**

Run: `./gradlew test --tests dev.earlydreamer.todayus.repository.PostgresSchemaIntegrationTest`
Expected: Postgres에서도 migration과 entity mapping이 동일하게 통과

---

### Task 3: 책 스냅샷과 주문 도메인 추가

**Files:**
- Create: `src/main/java/dev/earlydreamer/todayus/entity/BookSnapshotEntity.java`
- Create: `src/main/java/dev/earlydreamer/todayus/entity/BookSnapshotStatus.java`
- Create: `src/main/java/dev/earlydreamer/todayus/entity/OrderEntity.java`
- Create: `src/main/java/dev/earlydreamer/todayus/entity/OrderStatus.java`
- Create: `src/main/java/dev/earlydreamer/todayus/repository/BookSnapshotRepository.java`
- Create: `src/main/java/dev/earlydreamer/todayus/repository/OrderRepository.java`
- Create: `src/main/java/dev/earlydreamer/todayus/service/BookSnapshotService.java`
- Create: `src/main/java/dev/earlydreamer/todayus/service/OrderService.java`
- Modify: `src/main/java/dev/earlydreamer/todayus/service/TodayUsContractService.java`
- Create: `src/main/resources/db/migration/V2__create_book_snapshot_and_order_tables.sql`
- Modify: `src/test/java/dev/earlydreamer/todayus/controller/ContractApiIntegrationTest.java`

- [ ] **Step 1: `GET /api/v1/book-snapshots/current`의 null 필드를 유지한 채 엔티티 기반 조회 테스트를 먼저 추가**

```java
mockMvc.perform(get("/api/v1/book-snapshots/current"))
	.andExpect(status().isOk())
	.andExpect(jsonPath("$.bookProgress.state").value("growing"));
```

- [ ] **Step 2: `book_snapshots`, `orders` 테이블 migration 작성**

```sql
create table book_snapshots (
  id bigint generated by default as identity primary key,
  couple_id varchar(64) not null,
  status varchar(32) not null
);
```

- [ ] **Step 3: 스냅샷 생성 조건과 주문 readiness 계산을 `BookSnapshotService`로 분리**

```java
public CurrentBookSnapshotResponse getCurrentSnapshotSummary(String userId) {
	return bookSnapshotService.getCurrentSummary(userId);
}
```

- [ ] **Step 4: `TodayUsContractService`는 조립 역할만 남기고 book/order 로직을 전담 서비스로 위임**

```java
return new CurrentBookSnapshotResponse(progress, candidates, snapshot, order);
```

- [ ] **Step 5: 검증**

Run: `./gradlew test --tests dev.earlydreamer.todayus.controller.ContractApiIntegrationTest.currentBookSnapshotReturnsGrowthSummary`
Expected: 기존 계약은 유지하면서 snapshot/order 확장 여지를 확보

---

### Task 4: 업로드 intent와 외부 연동 surface 추가

**Files:**
- Create: `src/main/java/dev/earlydreamer/todayus/controller/UploadController.java`
- Create: `src/main/java/dev/earlydreamer/todayus/dto/upload/CreateUploadIntentRequest.java`
- Create: `src/main/java/dev/earlydreamer/todayus/dto/upload/CreateUploadIntentResponse.java`
- Create: `src/main/java/dev/earlydreamer/todayus/service/UploadIntentService.java`
- Create: `src/main/java/dev/earlydreamer/todayus/config/R2Properties.java`
- Create: `src/main/java/dev/earlydreamer/todayus/service/PrintPartnerClient.java`
- Create: `src/main/java/dev/earlydreamer/todayus/service/PaymentClient.java`
- Modify: `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md`

- [ ] **Step 1: upload intent 계약을 문서와 테스트로 먼저 잠그기**

```json
{
  "uploadUrl": "https://...",
  "objectKey": "day-cards/2026/04/07/photo-1.jpg"
}
```

- [ ] **Step 2: presigned URL 발급 서비스와 controller 추가**

```java
@PostMapping("/api/v1/uploads/intents")
public CreateUploadIntentResponse createIntent(@Valid @RequestBody CreateUploadIntentRequest request) {
	return uploadIntentService.createIntent(request);
}
```

- [ ] **Step 3: 외부 제작/결제 연동은 바로 실호출하지 말고 `WebClient` 래퍼를 분리**

```java
public interface PrintPartnerClient {
	void submitSnapshot(Long snapshotId);
}
```

- [ ] **Step 4: OpenAPI와 계약 문서를 같이 갱신**

```bash
./gradlew test --tests dev.earlydreamer.todayus.controller.ContractApiIntegrationTest.openApiDocsExposeEndpointSummariesAndSchemas
```

- [ ] **Step 5: 검증**

Run: `./gradlew test`
Expected: 새 endpoint가 들어와도 기존 contract와 Swagger 회귀가 같이 유지

---

### Task 5: 서비스 분리, 운영 안정화, 문서 정리

**Files:**
- Create: `src/main/java/dev/earlydreamer/todayus/service/HomeQueryService.java`
- Create: `src/main/java/dev/earlydreamer/todayus/service/ArchiveQueryService.java`
- Create: `src/main/java/dev/earlydreamer/todayus/service/CoupleCommandService.java`
- Create: `src/main/java/dev/earlydreamer/todayus/service/DayCardCommandService.java`
- Modify: `src/main/java/dev/earlydreamer/todayus/service/TodayUsContractService.java`
- Modify: `README.md`
- Modify: `docs/specs/2026-04-07-cross-env-handoff-v1.md`
- Modify: `docs/specs/2026-04-07-today-us-backend-api-contract-v1.md`

- [ ] **Step 1: 커지는 `TodayUsContractService`를 읽기/쓰기 기준으로 분리**

```java
public class HomeQueryService {
	public HomeResponse getHome(CurrentUserIdentity currentUser) { ... }
}
```

- [ ] **Step 2: controller는 유지하고, orchestration만 얇게 남기기**

```java
return homeQueryService.getHome(currentUserProvider.getCurrentUser());
```

- [ ] **Step 3: smoke test 외에 repo/service 단위 테스트를 보강**

```bash
./gradlew test
./gradlew build
```

- [ ] **Step 4: 로컬 실행 규칙, auth 모드, profile, seed 데이터 사용법을 문서화**

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
curl -H "X-Today-Us-Auth-User-Id: local-user-4" http://localhost:8080/api/v1/me/home
```

- [ ] **Step 5: 최종 검증**

Run: `./gradlew test && ./gradlew build`
Expected: contract, auth, Postgres, book/order, upload까지 확장한 뒤에도 전체 회귀가 유지

---

## 지금 바로 착수할 한 줄 우선순위

가장 먼저 할 일은 `Task 1 + Task 2`다. 이유는 지금 백엔드가 기능적으로는 움직이지만, 아직 `실제 사용자 식별`과 `운영 DB 기준 신뢰성`이 잠기지 않았기 때문이다.

## 완료 기준

- 프론트가 Supabase access token으로 backend를 직접 호출할 수 있다.
- local 개발자는 auth-disabled + H2 seed 모드로 빠르게 contract를 재현할 수 있다.
- 운영 환경은 Postgres + Flyway 기준으로 같은 스키마를 쓴다.
- `book_snapshots`, `orders`, upload intent까지 들어와도 기존 home/archive/day-card 계약이 깨지지 않는다.
