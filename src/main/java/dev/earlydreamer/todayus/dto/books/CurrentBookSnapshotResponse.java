package dev.earlydreamer.todayus.dto.books;

import dev.earlydreamer.todayus.dto.common.ContractTypes.BookProgressResponse;
import dev.earlydreamer.todayus.dto.common.ContractTypes.MomentRecordResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "현재 책 스냅샷 요약 응답")
public record CurrentBookSnapshotResponse(
	@Schema(description = "현재 책 진행도")
	BookProgressResponse bookProgress,
	@Schema(description = "책에 넣을 수 있는 후보 기록 목록")
	List<MomentRecordResponse> candidateMoments,
	@Schema(description = "생성된 스냅샷이 있으면 내려와요.", nullable = true)
	BookSnapshotSummaryResponse snapshot,
	@Schema(description = "주문 정보가 있으면 내려와요.", nullable = true)
	CurrentOrderSummaryResponse order
) {
}
