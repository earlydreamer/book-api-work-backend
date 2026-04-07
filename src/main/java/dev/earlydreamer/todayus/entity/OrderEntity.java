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
@Table(name = "orders")
public class OrderEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "snapshot_id", nullable = false)
	private BookSnapshotEntity snapshot;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ordering_user_id", nullable = false)
	private UserEntity orderingUser;

	@Column(name = "recipient_name", nullable = false, length = 100)
	private String recipientName;

	@Column(name = "recipient_phone", nullable = false, length = 20)
	private String recipientPhone;

	@Column(name = "postal_code", nullable = false, length = 10)
	private String postalCode;

	@Column(name = "address1", nullable = false, length = 200)
	private String address1;

	@Column(name = "address2", length = 200)
	private String address2;

	@Column(name = "shipping_memo", length = 200)
	private String shippingMemo;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private OrderStatus status;

	@Column(name = "sweetbook_order_uid", unique = true, length = 100)
	private String sweetbookOrderUid;

	@Column(name = "sweetbook_order_status_code")
	private Integer sweetbookOrderStatusCode;

	@Column(name = "sweetbook_order_status_display", length = 100)
	private String sweetbookOrderStatusDisplay;

	@Column(name = "tracking_carrier", length = 30)
	private String trackingCarrier;

	@Column(name = "tracking_number", length = 100)
	private String trackingNumber;

	@Column(name = "requested_at", nullable = false)
	private Instant requestedAt;

	@Column(name = "submitted_at")
	private Instant submittedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

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

	protected OrderEntity() {
	}

	public OrderEntity(
		BookSnapshotEntity snapshot,
		UserEntity orderingUser,
		String recipientName,
		String recipientPhone,
		String postalCode,
		String address1,
		String address2,
		String shippingMemo,
		Instant requestedAt
	) {
		this.snapshot = snapshot;
		this.orderingUser = orderingUser;
		this.recipientName = recipientName;
		this.recipientPhone = recipientPhone;
		this.postalCode = postalCode;
		this.address1 = address1;
		this.address2 = address2;
		this.shippingMemo = shippingMemo;
		this.requestedAt = requestedAt;
		this.status = OrderStatus.REQUESTED;
	}

	public void markSubmitted(String sweetbookOrderUid, Integer statusCode, String statusDisplay, Instant submittedAt) {
		this.sweetbookOrderUid = sweetbookOrderUid;
		this.sweetbookOrderStatusCode = statusCode;
		this.sweetbookOrderStatusDisplay = statusDisplay;
		this.submittedAt = submittedAt;
		this.status = OrderStatus.SUBMITTED;
		this.failureCode = null;
		this.failureMessage = null;
	}

	public void markConfirmed(String statusDisplay) {
		this.status = OrderStatus.CONFIRMED;
		this.sweetbookOrderStatusDisplay = statusDisplay;
	}

	public void markInProduction(String statusDisplay) {
		this.status = OrderStatus.IN_PRODUCTION;
		this.sweetbookOrderStatusDisplay = statusDisplay;
	}

	public void markProductionComplete(String statusDisplay) {
		this.status = OrderStatus.PRODUCTION_COMPLETE;
		this.sweetbookOrderStatusDisplay = statusDisplay;
	}

	public void markShipped(String statusDisplay, String trackingCarrier, String trackingNumber) {
		this.status = OrderStatus.SHIPPED;
		this.sweetbookOrderStatusDisplay = statusDisplay;
		this.trackingCarrier = trackingCarrier;
		this.trackingNumber = trackingNumber;
	}

	public void markDelivered(String statusDisplay, Instant completedAt) {
		this.status = OrderStatus.DELIVERED;
		this.sweetbookOrderStatusDisplay = statusDisplay;
		this.completedAt = completedAt;
	}

	public void markCanceled(String statusDisplay, Instant completedAt) {
		this.status = OrderStatus.CANCELED;
		this.sweetbookOrderStatusDisplay = statusDisplay;
		this.completedAt = completedAt;
	}

	public void markFailed(String failureCode, String failureMessage) {
		this.status = OrderStatus.FAILED;
		this.failureCode = failureCode;
		this.failureMessage = failureMessage;
		this.completedAt = Instant.now();
	}

	public void overwriteExternalStatus(Integer statusCode, String statusDisplay) {
		this.sweetbookOrderStatusCode = statusCode;
		this.sweetbookOrderStatusDisplay = statusDisplay;
	}

	public Long getId() {
		return id;
	}

	public BookSnapshotEntity getSnapshot() {
		return snapshot;
	}

	public UserEntity getOrderingUser() {
		return orderingUser;
	}

	public String getRecipientName() {
		return recipientName;
	}

	public String getRecipientPhone() {
		return recipientPhone;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getAddress1() {
		return address1;
	}

	public String getAddress2() {
		return address2;
	}

	public String getShippingMemo() {
		return shippingMemo;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public String getSweetbookOrderUid() {
		return sweetbookOrderUid;
	}

	public Integer getSweetbookOrderStatusCode() {
		return sweetbookOrderStatusCode;
	}

	public String getSweetbookOrderStatusDisplay() {
		return sweetbookOrderStatusDisplay;
	}

	public String getTrackingCarrier() {
		return trackingCarrier;
	}

	public String getTrackingNumber() {
		return trackingNumber;
	}

	public Instant getRequestedAt() {
		return requestedAt;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
