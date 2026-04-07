package dev.earlydreamer.todayus.repository;

import static org.assertj.core.api.Assertions.assertThat;

import dev.earlydreamer.todayus.entity.UserEntity;
import dev.earlydreamer.todayus.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class PostgresSchemaIntegrationTest {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
		registry.add("today-us.security.auth-enabled", () -> "false");
	}

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserRepository userRepository;

	@Test
	void flywayMigrationAndJpaMappingWorkAgainstPostgres() {
		Integer appliedMigrations = jdbcTemplate.queryForObject(
			"select count(*) from flyway_schema_history where success = true",
			Integer.class
		);
		assertThat(appliedMigrations).isNotNull().isGreaterThanOrEqualTo(1);

		UserEntity saved = userRepository.save(new UserEntity(
			"pg-user-1",
			"supabase",
			"지우",
			UserRole.USER
		));

		assertThat(userRepository.findById(saved.getId()))
			.isPresent()
			.get()
			.extracting(UserEntity::getDisplayName, UserEntity::getAuthProvider)
			.containsExactly("지우", "supabase");
	}
}
