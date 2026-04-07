package dev.earlydreamer.todayus.config;

import java.time.ZonedDateTime;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "today-us.time")
public record TimeProperties(String zoneId, ZonedDateTime fixedDateTime) {
}
