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

@Entity
@Table(name = "sweetbook_uploaded_assets")
public class SweetbookUploadedAssetEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sweetbook_book_id", nullable = false)
	private SweetbookBookEntity sweetbookBook;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "uploaded_asset_id", nullable = false)
	private UploadedAssetEntity uploadedAsset;

	@Column(name = "sweetbook_file_name", nullable = false, length = 255)
	private String sweetbookFileName;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private SweetbookUploadedAssetStatus status;

	@Column(name = "uploaded_at")
	private Instant uploadedAt;

	@Column(name = "failure_code", length = 100)
	private String failureCode;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected SweetbookUploadedAssetEntity() {
	}

	public SweetbookUploadedAssetEntity(
		SweetbookBookEntity sweetbookBook,
		UploadedAssetEntity uploadedAsset,
		String sweetbookFileName,
		SweetbookUploadedAssetStatus status,
		Instant uploadedAt
	) {
		this.sweetbookBook = sweetbookBook;
		this.uploadedAsset = uploadedAsset;
		this.sweetbookFileName = sweetbookFileName;
		this.status = status;
		this.uploadedAt = uploadedAt;
	}
}
