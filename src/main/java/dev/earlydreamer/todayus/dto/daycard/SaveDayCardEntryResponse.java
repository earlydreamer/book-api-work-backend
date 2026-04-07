package dev.earlydreamer.todayus.dto.daycard;

import dev.earlydreamer.todayus.dto.common.ContractTypes.BookProgressResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "하루 기록 저장 응답")
public record SaveDayCardEntryResponse(
	@Schema(description = "저장 후 오늘 카드 상태")
	TodayCardResponse todayCard,
	@Schema(description = "저장 후 책 진행도")
	BookProgressResponse bookProgress
) {
}
