package dev.earlydreamer.todayus;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@ConfigurationPropertiesScan
@SpringBootApplication
public class TodayUsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TodayUsBackendApplication.class, args);
	}

}
