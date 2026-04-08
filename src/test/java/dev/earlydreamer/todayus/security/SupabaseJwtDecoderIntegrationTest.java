package dev.earlydreamer.todayus.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
	webEnvironment = WebEnvironment.NONE,
	properties = {
		"today-us.security.auth-enabled=true",
		"today-us.supabase.project-url=https://project.supabase.co",
		"spring.datasource.url=jdbc:h2:mem:supabase-jwt-decoder;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.flyway.enabled=true"
	}
)
@ActiveProfiles("test")
class SupabaseJwtDecoderIntegrationTest {

	@Autowired
	private JwtDecoder jwtDecoder;

	@Test
	void supabaseProjectUrl만있어도JwtDecoder를만들어요() {
		assertThat(jwtDecoder).isNotNull();
	}
}
