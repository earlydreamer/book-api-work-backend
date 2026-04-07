package dev.earlydreamer.todayus.dto.couples;

import dev.earlydreamer.todayus.support.validation.ValidLocalDate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "초대 코드 생성 요청")
public record CreateInviteRequest(
	@NotBlank(message = "startDate는 비워둘 수 없어요.")
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "startDate는 yyyy-MM-dd 형식이어야 해요.")
	@ValidLocalDate(message = "startDate는 실제 달력에 있는 날짜여야 해요.")
	@Schema(
		description = "연결을 시작한 날짜예요. yyyy-MM-dd 형식을 사용해요.",
		example = "2026-04-07",
		pattern = "^\\d{4}-\\d{2}-\\d{2}$"
	)
	String startDate
) {
}
