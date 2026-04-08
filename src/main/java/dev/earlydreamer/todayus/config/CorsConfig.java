package dev.earlydreamer.todayus.config;

import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

	private static final List<String> ALLOWED_METHODS = List.of(
		"GET",
		"POST",
		"PUT",
		"PATCH",
		"DELETE",
		"OPTIONS"
	);

	@Bean
	CorsConfigurationSource corsConfigurationSource(SecurityProperties securityProperties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(securityProperties.allowedOrigins());
		configuration.setAllowedOriginPatterns(securityProperties.allowedOriginPatterns());
		configuration.setAllowedMethods(ALLOWED_METHODS);
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setExposedHeaders(List.of("Location"));
		configuration.setAllowCredentials(false);
		configuration.setMaxAge(Duration.ofHours(1));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
