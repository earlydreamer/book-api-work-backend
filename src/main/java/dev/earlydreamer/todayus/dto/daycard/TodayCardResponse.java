package dev.earlydreamer.todayus.dto.daycard;

import dev.earlydreamer.todayus.dto.common.ContractTypes.RecordEntryResponse;
import dev.earlydreamer.todayus.dto.common.ContractTypes.TodayCardState;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "하루 기록 카드 응답")
public record TodayCardResponse(
	@Schema(description = "로컬 날짜", example = "2026-04-07")
	String localDate,
	@Schema(description = "화면 표시용 날짜 라벨", example = "4월 7일 화")
	String dateLabel,
	@Schema(description = "오늘 카드 상태")
	TodayCardState state,
	@Schema(description = "내 기록")
	RecordEntryResponse me,
	@Schema(description = "파트너 기록", nullable = true)
	RecordEntryResponse partner
) {
}
