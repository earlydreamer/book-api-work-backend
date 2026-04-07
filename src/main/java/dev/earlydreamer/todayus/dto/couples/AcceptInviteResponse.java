package dev.earlydreamer.todayus.dto.couples;

import dev.earlydreamer.todayus.dto.common.ContractTypes.BookProgressResponse;
import dev.earlydreamer.todayus.dto.common.ContractTypes.RelationshipSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "초대 수락 응답")
public record AcceptInviteResponse(
	@Schema(description = "수락 후 현재 관계 정보")
	RelationshipSummaryResponse relationship,
	@Schema(description = "초기화된 책 진행도")
	BookProgressResponse bookProgress
) {
}
