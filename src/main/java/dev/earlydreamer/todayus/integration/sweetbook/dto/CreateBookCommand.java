package dev.earlydreamer.todayus.integration.sweetbook.dto;

public record CreateBookCommand(
	String title,
	String bookSpecUid,
	String idempotencyKey
) {
}
