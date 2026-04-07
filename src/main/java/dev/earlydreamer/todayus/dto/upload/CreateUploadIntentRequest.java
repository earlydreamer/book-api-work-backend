package dev.earlydreamer.todayus.dto.upload;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "R2 direct upload intent 발급 요청")
public record CreateUploadIntentRequest(
	@NotBlank(message = "fileName은 비워둘 수 없어요.")
	@Schema(description = "원본 파일 이름이에요.", example = "photo.jpg")
	String fileName,
	@NotBlank(message = "contentType은 비워둘 수 없어요.")
	@Schema(description = "업로드할 파일의 MIME 타입이에요.", example = "image/jpeg")
	String contentType,
	@Min(value = 1L, message = "fileSize는 1바이트 이상이어야 해요.")
	@Schema(description = "파일 크기예요.", example = "231231")
	long fileSize
) {
}
