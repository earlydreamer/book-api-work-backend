package dev.earlydreamer.todayus.dto.books;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 스냅샷에 연결된 최신 주문 요약")
public record CurrentOrderSummaryResponse(
	@Schema(description = "주문 ID예요.", example = "1")
	Long orderId,
	@Schema(description = "주문 상태예요.", example = "requested")
	String status,
	@Schema(description = "주문 생성 시각이에요.", example = "2026-04-07T00:10:00Z")
	String createdAt
) {
}
