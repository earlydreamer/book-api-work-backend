package dev.earlydreamer.todayus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "uploaded_assets")
public class UploadedAssetEntity {

	@Id
	@Column(name = "id", nullable = false, length = 100)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_user_id", nullable = false)
	private UserEntity ownerUser;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "couple_id", nullable = false)
	private CoupleEntity couple;

	@Column(name = "object_key", nullable = false, unique = true, length = 500)
	private String objectKey;

	@Column(name = "public_url", nullable = false, length = 2000)
	private String publicUrl;

	@Column(name = "original_file_name", nullable = false, length = 255)
	private String originalFileName;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(name = "file_size", nullable = false)
	private long fileSize;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private UploadedAssetStatus status;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "uploaded_at")
	private Instant uploadedAt;

	protected UploadedAssetEntity() {
	}

	public UploadedAssetEntity(
		String id,
		UserEntity ownerUser,
		CoupleEntity couple,
		String objectKey,
		String publicUrl,
		String originalFileName,
		String contentType,
		long fileSize
	) {
		this.id = id;
		this.ownerUser = ownerUser;
		this.couple = couple;
		this.objectKey = objectKey;
		this.publicUrl = publicUrl;
		this.originalFileName = originalFileName;
		this.contentType = contentType;
		this.fileSize = fileSize;
		this.status = UploadedAssetStatus.PENDING_UPLOAD;
	}

	public void markUploaded(Instant uploadedAt) {
		if (this.status == UploadedAssetStatus.UPLOADED) {
			return;
		}
		this.status = UploadedAssetStatus.UPLOADED;
		this.uploadedAt = uploadedAt;
	}

	public String getId() {
		return id;
	}

	public UserEntity getOwnerUser() {
		return ownerUser;
	}

	public CoupleEntity getCouple() {
		return couple;
	}

	public String getObjectKey() {
		return objectKey;
	}

	public String getPublicUrl() {
		return publicUrl;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public String getContentType() {
		return contentType;
	}

	public UploadedAssetStatus getStatus() {
		return status;
	}
}
