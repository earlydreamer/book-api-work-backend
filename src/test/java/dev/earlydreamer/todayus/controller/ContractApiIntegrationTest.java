package dev.earlydreamer.todayus.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/db/test/reset.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/db/test/seed.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ContractApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void meHomeReturnsFeedFriendlyContract() throws Exception {
		mockMvc.perform(get("/api/v1/me/home"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.relationship.state").value("connected"))
			.andExpect(jsonPath("$.relationship.coupleId").value("cpl_active_20260407"))
			.andExpect(jsonPath("$.todayCard.state").value("mine-only"))
			.andExpect(jsonPath("$.recentMoments[0].state").value("partial"))
			.andExpect(jsonPath("$.bookProgress.state").value("growing"))
			.andExpect(jsonPath("$.sections").doesNotExist());
	}

	@Test
	void archiveSeparatesCurrentAndArchivedSections() throws Exception {
		mockMvc.perform(get("/api/v1/archive"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sections[0].type").value("current"))
			.andExpect(jsonPath("$.sections[1].type").value("archived"))
			.andExpect(jsonPath("$.sections[1].recordLabel").value("이전 파트너와의 기록"));
	}

	@Test
	void createInviteReturnsConflictWhenCurrentUserAlreadyHasActiveRelationship() throws Exception {
		mockMvc.perform(post("/api/v1/couples/invites")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "startDate": "2026-04-07"
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("active_relationship_conflict"));
	}

	@Test
	void createInviteRejectsImpossibleStartDate() throws Exception {
		mockMvc.perform(post("/api/v1/couples/invites")
				.header("X-Today-Us-Auth-User-Id", "local-user-4")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "startDate": "2026-02-30"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("validation_failed"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("startDate"));
	}

	@Test
	void invitePreviewReturnsProblemDetailWhenMissing() throws Exception {
		mockMvc.perform(get("/api/v1/couples/invites/TODAY9999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("invite_not_found"))
			.andExpect(jsonPath("$.title").value("초대 코드를 찾을 수 없어요."));
	}

	@Test
	void acceptInviteRejectsTooShortInviteCodeAsValidationFailure() throws Exception {
		mockMvc.perform(post("/api/v1/couples/invites/x/accept"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("validation_failed"));
	}

	@Test
	void inviteCreatePreviewAcceptFlowPersistsAcrossRequests() throws Exception {
		MvcResult createInviteResult = mockMvc.perform(post("/api/v1/couples/invites")
				.header("X-Today-Us-Auth-User-Id", "local-user-4")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "startDate": "2026-04-08"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.relationship.state").value("invite-pending"))
			.andExpect(jsonPath("$.relationship.inviteCode").value(org.hamcrest.Matchers.matchesPattern("^TODAY[A-Z0-9]{8}$")))
			.andExpect(jsonPath("$.relationship.partnerName").isEmpty())
			.andReturn();

		String inviteCode = JsonPath.read(
			createInviteResult.getResponse().getContentAsString(),
			"$.relationship.inviteCode"
		);

		mockMvc.perform(get("/api/v1/couples/invites/{inviteCode}", inviteCode))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.inviteCode").value(inviteCode))
			.andExpect(jsonPath("$.inviterName").value("하늘"))
			.andExpect(jsonPath("$.status").value("invite-pending"));

		mockMvc.perform(post("/api/v1/couples/invites/{inviteCode}/accept", inviteCode)
				.header("X-Today-Us-Auth-User-Id", "local-user-5"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.relationship.state").value("connected"))
			.andExpect(jsonPath("$.relationship.partnerName").value("하늘"))
			.andExpect(jsonPath("$.bookProgress.recordedDays").value(0));
	}

	@Test
	void unlinkUpdatesSubsequentHomeState() throws Exception {
		mockMvc.perform(post("/api/v1/couples/current/unlink"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.relationship.state").value("unconnected"))
			.andExpect(jsonPath("$.archiveUpdated").value(true));

		mockMvc.perform(get("/api/v1/me/home"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.relationship.state").value("unconnected"))
			.andExpect(jsonPath("$.relationship.coupleId").isEmpty());
	}

	@Test
	void saveDayCardEntryRequiresEmotionCode() throws Exception {
		mockMvc.perform(put("/api/v1/day-cards/2026-04-07/entry")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "memo": "감정 없이 저장하면 안 돼요.",
					  "photoUrl": null
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("validation_failed"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("emotionCode"));
	}

	@Test
	void saveDayCardEntryRejectsImpossibleLocalDatePath() throws Exception {
		mockMvc.perform(put("/api/v1/day-cards/2026-02-30/entry")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "emotionCode": "calm",
					  "memo": "달력상 없는 날짜예요.",
					  "photoUrl": null
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("validation_failed"));
	}

	@Test
	void saveDayCardEntryUpdatesSubsequentTodayCardRead() throws Exception {
		mockMvc.perform(put("/api/v1/day-cards/2026-04-07/entry")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "emotionCode": "calm",
					  "memo": "사진 한 장 없이도 남길 수 있어요.",
					  "photoUrl": null
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.todayCard.state").value("mine-only"))
			.andExpect(jsonPath("$.todayCard.me.memo").value("사진 한 장 없이도 남길 수 있어요."))
			.andExpect(jsonPath("$.bookProgress.recordedDays").value(2));

		mockMvc.perform(get("/api/v1/day-cards/today"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.me.memo").value("사진 한 장 없이도 남길 수 있어요."));
	}

	@Test
	void currentBookSnapshotReturnsGrowthSummary() throws Exception {
		mockMvc.perform(get("/api/v1/book-snapshots/current"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.bookProgress.recordedDays").value(2))
			.andExpect(jsonPath("$.candidateMoments[0].state").value("partial"))
			.andExpect(jsonPath("$.candidateMoments[1].state").value("complete"))
			.andExpect(jsonPath("$.snapshot").isEmpty())
			.andExpect(jsonPath("$.order").isEmpty());
	}

	@Test
	void openApiDocsExposeEndpointSummariesAndSchemas() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.info.title").value("오늘 우리 백엔드 Contract API"))
			.andExpect(jsonPath("$.paths['/api/v1/me/home'].get.summary").value("피드 홈 요약 조회"))
			.andExpect(jsonPath("$.paths['/api/v1/couples/invites'].post.tags[0]").value("Couples"))
			.andExpect(jsonPath("$.paths['/api/v1/orders'].post.summary").value("수동 주문 생성"))
			.andExpect(jsonPath("$.paths['/api/v1/webhooks/sweetbook'].post.tags[0]").value("Webhooks"))
			.andExpect(jsonPath("$.paths['/api/v1/day-cards/{localDate}/entry'].put.parameters[0].description")
				.value("기록을 저장할 로컬 날짜예요. yyyy-MM-dd 형식을 사용해요."))
			.andExpect(jsonPath("$.components.schemas.CreateInviteRequest.properties.startDate.description")
				.value("연결을 시작한 날짜예요. yyyy-MM-dd 형식을 사용해요."))
			.andExpect(jsonPath("$.components.schemas.CreateOrderRequest.properties.postalCode.description")
				.value("우편번호예요."))
			.andExpect(jsonPath("$.components.schemas.SaveDayCardEntryRequest.properties.emotionCode.description")
				.value("선택한 감정 코드예요."));
	}
}
