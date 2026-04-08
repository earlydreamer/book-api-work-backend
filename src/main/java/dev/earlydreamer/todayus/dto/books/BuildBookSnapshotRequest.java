package dev.earlydreamer.todayus.dto.books;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "책 스냅샷 빌드 요청")
public record BuildBookSnapshotRequest(
	@Schema(description = "Sweetbook 책 사양 ID (bookSpecId)", example = "8x8_hard_standard")
	@NotBlank(message = "책 사양 ID는 필수예요.")
	String bookSpecId,

	@Schema(description = "표지 템플릿 ID", example = "cover_modern_01")
	@NotBlank(message = "표지 템플릿 ID는 필수예요.")
	String coverTemplateId,

	@Schema(description = "내지 템플릿 ID", example = "content_minimal_01")
	@NotBlank(message = "내지 템플릿 ID는 필수예요.")
	String contentTemplateId,

	@Schema(description = "간지 템플릿 ID (선택)", example = "interleaf_simple_01")
	String interleafTemplateId,

	@Schema(description = "발행 정보(Publishing) 템플릿 ID (선택)", example = "pub_info_standard")
	String publishingTemplateId
) {
}
