package dev.earlydreamer.todayus.repository;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class UserRepositoryImpl implements UserRepositoryCustom {

	private static final String POSTGRES_INSERT_IF_ABSENT = """
		insert into users (id, auth_provider, display_name, role, created_at, deleted_at)
		values (?, ?, ?, ?, current_timestamp, null)
		on conflict (id) do nothing
		""";

	private static final String H2_INSERT_IF_ABSENT = """
		merge into users (id, auth_provider, display_name, role, created_at, deleted_at)
		key(id)
		values (?, ?, ?, ?, current_timestamp, null)
		""";

	private final JdbcTemplate jdbcTemplate;
	private final DataSource dataSource;

	public UserRepositoryImpl(JdbcTemplate jdbcTemplate, DataSource dataSource) {
		this.jdbcTemplate = jdbcTemplate;
		this.dataSource = dataSource;
	}

	@Override
	public int insertIfAbsent(String id, String authProvider, String displayName, String role) {
		return jdbcTemplate.update(resolveInsertIfAbsentSql(), id, authProvider, displayName, role);
	}

	private String resolveInsertIfAbsentSql() {
		Connection connection = DataSourceUtils.getConnection(dataSource);
		try {
			String databaseProductName = connection.getMetaData().getDatabaseProductName();
			if (databaseProductName != null && databaseProductName.toLowerCase().contains("postgres")) {
				return POSTGRES_INSERT_IF_ABSENT;
			}
			return H2_INSERT_IF_ABSENT;
		} catch (SQLException exception) {
			throw new IllegalStateException("데이터베이스 종류를 확인할 수 없어요.", exception);
		} finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
	}
}
