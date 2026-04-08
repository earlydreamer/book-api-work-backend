package dev.earlydreamer.todayus.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
	"today-us.security.auth-enabled=true",
	"today-us.security.allowed-origins=https://today-us.earlydreamer.dev",
	"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://example.invalid/auth/v1/.well-known/jwks.json"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void authEnabledWithoutTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/me/home"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void authEnabledAllowsPublicMatchersWithoutToken() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk());
	}

	@Test
	void authEnabledHandlesCorsPreflightForAllowedOrigin() throws Exception {
		mockMvc.perform(options("/api/v1/me/home")
				.header("Origin", "https://today-us.earlydreamer.dev")
				.header("Access-Control-Request-Method", "GET"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "https://today-us.earlydreamer.dev"));
	}

	@Test
	void authEnabledJwtWithSubjectAndNameReturnsOk() throws Exception {
		mockMvc.perform(get("/api/v1/me/home")
				.with(jwt().jwt((jwt) -> jwt
					.subject("supabase-user-1")
					.claim("name", "지우"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.relationship.myName").value("지우"))
			.andExpect(jsonPath("$.relationship.state").value("unconnected"));
	}

	@Test
	void authEnabledJwtIgnoresLocalHeaderFallbackAndUsesJwtSubject() throws Exception {
		mockMvc.perform(get("/api/v1/me/home")
				.header("X-Today-Us-Auth-User-Id", "local-user-1")
				.with(jwt().jwt((jwt) -> jwt
					.subject("supabase-user-1")
					.claim("name", "지우"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.relationship.state").value("unconnected"))
			.andExpect(jsonPath("$.relationship.coupleId").isEmpty());
	}
}
