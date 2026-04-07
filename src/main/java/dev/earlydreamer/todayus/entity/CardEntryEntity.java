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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "card_entries")
public class CardEntryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "day_card_id", nullable = false)
	private DayCardEntity dayCard;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	@Column(name = "emotion_code", nullable = false, length = 30)
	private String emotionCode;

	@Column(name = "memo", length = 2000)
	private String memo;

	@Column(name = "photo_url", length = 2000)
	private String photoUrl;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uploaded_asset_id")
	private UploadedAssetEntity uploadedAsset;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected CardEntryEntity() {
	}

	public CardEntryEntity(DayCardEntity dayCard, UserEntity user) {
		this.dayCard = dayCard;
		this.user = user;
	}

	public void update(String emotionCode, String memo, String photoUrl, UploadedAssetEntity uploadedAsset) {
		this.emotionCode = emotionCode;
		this.memo = memo;
		this.photoUrl = photoUrl;
		this.uploadedAsset = uploadedAsset;
	}

	public boolean hasRecordedContent() {
		return this.emotionCode != null && !this.emotionCode.isBlank();
	}

	public UserEntity getUser() {
		return user;
	}

	public String getEmotionCode() {
		return emotionCode;
	}

	public String getMemo() {
		return memo;
	}

	public String getPhotoUrl() {
		return photoUrl;
	}

	public UploadedAssetEntity getUploadedAsset() {
		return uploadedAsset;
	}
}
