package dev.earlydreamer.todayus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

	@Bean
	WebClient sweetbookWebClient(SweetbookProperties sweetbookProperties) {
		return WebClient.builder()
			.baseUrl(sweetbookProperties.baseUrl())
			.build();
	}
}
