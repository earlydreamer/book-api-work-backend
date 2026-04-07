package dev.earlydreamer.todayus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "today-us.security")
public record SecurityProperties(boolean authEnabled) {
}
