package dev.earlydreamer.todayus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "today-us.supabase")
public record SupabaseProperties(String projectUrl) {

	public String resolveJwkSetUri() {
		String normalizedProjectUrl = normalize(projectUrl);
		if (normalizedProjectUrl == null) {
			return null;
		}
		return normalizedProjectUrl + "/auth/v1/.well-known/jwks.json";
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return null;
		}

		return trimmed.replaceAll("/+$", "");
	}
}
