package dev.earlydreamer.todayus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "today-us.sweetbook")
public record SweetbookProperties(
	String baseUrl,
	String apiKey,
	String bookSpecId,
	String templateId,
	String webhookSecret
) {
}
