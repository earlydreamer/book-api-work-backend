package dev.earlydreamer.todayus.integration.sweetbook.dto;

public record CreateOrderCommand(
	String bookUid,
	int quantity,
	String recipientName,
	String recipientPhone,
	String postalCode,
	String address1,
	String address2,
	String shippingMemo,
	String externalRef,
	String externalUserId,
	String idempotencyKey
) {
}
