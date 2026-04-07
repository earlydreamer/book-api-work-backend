package dev.earlydreamer.todayus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "users")
public class UserEntity {

	@Id
	@Column(name = "id", nullable = false, length = 100)
	private String id;

	@Column(name = "auth_provider", nullable = false, length = 50)
	private String authProvider;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private UserRole role;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	protected UserEntity() {
	}

	public UserEntity(String id, String authProvider, String displayName, UserRole role) {
		this.id = id;
		this.authProvider = authProvider;
		this.displayName = displayName;
		this.role = role;
	}

	public String getId() {
		return id;
	}

	public String getAuthProvider() {
		return authProvider;
	}

	public String getDisplayName() {
		return displayName;
	}

	public UserRole getRole() {
		return role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}
}
