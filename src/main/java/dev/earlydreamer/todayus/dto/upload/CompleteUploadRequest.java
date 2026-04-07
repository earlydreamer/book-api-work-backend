package dev.earlydreamer.todayus.dto.upload;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "업로드 완료 등록 요청")
public record CompleteUploadRequest(
	@NotBlank(message = "assetId는 비워둘 수 없어요.")
	@Schema(description = "업로드 완료 처리할 자산 ID예요.", example = "asset_abc123")
	String assetId
) {
}
