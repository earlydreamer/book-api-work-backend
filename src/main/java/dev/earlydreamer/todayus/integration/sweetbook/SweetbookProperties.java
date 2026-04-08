package dev.earlydreamer.todayus.integration.sweetbook;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "today-us.sweetbook")
public class SweetbookProperties {
	private String apiKey;
	private String apiUrl = "https://api.sweetbook.com"; // 기본값 설정
	private String partnerId;
}
