package dev.earlydreamer.todayus.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:default-security;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.flyway.enabled=true",
	"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://example.invalid/auth/v1/.well-known/jwks.json"
})
@AutoConfigureMockMvc
class DefaultSecurityProfileIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void defaultProfileRequiresAuthenticationForProtectedEndpoint() throws Exception {
		mockMvc.perform(get("/api/v1/me/home"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void defaultProfileRequiresAuthenticationForApiDocs() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isUnauthorized());
	}
}
