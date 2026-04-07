package dev.earlydreamer.todayus.dto.daycard;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "하루 기록 저장 요청")
public record SaveDayCardEntryRequest(
	@NotBlank(message = "emotionCode는 비워둘 수 없어요.")
	@Schema(description = "선택한 감정 코드예요.", example = "calm")
	String emotionCode,
	@Schema(description = "짧은 기록 메모예요.", example = "사진 한 장 없이도 남길 수 있어요.")
	String memo,
	@Schema(description = "기존 호환용 사진 URL이에요. uploadedAssetId가 있으면 이 값보다 자산 URL을 우선해요.", nullable = true, example = "https://example.com/photo.jpg")
	String photoUrl,
	@Schema(description = "R2에 업로드 완료된 자산 ID예요. 있으면 이 자산의 공개 URL을 기록에 연결해요.", nullable = true, example = "asset_abc123")
	String uploadedAssetId
) {
}
