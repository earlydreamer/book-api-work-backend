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

@Entity
@Table(name = "order_events")
public class OrderEventEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private OrderEntity order;

	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@Column(name = "dedupe_key", nullable = false, unique = true, length = 150)
	private String dedupeKey;

	@Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
	private String payloadJson;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected OrderEventEntity() {
	}

	public OrderEventEntity(OrderEntity order, String eventType, String dedupeKey, String payloadJson) {
		this.order = order;
		this.eventType = eventType;
		this.dedupeKey = dedupeKey;
		this.payloadJson = payloadJson;
	}
}
