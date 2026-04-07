package dev.earlydreamer.todayus.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PostgresConcurrencyIntegrationTest {

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
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private ExecutorService executorService;

	@BeforeEach
	void setUp() {
		executorService = Executors.newFixedThreadPool(2);
		jdbcTemplate.execute("TRUNCATE TABLE card_entries, day_cards, couples, users RESTART IDENTITY CASCADE");
	}

	@AfterEach
	void tearDown() {
		executorService.shutdownNow();
	}

	@Test
	void concurrentFirstInviteCreationForSameNewUserLeavesSinglePendingRelationship() throws Exception {
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);

		Callable<Integer> createInvite = () -> {
			ready.countDown();
			assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
			return mockMvc.perform(post("/api/v1/couples/invites")
					.header("X-Today-Us-Auth-User-Id", "race-user-1")
					.contentType(APPLICATION_JSON)
					.content("""
						{
						  "startDate": "2026-04-08"
						}
						"""))
				.andReturn()
				.getResponse()
				.getStatus();
		};

		Future<Integer> first = executorService.submit(createInvite);
		Future<Integer> second = executorService.submit(createInvite);

		assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
		start.countDown();

		List<Integer> statuses = List.of(
			first.get(10, TimeUnit.SECONDS),
			second.get(10, TimeUnit.SECONDS)
		);

		assertThat(statuses).containsExactlyInAnyOrder(200, 409);
		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from users where id = ?",
			Integer.class,
			"race-user-1"
		)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from couples where creator_user_id = ? and status = 'INVITE_PENDING'",
			Integer.class,
			"race-user-1"
		)).isEqualTo(1);
	}

	@Test
	void firstReadRequestForNewUserCreatesUserAndReturnsHome() throws Exception {
		mockMvc.perform(get("/api/v1/me/home")
				.header("X-Today-Us-Auth-User-Id", "race-read-user-1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.relationship.state").value("unconnected"))
			.andExpect(jsonPath("$.relationship.myName").value("race-read-user-1"));

		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from users where id = ?",
			Integer.class,
			"race-read-user-1"
		)).isEqualTo(1);
	}
}
