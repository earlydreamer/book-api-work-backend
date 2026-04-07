package dev.earlydreamer.todayus.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "today-us.security", name = "auth-enabled", havingValue = "true", matchIfMissing = true)
public class JwtCurrentUserProvider implements CurrentUserProvider {

	@Override
	public CurrentUserIdentity getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Jwt jwt = extractJwt(authentication);
		if (jwt == null) {
			throw new IllegalStateException("인증된 사용자를 찾을 수 없어요.");
		}

		String authUserId = jwt.getSubject();
		if (authUserId == null || authUserId.isBlank()) {
			throw new IllegalStateException("인증된 사용자를 찾을 수 없어요.");
		}
		String displayName = jwt.getClaimAsString("name");
		if (displayName == null || displayName.isBlank()) {
			displayName = authUserId;
		}
		return new CurrentUserIdentity(authUserId, displayName, "supabase");
	}

	private Jwt extractJwt(Authentication authentication) {
		if (authentication == null) {
			return null;
		}
		if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
			return jwtAuthenticationToken.getToken();
		}
		if (authentication.getPrincipal() instanceof Jwt jwt) {
			return jwt;
		}
		return null;
	}
}
