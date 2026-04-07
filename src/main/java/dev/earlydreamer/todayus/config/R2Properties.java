package dev.earlydreamer.todayus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "today-us.r2")
public record R2Properties(
	String accountId,
	String accessKeyId,
	String secretAccessKey,
	String bucket,
	String publicBaseUrl,
	String uploadPrefix,
	int presignTtlSeconds
) {
}
