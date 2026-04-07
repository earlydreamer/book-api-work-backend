package dev.earlydreamer.todayus.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
		http.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.authorizeHttpRequests((auth) -> auth
			.requestMatchers(PUBLIC_MATCHERS).permitAll()
			.anyRequest().authenticated());
		http.oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}

	@Bean
	@ConditionalOnProperty(prefix = "today-us.security", name = "auth-enabled", havingValue = "false")
	SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable);
		http.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.authorizeHttpRequests((auth) -> auth.anyRequest().permitAll());
		return http.build();
	}
}
