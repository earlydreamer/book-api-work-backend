package dev.earlydreamer.todayus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.earlydreamer.todayus.config.SweetbookProperties;
import dev.earlydreamer.todayus.dto.webhooks.SweetbookWebhookAckResponse;
import dev.earlydreamer.todayus.entity.OrderEntity;
import dev.earlydreamer.todayus.entity.OrderEventEntity;
import dev.earlydreamer.todayus.entity.OrderStatus;
import dev.earlydreamer.todayus.repository.OrderEventRepository;
import dev.earlydreamer.todayus.repository.OrderRepository;
import dev.earlydreamer.todayus.support.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SweetbookWebhookService {

	private final OrderRepository orderRepository;
	private final OrderEventRepository orderEventRepository;
	private final SweetbookProperties sweetbookProperties;
	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	public SweetbookWebhookService(
		OrderRepository orderRepository,
		OrderEventRepository orderEventRepository,
		SweetbookProperties sweetbookProperties
	) {
		this.orderRepository = orderRepository;
		this.orderEventRepository = orderEventRepository;
		this.sweetbookProperties = sweetbookProperties;
	}

	public SweetbookWebhookAckResponse handle(
		String signature,
		String timestamp,
		String eventType,
		String deliveryId,
		String rawPayload
	) {
		requireHeaders(timestamp, eventType, deliveryId);
		verifySignature(signature, timestamp, rawPayload);
		if (orderEventRepository.existsByDedupeKey(deliveryId)) {
			return new SweetbookWebhookAckResponse(true, true);
		}

		JsonNode payload = readPayload(rawPayload);
		OrderEntity order = orderRepository.findBySweetbookOrderUid(payload.path("orderUid").asText()).orElse(null);
		orderEventRepository.save(new OrderEventEntity(order, eventType, deliveryId, rawPayload));
		if (order != null) {
			applyEvent(order, eventType, payload);
		}
		return new SweetbookWebhookAckResponse(true, false);
	}

	private void applyEvent(OrderEntity order, String eventType, JsonNode payload) {
		String status = payload.path("status").asText("");
		OrderStatus nextStatus = toOrderStatus(eventType);
		if (nextStatus != null && shouldIgnoreTransition(order.getStatus(), nextStatus)) {
			order.overwriteExternalStatus(order.getSweetbookOrderStatusCode(), status);
			return;
		}
		switch (eventType) {
			case "order.created", "order.restored" -> order.markSubmitted(
				order.getSweetbookOrderUid(),
				order.getSweetbookOrderStatusCode() == null ? 20 : order.getSweetbookOrderStatusCode(),
				status,
				order.getSubmittedAt() != null ? order.getSubmittedAt() : Instant.parse(payload.path("timestamp").asText())
			);
			case "production.confirmed" -> order.markConfirmed(status);
			case "production.started" -> order.markInProduction(status);
			case "production.completed" -> order.markProductionComplete(status);
			case "shipping.departed" -> order.markShipped(
				status,
				payload.path("trackingCarrier").asText(null),
				payload.path("trackingNumber").asText(null)
			);
			case "shipping.delivered" -> order.markDelivered(status, Instant.parse(payload.path("deliveredAt").asText()));
			case "order.cancelled" -> {
				order.markCanceled(status, Instant.parse(payload.path("cancelledAt").asText()));
				order.getSnapshot().reopenForOrder();
			}
			default -> order.overwriteExternalStatus(order.getSweetbookOrderStatusCode(), status);
		}
	}

	private void requireHeaders(String timestamp, String eventType, String deliveryId) {
		if (timestamp == null || timestamp.isBlank() || eventType == null || eventType.isBlank() || deliveryId == null || deliveryId.isBlank()) {
			throw new ApiException(
				HttpStatus.BAD_REQUEST,
				"invalid_webhook_headers",
				"웹훅 헤더가 올바르지 않아요.",
				"timestamp, event, delivery 헤더를 다시 확인해 주세요."
			);
		}
	}

	private void verifySignature(String signature, String timestamp, String rawPayload) {
		if (signature == null || timestamp == null || signature.isBlank() || timestamp.isBlank()) {
			throw invalidSignature();
		}
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(sweetbookProperties.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] digest = mac.doFinal((timestamp + "." + rawPayload).getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte value : digest) {
				hex.append(String.format("%02x", value));
			}
			String expected = "sha256=" + hex;
			if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
				throw invalidSignature();
			}
		} catch (ApiException exception) {
			throw exception;
		} catch (Exception exception) {
			throw invalidSignature();
		}
	}

	private JsonNode readPayload(String rawPayload) {
		try {
			return objectMapper.readTree(rawPayload);
		} catch (Exception exception) {
			throw new ApiException(
				HttpStatus.BAD_REQUEST,
				"invalid_webhook_payload",
				"웹훅 payload를 읽을 수 없어요.",
				"Sweetbook가 보낸 JSON 본문을 해석하지 못했어요."
			);
		}
	}

	private ApiException invalidSignature() {
		return new ApiException(
			HttpStatus.UNAUTHORIZED,
			"invalid_webhook_signature",
			"유효하지 않은 웹훅 서명이에요.",
			"Sweetbook webhook secret과 서명 헤더를 다시 확인해 주세요."
		);
	}

	private OrderStatus toOrderStatus(String eventType) {
		return switch (eventType) {
			case "order.created", "order.restored" -> OrderStatus.SUBMITTED;
			case "production.confirmed" -> OrderStatus.CONFIRMED;
			case "production.started" -> OrderStatus.IN_PRODUCTION;
			case "production.completed" -> OrderStatus.PRODUCTION_COMPLETE;
			case "shipping.departed" -> OrderStatus.SHIPPED;
			case "shipping.delivered" -> OrderStatus.DELIVERED;
			case "order.cancelled" -> OrderStatus.CANCELED;
			default -> null;
		};
	}

	private boolean shouldIgnoreTransition(OrderStatus currentStatus, OrderStatus nextStatus) {
		if (nextStatus == OrderStatus.CANCELED) {
			return false;
		}
		return statusRank(nextStatus) < statusRank(currentStatus);
	}

	private int statusRank(OrderStatus status) {
		return switch (status) {
			case REQUESTED -> 10;
			case SUBMITTED -> 20;
			case CONFIRMED -> 30;
			case IN_PRODUCTION -> 40;
			case PRODUCTION_COMPLETE -> 50;
			case SHIPPED -> 60;
			case DELIVERED -> 70;
			case FAILED -> 80;
			case CANCELED -> 90;
		};
	}
}
