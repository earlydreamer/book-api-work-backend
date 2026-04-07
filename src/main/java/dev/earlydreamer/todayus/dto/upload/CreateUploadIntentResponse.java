package dev.earlydreamer.todayus.dto.upload;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "R2 direct upload intent 응답")
public record CreateUploadIntentResponse(
	@Schema(description = "업로드 자산 ID예요.", example = "asset_abc123")
	String assetId,
	@Schema(description = "R2 오브젝트 키예요.", example = "uploads/local-user-1/2026/04/07/asset_abc123-photo.jpg")
	String objectKey,
	@Schema(description = "브라우저가 직접 PUT 할 presigned URL이에요.")
	String uploadUrl,
	@Schema(description = "업로드 후 사용할 공개 URL이에요.")
	String publicUrl,
	@Schema(description = "presigned URL 만료 초예요.", example = "900")
	int expiresInSeconds
) {
}
