package dev.earlydreamer.todayus.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final String[] PUBLIC_MATCHERS = {
		"/actuator/health",
		"/actuator/info"
	};

	@Bean
	@ConditionalOnProperty(prefix = "today-us.security", name = "auth-enabled", havingValue = "true", matchIfMissing = true)
	SecurityFilterChain authenticatedSecurityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable);
		http.cors(Customizer.withDefaults());
		http.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.authorizeHttpRequests((auth) -> auth
			.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
			.requestMatchers(PUBLIC_MATCHERS).permitAll()
			.anyRequest().authenticated());
		http.oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}

	@Bean
	@ConditionalOnProperty(prefix = "today-us.security", name = "auth-enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean(JwtDecoder.class)
	JwtDecoder jwtDecoder(Environment environment, SupabaseProperties supabaseProperties) {
		String configuredJwkSetUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");
		String resolvedJwkSetUri = normalize(configuredJwkSetUri);
		if (resolvedJwkSetUri == null) {
			resolvedJwkSetUri = supabaseProperties.resolveJwkSetUri();
		}
		if (resolvedJwkSetUri == null) {
			throw new IllegalStateException("JWT 검증 설정이 비어 있어요. `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` 또는 `TODAY_US_SUPABASE_PROJECT_URL`를 설정해 주세요.");
		}
		return NimbusJwtDecoder.withJwkSetUri(resolvedJwkSetUri).build();
	}

	@Bean
	@ConditionalOnProperty(prefix = "today-us.security", name = "auth-enabled", havingValue = "false")
	SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable);
		http.cors(Customizer.withDefaults());
		http.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.authorizeHttpRequests((auth) -> auth
			.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
			.anyRequest().permitAll());
		return http.build();
	}

	private String normalize(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
