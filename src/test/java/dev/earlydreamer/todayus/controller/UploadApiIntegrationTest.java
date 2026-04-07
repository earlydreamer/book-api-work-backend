package dev.earlydreamer.todayus.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/db/test/reset.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/db/test/seed.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UploadApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createUploadIntentReturnsR2DirectUploadContract() throws Exception {
		mockMvc.perform(post("/api/v1/uploads/intents")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "fileName": "photo.jpg",
					  "contentType": "image/jpeg",
					  "fileSize": 231231
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.assetId").value(matchesPattern("^asset_[a-z0-9]+$")))
			.andExpect(jsonPath("$.objectKey").value(matchesPattern("^uploads/local-user-1/2026/04/07/.+photo\\.jpg$")))
			.andExpect(jsonPath("$.uploadUrl").value(containsString("X-Amz-Algorithm=AWS4-HMAC-SHA256")))
			.andExpect(jsonPath("$.publicUrl").value(matchesPattern("^https://cdn\\.example\\.com/uploads/local-user-1/2026/04/07/.+photo\\.jpg$")))
			.andExpect(jsonPath("$.expiresInSeconds").value(900));
	}

	@Test
	void completeUploadThenSaveDayCardWithUploadedAssetIdReflectsPhotoUrl() throws Exception {
		MvcResult createIntentResult = mockMvc.perform(post("/api/v1/uploads/intents")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "fileName": "photo.jpg",
					  "contentType": "image/jpeg",
					  "fileSize": 231231
					}
					"""))
			.andExpect(status().isOk())
			.andReturn();

		String assetId = JsonPath.read(createIntentResult.getResponse().getContentAsString(), "$.assetId");
		String publicUrl = JsonPath.read(createIntentResult.getResponse().getContentAsString(), "$.publicUrl");

		mockMvc.perform(post("/api/v1/uploads/complete")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "assetId": "%s"
					}
					""".formatted(assetId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.assetId").value(assetId))
			.andExpect(jsonPath("$.status").value("uploaded"));

		mockMvc.perform(put("/api/v1/day-cards/2026-04-07/entry")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "emotionCode": "calm",
					  "memo": "업로드 자산으로 저장해요.",
					  "uploadedAssetId": "%s"
					}
					""".formatted(assetId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.todayCard.me.photoUrl").value(publicUrl));
	}

	@Test
	void saveDayCardEntryRejectsUploadedAssetOwnedByAnotherUser() throws Exception {
		MvcResult createIntentResult = mockMvc.perform(post("/api/v1/uploads/intents")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "fileName": "photo.jpg",
					  "contentType": "image/jpeg",
					  "fileSize": 231231
					}
					"""))
			.andExpect(status().isOk())
			.andReturn();

		String assetId = JsonPath.read(createIntentResult.getResponse().getContentAsString(), "$.assetId");

		mockMvc.perform(post("/api/v1/uploads/complete")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "assetId": "%s"
					}
					""".formatted(assetId)))
			.andExpect(status().isOk());

		mockMvc.perform(put("/api/v1/day-cards/2026-04-07/entry")
				.header("X-Today-Us-Auth-User-Id", "local-user-2")
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "emotionCode": "loving",
					  "memo": "다른 사람이 올린 자산을 붙여보려 해요.",
					  "uploadedAssetId": "%s"
					}
					""".formatted(assetId)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("uploaded_asset_forbidden"));
	}
}
