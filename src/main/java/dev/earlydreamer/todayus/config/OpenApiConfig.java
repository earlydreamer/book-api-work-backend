package dev.earlydreamer.todayus.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI todayUsOpenApi() {
		return new OpenAPI()
			.info(new Info()
				.title("오늘 우리 백엔드 Contract API")
				.version("2026-04-07")
				.description("today-us-front와 백엔드 연동을 맞추기 위한 contract surface와 JPA core 구현"));
	}
}
