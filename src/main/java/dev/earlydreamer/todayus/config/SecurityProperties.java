package dev.earlydreamer.todayus.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "today-us.security")
public record SecurityProperties(
	boolean authEnabled,
	List<String> allowedOrigins,
	List<String> allowedOriginPatterns
) {

	public SecurityProperties {
		allowedOrigins = normalize(allowedOrigins);
		allowedOriginPatterns = normalize(allowedOriginPatterns);
	}

	private static List<String> normalize(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream()
			.map(String::trim)
			.filter((value) -> !value.isEmpty())
			.toList();
	}
}
