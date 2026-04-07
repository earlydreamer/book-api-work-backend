package dev.earlydreamer.todayus.dto.archive;

import dev.earlydreamer.todayus.dto.common.ContractTypes.ArchiveSectionType;
import dev.earlydreamer.todayus.dto.common.ContractTypes.MomentRecordResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "보관함 화면 응답")
public record ArchiveResponse(List<ArchiveSectionResponse> sections) {

	@Schema(description = "보관함 section 하나")
	public record ArchiveSectionResponse(
		@Schema(description = "section 구분값")
		ArchiveSectionType type,
		@Schema(description = "section 제목", example = "이전 연결 기록")
		String title,
		@Schema(description = "section 설명", example = "연결이 끝난 뒤에도 이전 기록은 보관함에 남아 있어요.")
		String description,
		@Schema(description = "section 안의 기록 수", example = "1")
		int count,
		@Schema(description = "기록 카드에 붙일 보조 라벨이에요.", nullable = true, example = "이전 파트너와의 기록")
		String recordLabel,
		@Schema(description = "section에 속한 기록 목록")
		List<MomentRecordResponse> records
	) {
	}
}
