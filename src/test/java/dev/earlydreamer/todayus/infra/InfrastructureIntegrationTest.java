package dev.earlydreamer.todayus.infra;

import static org.assertj.core.api.Assertions.assertThat;

import dev.earlydreamer.todayus.service.CurrentUserProvider;
import dev.earlydreamer.todayus.service.JwtCurrentUserProvider;
import dev.earlydreamer.todayus.service.R2UploadUrlSigner;
import dev.earlydreamer.todayus.service.AwsR2UploadUrlSigner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
	"today-us.security.auth-enabled=true",
	"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://example.invalid/auth/v1/.well-known/jwks.json",
	"today-us.r2.account-id=test-account",
	"today-us.r2.access-key-id=test-access-key",
	"today-us.r2.secret-access-key=test-secret-key",
	"today-us.r2.bucket=test-bucket"
})
@ActiveProfiles("test")
class InfrastructureIntegrationTest {

	@Autowired
	private R2UploadUrlSigner r2UploadUrlSigner;

	@Autowired
	private CurrentUserProvider currentUserProvider;

	@Test
	void r2IntegrationActivatesWhenPropertiesProvided() {
		assertThat(r2UploadUrlSigner).isInstanceOf(AwsR2UploadUrlSigner.class);
	}

	@Test
	void supabaseAuthActivatesWhenAuthEnabled() {
		assertThat(currentUserProvider).isInstanceOf(JwtCurrentUserProvider.class);
	}
}
