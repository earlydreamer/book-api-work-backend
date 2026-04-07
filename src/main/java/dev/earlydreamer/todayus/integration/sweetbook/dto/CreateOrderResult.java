package dev.earlydreamer.todayus.integration.sweetbook.dto;

import java.time.Instant;

public record CreateOrderResult(
	String orderUid,
	int orderStatusCode,
	String orderStatusDisplay,
	Instant orderedAt
) {
}
