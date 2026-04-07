package dev.earlydreamer.todayus.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

	@Bean
	Clock todayUsClock(TimeProperties timeProperties) {
		ZoneId zoneId = ZoneId.of(timeProperties.zoneId());
		if (timeProperties.fixedDateTime() != null) {
			return Clock.fixed(timeProperties.fixedDateTime().toInstant(), zoneId);
		}
		return Clock.system(zoneId);
	}
}
