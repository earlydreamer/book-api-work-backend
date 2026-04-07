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
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "couples")
public class CoupleEntity {

	@Id
	@Column(name = "id", nullable = false, length = 100)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "creator_user_id", nullable = false)
	private UserEntity creatorUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "partner_user_id")
	private UserEntity partnerUser;

	@Column(name = "invite_code", nullable = false, unique = true, length = 30)
	private String inviteCode;

	@Column(name = "anniversary_date", nullable = false)
	private LocalDate anniversaryDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private CoupleStatus status;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "accepted_at")
	private Instant acceptedAt;

	@Column(name = "unlinked_at")
	private Instant unlinkedAt;

	protected CoupleEntity() {
	}

	public static CoupleEntity invitePending(String id, UserEntity creatorUser, String inviteCode, LocalDate anniversaryDate) {
		CoupleEntity couple = new CoupleEntity();
		couple.id = id;
		couple.creatorUser = creatorUser;
		couple.inviteCode = inviteCode;
		couple.anniversaryDate = anniversaryDate;
		couple.status = CoupleStatus.INVITE_PENDING;
		return couple;
	}

	public void connect(UserEntity partnerUser, Instant acceptedAt) {
		this.partnerUser = partnerUser;
		this.acceptedAt = acceptedAt;
		this.status = CoupleStatus.ACTIVE;
	}

	public void unlink(Instant unlinkedAt) {
		this.status = CoupleStatus.UNLINKED;
		this.unlinkedAt = unlinkedAt;
	}

	public boolean hasParticipant(String userId) {
		return creatorUser.getId().equals(userId)
			|| (partnerUser != null && partnerUser.getId().equals(userId));
	}

	public UserEntity getCounterpart(String userId) {
		if (creatorUser.getId().equals(userId)) {
			return partnerUser;
		}
		return creatorUser;
	}

	public String getId() {
		return id;
	}

	public UserEntity getCreatorUser() {
		return creatorUser;
	}

	public UserEntity getPartnerUser() {
		return partnerUser;
	}

	public String getInviteCode() {
		return inviteCode;
	}

	public LocalDate getAnniversaryDate() {
		return anniversaryDate;
	}

	public CoupleStatus getStatus() {
		return status;
	}
}
