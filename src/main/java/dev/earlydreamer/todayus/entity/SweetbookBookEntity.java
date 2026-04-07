package dev.earlydreamer.todayus.entity;

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
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "sweetbook_books")
public class SweetbookBookEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "snapshot_id", nullable = false, unique = true)
	private BookSnapshotEntity snapshot;

	@Column(name = "sweetbook_book_uid", unique = true, length = 100)
	private String sweetbookBookUid;

	@Column(name = "book_spec_id", nullable = false, length = 100)
	private String bookSpecId;

	@Column(name = "template_id", nullable = false, length = 100)
	private String templateId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private SweetbookBookStatus status;

	@Column(name = "created_request_id", nullable = false, length = 150)
	private String createdRequestId;

	@Column(name = "finalized_at")
	private Instant finalizedAt;

	@Column(name = "failure_code", length = 100)
	private String failureCode;

	@Column(name = "failure_message", length = 2000)
	private String failureMessage;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected SweetbookBookEntity() {
	}

	public SweetbookBookEntity(BookSnapshotEntity snapshot, String bookSpecId, String templateId, String createdRequestId) {
		this.snapshot = snapshot;
		this.bookSpecId = bookSpecId;
		this.templateId = templateId;
		this.createdRequestId = createdRequestId;
		this.status = SweetbookBookStatus.BUILDING;
	}

	public void attachBookUid(String sweetbookBookUid) {
		this.sweetbookBookUid = sweetbookBookUid;
	}

	public void markFinalized(Instant finalizedAt) {
		this.status = SweetbookBookStatus.FINALIZED;
		this.finalizedAt = finalizedAt;
		this.failureCode = null;
		this.failureMessage = null;
	}

	public void markFailed(String failureCode, String failureMessage) {
		this.status = SweetbookBookStatus.FAILED;
		this.failureCode = failureCode;
		this.failureMessage = failureMessage;
	}

	public Long getId() {
		return id;
	}

	public BookSnapshotEntity getSnapshot() {
		return snapshot;
	}

	public String getSweetbookBookUid() {
		return sweetbookBookUid;
	}

	public SweetbookBookStatus getStatus() {
		return status;
	}
}
