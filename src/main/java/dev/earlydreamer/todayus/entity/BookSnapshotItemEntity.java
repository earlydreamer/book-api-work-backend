package dev.earlydreamer.todayus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "book_snapshot_items")
public class BookSnapshotItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "snapshot_id", nullable = false)
	private BookSnapshotEntity snapshot;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "day_card_id", nullable = false)
	private DayCardEntity dayCard;

	@Column(name = "local_date", nullable = false)
	private LocalDate localDate;

	@Column(name = "my_entry_json", columnDefinition = "TEXT")
	private String myEntryJson;

	@Column(name = "partner_entry_json", columnDefinition = "TEXT")
	private String partnerEntryJson;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "photo_asset_id")
	private UploadedAssetEntity photoAsset;

	@Column(name = "page_order", nullable = false)
	private int pageOrder;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected BookSnapshotItemEntity() {
	}

	public BookSnapshotItemEntity(
		BookSnapshotEntity snapshot,
		DayCardEntity dayCard,
		LocalDate localDate,
		String myEntryJson,
		String partnerEntryJson,
		UploadedAssetEntity photoAsset,
		int pageOrder
	) {
		this.snapshot = snapshot;
		this.dayCard = dayCard;
		this.localDate = localDate;
		this.myEntryJson = myEntryJson;
		this.partnerEntryJson = partnerEntryJson;
		this.photoAsset = photoAsset;
		this.pageOrder = pageOrder;
	}

	public LocalDate getLocalDate() {
		return localDate;
	}

	public UploadedAssetEntity getPhotoAsset() {
		return photoAsset;
	}
}
