package dev.earlydreamer.todayus.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "today-us.security", name = "auth-enabled", havingValue = "false")
public class LocalCurrentUserProvider implements CurrentUserProvider {

	private static final String LOCAL_USER_HEADER = "X-Today-Us-Auth-User-Id";
	private static final String DEFAULT_LOCAL_USER_ID = "local-user-1";
	private static final Map<String, String> LOCAL_DISPLAY_NAMES = Map.of(
		"local-user-1", "지우",
		"local-user-2", "민준",
		"local-user-3", "서윤",
		"local-user-4", "하늘",
		"local-user-5", "도윤"
	);

	private final ObjectProvider<HttpServletRequest> requestProvider;

	public LocalCurrentUserProvider(ObjectProvider<HttpServletRequest> requestProvider) {
		this.requestProvider = requestProvider;
	}

	@Override
	public CurrentUserIdentity getCurrentUser() {
		HttpServletRequest request = requestProvider.getIfAvailable();
		String authUserId = request != null ? request.getHeader(LOCAL_USER_HEADER) : null;
		if (authUserId == null || authUserId.isBlank()) {
			authUserId = DEFAULT_LOCAL_USER_ID;
		}
		String displayName = LOCAL_DISPLAY_NAMES.getOrDefault(authUserId, authUserId);
		return new CurrentUserIdentity(authUserId, displayName, "local-dev");
	}
}
