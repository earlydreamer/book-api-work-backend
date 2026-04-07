package dev.earlydreamer.todayus.dto.upload;

import dev.earlydreamer.todayus.entity.UploadedAssetStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업로드 완료 등록 응답")
public record CompleteUploadResponse(
	@Schema(description = "업로드 자산 ID예요.", example = "asset_abc123")
	String assetId,
	@Schema(description = "업로드 자산 상태예요.")
	UploadedAssetStatus status,
	@Schema(description = "업로드된 자산의 공개 URL이에요.")
	String publicUrl
) {
}
