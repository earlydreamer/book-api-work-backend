package dev.earlydreamer.todayus.integration.sweetbook.dto;

import java.time.Instant;

public record FinalizeBookResult(
	int pageCount,
	Instant finalizedAt
) {
}
