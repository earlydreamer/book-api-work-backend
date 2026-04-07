package dev.earlydreamer.todayus.dto.couples;

import dev.earlydreamer.todayus.dto.common.ContractTypes.RelationshipSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "초대 코드 생성 응답")
public record CreateInviteResponse(RelationshipSummaryResponse relationship) {
}
