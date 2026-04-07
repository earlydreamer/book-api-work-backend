package dev.earlydreamer.todayus.dto.couples;

import dev.earlydreamer.todayus.dto.common.ContractTypes.RelationshipState;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "초대 코드 미리보기 응답")
public record InvitePreviewResponse(
	@Schema(description = "확인한 초대 코드", example = "TODAY2026")
	String inviteCode,
	@Schema(description = "초대한 사람 이름", example = "민준")
	String inviterName,
	@Schema(description = "연결 시작일", example = "2026-04-07")
	String startDate,
	@Schema(description = "현재 초대 상태")
	RelationshipState status
) {
}
