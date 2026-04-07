package dev.earlydreamer.todayus.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.earlydreamer.todayus.integration.sweetbook.SweetbookClient;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateBookCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateBookResult;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateOrderCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateOrderResult;
import dev.earlydreamer.todayus.integration.sweetbook.dto.FinalizeBookResult;
import dev.earlydreamer.todayus.integration.sweetbook.dto.UploadPhotoCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.UploadPhotoResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SweetbookWebhookControllerIntegrationTest.FakeSweetbookConfig.class)
@Sql(scripts = "/db/test/reset.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/db/test/seed.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SweetbookWebhookControllerIntegrationTest {

	private static final String WEBHOOK_SECRET = "test-webhook-secret";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void webhookUpdatesOrderStatusAndDedupesByDeliveryId() throws Exception {
		Integer orderId = createReadyOrder();
		String payload = """
			{
			  "event": "shipping.departed",
			  "orderUid": "or_fake_123",
			  "bookUid": "bk_fake_123",
			  "status": "SHIPPED",
			  "trackingNumber": "1234567890123",
			  "trackingCarrier": "CJ",
			  "shippedAt": "2026-04-08T01:30:00Z",
			  "isTest": true,
			  "timestamp": "2026-04-08T01:30:00Z"
			}
			""";
		String timestamp = "1775583000";
		String signature = sign(timestamp, payload, WEBHOOK_SECRET);

		mockMvc.perform(post("/api/v1/webhooks/sweetbook")
				.contentType(APPLICATION_JSON)
				.content(payload)
				.header("X-Webhook-Timestamp", timestamp)
				.header("X-Webhook-Signature", signature)
				.header("X-Webhook-Event", "shipping.departed")
				.header("X-Webhook-Delivery", "wh_test_delivery_1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.received").value(true))
			.andExpect(jsonPath("$.duplicate").value(false));

		mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("shipped"))
			.andExpect(jsonPath("$.trackingCarrier").value("CJ"))
			.andExpect(jsonPath("$.trackingNumber").value("1234567890123"));

		mockMvc.perform(get("/api/v1/book-snapshots/current"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.order.status").value("shipped"));

		mockMvc.perform(post("/api/v1/webhooks/sweetbook")
				.contentType(APPLICATION_JSON)
				.content(payload)
				.header("X-Webhook-Timestamp", timestamp)
				.header("X-Webhook-Signature", signature)
				.header("X-Webhook-Event", "shipping.departed")
				.header("X-Webhook-Delivery", "wh_test_delivery_1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.received").value(true))
			.andExpect(jsonPath("$.duplicate").value(true));

		mockMvc.perform(post("/api/v1/webhooks/sweetbook")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "event": "order.created",
					  "orderUid": "or_fake_123",
					  "bookUid": "bk_fake_123",
					  "status": "PAID",
					  "timestamp": "2026-04-07T23:00:00Z",
					  "isTest": true
					}
					""")
				.header("X-Webhook-Timestamp", "1775577600")
				.header("X-Webhook-Signature", sign("1775577600", """
					{
					  "event": "order.created",
					  "orderUid": "or_fake_123",
					  "bookUid": "bk_fake_123",
					  "status": "PAID",
					  "timestamp": "2026-04-07T23:00:00Z",
					  "isTest": true
					}
					""", WEBHOOK_SECRET))
				.header("X-Webhook-Event", "order.created")
				.header("X-Webhook-Delivery", "wh_test_delivery_2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.duplicate").value(false));

		mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("shipped"));
	}

	@Test
	void webhookRejectsInvalidSignature() throws Exception {
		mockMvc.perform(post("/api/v1/webhooks/sweetbook")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "event": "order.created",
					  "orderUid": "or_fake_123",
					  "status": "PAID",
					  "timestamp": "2026-04-08T01:00:00Z",
					  "isTest": true
					}
					""")
				.header("X-Webhook-Timestamp", "1775581200")
				.header("X-Webhook-Signature", "sha256=invalid")
				.header("X-Webhook-Event", "order.created")
				.header("X-Webhook-Delivery", "wh_invalid"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("invalid_webhook_signature"));
	}

	@Test
	void webhookRejectsMissingDeliveryHeaders() throws Exception {
		mockMvc.perform(post("/api/v1/webhooks/sweetbook")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "event": "order.created",
					  "orderUid": "or_fake_123",
					  "status": "PAID",
					  "timestamp": "2026-04-08T01:00:00Z",
					  "isTest": true
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("invalid_webhook_headers"));
	}

	private Integer createReadyOrder() throws Exception {
		seedEligibilityDays();
		MvcResult buildResult = mockMvc.perform(post("/api/v1/book-snapshots/current/build"))
			.andExpect(status().isOk())
			.andReturn();
		Integer snapshotId = JsonPath.read(buildResult.getResponse().getContentAsString(), "$.snapshot.snapshotId");

		MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "snapshotId": %d,
					  "recipientName": "김지우",
					  "recipientPhone": "010-1234-5678",
					  "postalCode": "06101",
					  "address1": "서울시 강남구 테헤란로 123",
					  "address2": "4층 401호",
					  "shippingMemo": "부재 시 경비실에 맡겨주세요."
					}
					""".formatted(snapshotId)))
			.andExpect(status().isCreated())
			.andReturn();

		return JsonPath.read(orderResult.getResponse().getContentAsString(), "$.orderId");
	}

	private void seedEligibilityDays() {
		for (int offset = 2; offset < 20; offset++) {
			insertRecordedDay(LocalDate.parse("2026-04-07").minusDays(offset));
		}
	}

	private void insertRecordedDay(LocalDate localDate) {
		long dayCardId = 7000L + Math.abs(localDate.hashCode());
		long entryId = 8000L + Math.abs(localDate.hashCode());
		Instant baseInstant = localDate.atStartOfDay().atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant();

		jdbcTemplate.update(
			"""
				insert into day_cards (
				  id,
				  couple_id,
				  local_date,
				  state,
				  close_at_utc,
				  closed_at,
				  created_at,
				  updated_at
				) values (?, ?, ?, ?, ?, ?, ?, ?)
				""",
			dayCardId,
			"cpl_active_20260407",
			Date.valueOf(localDate),
			"PARTIAL",
			Timestamp.from(baseInstant.plusSeconds(19L * 60L * 60L)),
			null,
			Timestamp.from(baseInstant),
			Timestamp.from(baseInstant)
		);

		jdbcTemplate.update(
			"""
				insert into card_entries (
				  id,
				  day_card_id,
				  user_id,
				  emotion_code,
				  memo,
				  photo_url,
				  uploaded_asset_id,
				  created_at,
				  updated_at
				) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""",
			entryId,
			dayCardId,
			"local-user-1",
			"calm",
			"웹훅 테스트용 기록이에요.",
			null,
			null,
			Timestamp.from(baseInstant),
			Timestamp.from(baseInstant)
		);
	}

	private String sign(String timestamp, String payload, String secretKey) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		byte[] digest = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
		StringBuilder hex = new StringBuilder();
		for (byte value : digest) {
			hex.append(String.format("%02x", value));
		}
		return "sha256=" + hex;
	}

	@TestConfiguration
	static class FakeSweetbookConfig {

		@Bean
		@Primary
		SweetbookClient sweetbookClient() {
			return new SweetbookClient() {
				@Override
				public CreateBookResult createBook(CreateBookCommand command) {
					return new CreateBookResult("bk_fake_123");
				}

				@Override
				public UploadPhotoResult uploadPhoto(String bookUid, UploadPhotoCommand command) {
					return new UploadPhotoResult("photo_fake_123.JPG");
				}

				@Override
				public void createCover(String bookUid, String templateUid, java.util.Map<String, Object> parameters) {
				}

				@Override
				public void createContent(String bookUid, String templateUid, java.util.Map<String, Object> parameters, String breakBefore) {
				}

				@Override
				public FinalizeBookResult finalizeBook(String bookUid) {
					return new FinalizeBookResult(24, Instant.parse("2026-04-07T00:05:00Z"));
				}

				@Override
				public CreateOrderResult createOrder(CreateOrderCommand command) {
					return new CreateOrderResult(
						"or_fake_123",
						20,
						"결제완료",
						Instant.parse("2026-04-07T00:10:00Z")
					);
				}
			};
		}
	}
}
