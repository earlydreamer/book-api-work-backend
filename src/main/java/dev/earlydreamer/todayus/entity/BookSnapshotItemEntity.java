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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "book_snapshot_items")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

	@Column(name = "me_memo", columnDefinition = "TEXT")
	private String meMemo;

	@Column(name = "partner_memo", columnDefinition = "TEXT")
	private String partnerMemo;

	@Column(name = "me_display_name", length = 100)
	private String meDisplayName;

	@Column(name = "partner_display_name", length = 100)
	private String partnerDisplayName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "photo_asset_id")
	private UploadedAssetEntity photoAsset;

	@Column(name = "page_order", nullable = false)
	private int pageOrder;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public BookSnapshotItemEntity(
		BookSnapshotEntity snapshot,
		DayCardEntity dayCard,
		LocalDate localDate,
		String myEntryJson,
		String partnerEntryJson,
		String meMemo,
		String partnerMemo,
		String meDisplayName,
		String partnerDisplayName,
		UploadedAssetEntity photoAsset,
		int pageOrder
	) {
		this.snapshot = snapshot;
		this.dayCard = dayCard;
		this.localDate = localDate;
		this.myEntryJson = myEntryJson;
		this.partnerEntryJson = partnerEntryJson;
		this.meMemo = meMemo;
		this.partnerMemo = partnerMemo;
		this.meDisplayName = meDisplayName;
		this.partnerDisplayName = partnerDisplayName;
		this.photoAsset = photoAsset;
		this.pageOrder = pageOrder;
	}
}
