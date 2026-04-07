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
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
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
@Import(OrderControllerIntegrationTest.FakeSweetbookConfig.class)
@Sql(scripts = "/db/test/reset.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/db/test/seed.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class OrderControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void createOrderMarksSnapshotOrderedAndExposesLatestOrderSummary() throws Exception {
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
			.andExpect(jsonPath("$.orderId").isNumber())
			.andExpect(jsonPath("$.snapshotId").value(snapshotId))
			.andExpect(jsonPath("$.status").value("submitted"))
			.andExpect(jsonPath("$.sweetbookOrderUid").value("or_fake_123"))
			.andExpect(jsonPath("$.recipientName").value("김지우"))
			.andExpect(jsonPath("$.postalCode").value("06101"))
			.andReturn();

		Integer orderId = JsonPath.read(orderResult.getResponse().getContentAsString(), "$.orderId");

		mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.orderId").value(orderId))
			.andExpect(jsonPath("$.status").value("submitted"))
			.andExpect(jsonPath("$.sweetbookOrderUid").value("or_fake_123"));

		mockMvc.perform(get("/api/v1/book-snapshots/current"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.bookProgress.state").value("ordered"))
			.andExpect(jsonPath("$.snapshot.status").value("ordered"))
			.andExpect(jsonPath("$.order.orderId").value(orderId))
			.andExpect(jsonPath("$.order.status").value("submitted"));
	}

	@Test
	void createOrderRejectsDuplicateOrderForSameSnapshot() throws Exception {
		seedEligibilityDays();

		MvcResult buildResult = mockMvc.perform(post("/api/v1/book-snapshots/current/build"))
			.andExpect(status().isOk())
			.andReturn();
		Integer snapshotId = JsonPath.read(buildResult.getResponse().getContentAsString(), "$.snapshot.snapshotId");

		String requestBody = """
			{
			  "snapshotId": %d,
			  "recipientName": "김지우",
			  "recipientPhone": "010-1234-5678",
			  "postalCode": "06101",
			  "address1": "서울시 강남구 테헤란로 123",
			  "address2": "4층 401호",
			  "shippingMemo": "부재 시 경비실에 맡겨주세요."
			}
			""".formatted(snapshotId);

		mockMvc.perform(post("/api/v1/orders")
				.contentType(APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/orders")
				.contentType(APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("order_already_exists"));
	}

	private void seedEligibilityDays() {
		for (int offset = 2; offset < 20; offset++) {
			insertRecordedDay(LocalDate.parse("2026-04-07").minusDays(offset));
		}
	}

	private void insertRecordedDay(LocalDate localDate) {
		long dayCardId = 5000L + Math.abs(localDate.hashCode());
		long entryId = 6000L + Math.abs(localDate.hashCode());
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
			"주문 자격을 채우는 기록이에요.",
			null,
			null,
			Timestamp.from(baseInstant),
			Timestamp.from(baseInstant)
		);
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
