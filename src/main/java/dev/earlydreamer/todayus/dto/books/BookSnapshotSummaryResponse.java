package dev.earlydreamer.todayus.dto.books;

import dev.earlydreamer.todayus.entity.BookSnapshotStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 관계의 최근 책 스냅샷 요약")
public record BookSnapshotSummaryResponse(
	@Schema(description = "스냅샷 ID예요.", example = "1")
	Long snapshotId,
	@Schema(description = "스냅샷 상태예요.")
	BookSnapshotStatus status,
	@Schema(description = "최근 창 시작일이에요.", example = "2026-03-09")
	String windowStartDate,
	@Schema(description = "최근 창 종료일이에요.", example = "2026-04-07")
	String windowEndDate,
	@Schema(description = "최근 창에서 기록된 일수예요.", example = "20")
	int recordedDays,
	@Schema(description = "이번 스냅샷에 고정된 아이템 수예요.", example = "20")
	int selectedItemCount,
	@Schema(description = "빌드를 시작한 시각이에요.", example = "2026-04-07T00:00:00Z")
	String buildStartedAt,
	@Schema(description = "빌드를 끝낸 시각이에요.", nullable = true, example = "2026-04-07T00:10:00Z")
	String buildCompletedAt
) {
}
