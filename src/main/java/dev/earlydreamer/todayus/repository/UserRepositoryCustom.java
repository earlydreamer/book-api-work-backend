package dev.earlydreamer.todayus.repository;

public interface UserRepositoryCustom {

	int insertIfAbsent(String id, String authProvider, String displayName, String role);
}
