package dev.earlydreamer.todayus.dto.couples;

import dev.earlydreamer.todayus.dto.common.ContractTypes.BookProgressResponse;
import dev.earlydreamer.todayus.dto.common.ContractTypes.RelationshipSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 연결 종료 응답")
public record UnlinkCurrentCoupleResponse(
	@Schema(description = "연결 종료 후 관계 상태")
	RelationshipSummaryResponse relationship,
	@Schema(description = "보관함 반영 여부", example = "true")
	boolean archiveUpdated,
	@Schema(description = "초기화된 책 진행도")
	BookProgressResponse bookProgress
) {
}
