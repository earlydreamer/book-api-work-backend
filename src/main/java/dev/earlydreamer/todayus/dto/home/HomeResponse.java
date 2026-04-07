package dev.earlydreamer.todayus.dto.home;

import dev.earlydreamer.todayus.dto.common.ContractTypes.BookProgressResponse;
import dev.earlydreamer.todayus.dto.common.ContractTypes.MomentRecordResponse;
import dev.earlydreamer.todayus.dto.common.ContractTypes.RelationshipSummaryResponse;
import dev.earlydreamer.todayus.dto.daycard.TodayCardResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "피드 홈 요약 응답")
public record HomeResponse(
	@Schema(description = "현재 관계 요약")
	RelationshipSummaryResponse relationship,
	@Schema(description = "오늘 카드")
	TodayCardResponse todayCard,
	@Schema(description = "최근 기록 목록")
	List<MomentRecordResponse> recentMoments,
	@Schema(description = "책 진행도")
	BookProgressResponse bookProgress
) {
}
