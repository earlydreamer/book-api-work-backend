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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "book_snapshots")
public class BookSnapshotEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "couple_id", nullable = false)
	private CoupleEntity couple;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private BookSnapshotStatus status;

	@Column(name = "window_start_date", nullable = false)
	private LocalDate windowStartDate;

	@Column(name = "window_end_date", nullable = false)
	private LocalDate windowEndDate;

	@Column(name = "recorded_days", nullable = false)
	private int recordedDays;

	@Column(name = "selected_item_count", nullable = false)
	private int selectedItemCount;

	@Column(name = "build_started_at", nullable = false)
	private Instant buildStartedAt;

	@Column(name = "build_completed_at")
	private Instant buildCompletedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<BookSnapshotItemEntity> items = new ArrayList<>();

	protected BookSnapshotEntity() {
	}

	public BookSnapshotEntity(
		CoupleEntity couple,
		BookSnapshotStatus status,
		LocalDate windowStartDate,
		LocalDate windowEndDate,
		int recordedDays,
		int selectedItemCount,
		Instant buildStartedAt
	) {
		this.couple = couple;
		this.status = status;
		this.windowStartDate = windowStartDate;
		this.windowEndDate = windowEndDate;
		this.recordedDays = recordedDays;
		this.selectedItemCount = selectedItemCount;
		this.buildStartedAt = buildStartedAt;
	}

	public void addItem(BookSnapshotItemEntity item) {
		this.items.add(item);
	}

	public void markReadyToOrder(Instant buildCompletedAt) {
		this.status = BookSnapshotStatus.READY_TO_ORDER;
		this.buildCompletedAt = buildCompletedAt;
	}

	public void markOrdered() {
		this.status = BookSnapshotStatus.ORDERED;
	}

	public void reopenForOrder() {
		this.status = BookSnapshotStatus.READY_TO_ORDER;
	}

	public Long getId() {
		return id;
	}

	public BookSnapshotStatus getStatus() {
		return status;
	}

	public CoupleEntity getCouple() {
		return couple;
	}

	public LocalDate getWindowStartDate() {
		return windowStartDate;
	}

	public LocalDate getWindowEndDate() {
		return windowEndDate;
	}

	public int getRecordedDays() {
		return recordedDays;
	}

	public int getSelectedItemCount() {
		return selectedItemCount;
	}

	public Instant getBuildStartedAt() {
		return buildStartedAt;
	}

	public Instant getBuildCompletedAt() {
		return buildCompletedAt;
	}

	public List<BookSnapshotItemEntity> getItems() {
		return items;
	}
}
