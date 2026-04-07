package dev.earlydreamer.todayus.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "day_cards")
public class DayCardEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "couple_id", nullable = false)
	private CoupleEntity couple;

	@Column(name = "local_date", nullable = false)
	private LocalDate localDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "state", nullable = false, length = 20)
	private DayCardStatus state;

	@Column(name = "close_at_utc")
	private Instant closeAtUtc;

	@Column(name = "closed_at")
	private Instant closedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "dayCard", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CardEntryEntity> entries = new ArrayList<>();

	protected DayCardEntity() {
	}

	public DayCardEntity(CoupleEntity couple, LocalDate localDate, Instant closeAtUtc) {
		this.couple = couple;
		this.localDate = localDate;
		this.closeAtUtc = closeAtUtc;
		this.state = DayCardStatus.EMPTY;
	}

	public Optional<CardEntryEntity> findEntry(String userId) {
		return this.entries.stream()
			.filter((entry) -> entry.getUser().getId().equals(userId))
			.findFirst();
	}

	public void upsertEntry(UserEntity user, String emotionCode, String memo, String photoUrl) {
		CardEntryEntity entry = findEntry(user.getId())
			.orElseGet(() -> {
				CardEntryEntity created = new CardEntryEntity(this, user);
				this.entries.add(created);
				return created;
			});
		entry.update(emotionCode, memo, photoUrl);
		refreshState();
	}

	public void refreshState() {
		long populatedEntryCount = this.entries.stream().filter(CardEntryEntity::hasRecordedContent).count();
		if (closedAt != null) {
			this.state = DayCardStatus.CLOSED;
			return;
		}
		if (populatedEntryCount == 0) {
			this.state = DayCardStatus.EMPTY;
			return;
		}
		this.state = populatedEntryCount >= 2 ? DayCardStatus.COMPLETE : DayCardStatus.PARTIAL;
	}

	public Long getId() {
		return id;
	}

	public CoupleEntity getCouple() {
		return couple;
	}

	public LocalDate getLocalDate() {
		return localDate;
	}

	public DayCardStatus getState() {
		return state;
	}

	public Instant getClosedAt() {
		return closedAt;
	}

	public List<CardEntryEntity> getEntries() {
		return entries;
	}
}
