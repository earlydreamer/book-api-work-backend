package dev.earlydreamer.todayus.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
	"spring.config.import=optional:file:.env[.properties]",
	"today-us.security.allowed-origins=https://today-us.earlydreamer.dev,http://localhost:3000",
	"today-us.security.allowed-origin-patterns=https://*.pages.dev",
	"today-us.r2.account-id=test-r2-account",
	"today-us.r2.access-key-id=test-r2-access-key",
	"today-us.r2.secret-access-key=test-r2-secret",
	"today-us.r2.bucket=test-r2-bucket",
	"today-us.r2.public-base-url=https://cdn.example.com",
	"today-us.r2.upload-prefix=uploads",
	"today-us.r2.presign-ttl-seconds=900",
	"today-us.sweetbook.base-url=https://api-sandbox.sweetbook.com",
	"today-us.sweetbook.api-key=test-sweetbook-key",
	"today-us.sweetbook.book-spec-id=book-spec-id",
	"today-us.sweetbook.template-id=template-id",
	"today-us.sweetbook.webhook-secret=test-webhook-secret"
})
@ActiveProfiles("test")
class ExternalPropertiesBindingTest {

	@Autowired
	private R2Properties r2Properties;

	@Autowired
	private SecurityProperties securityProperties;

	@Autowired
	private SweetbookProperties sweetbookProperties;

	@Autowired
	private WebClient sweetbookWebClient;

	@Test
	void bindsR2AndSweetbookPropertiesFromExternalConfig() {
		assertThat(securityProperties.allowedOrigins())
			.containsExactly("https://today-us.earlydreamer.dev", "http://localhost:3000");
		assertThat(securityProperties.allowedOriginPatterns())
			.containsExactly("https://*.pages.dev");

		assertThat(r2Properties.accountId()).isEqualTo("test-r2-account");
		assertThat(r2Properties.accessKeyId()).isEqualTo("test-r2-access-key");
		assertThat(r2Properties.secretAccessKey()).isEqualTo("test-r2-secret");
		assertThat(r2Properties.bucket()).isEqualTo("test-r2-bucket");
		assertThat(r2Properties.publicBaseUrl()).isEqualTo("https://cdn.example.com");
		assertThat(r2Properties.uploadPrefix()).isEqualTo("uploads");
		assertThat(r2Properties.presignTtlSeconds()).isEqualTo(900);

		assertThat(sweetbookProperties.baseUrl()).isEqualTo("https://api-sandbox.sweetbook.com");
		assertThat(sweetbookProperties.apiKey()).isEqualTo("test-sweetbook-key");
		assertThat(sweetbookProperties.bookSpecId()).isEqualTo("book-spec-id");
		assertThat(sweetbookProperties.templateId()).isEqualTo("template-id");
		assertThat(sweetbookProperties.webhookSecret()).isEqualTo("test-webhook-secret");

		assertThat(sweetbookWebClient).isNotNull();
	}
}
