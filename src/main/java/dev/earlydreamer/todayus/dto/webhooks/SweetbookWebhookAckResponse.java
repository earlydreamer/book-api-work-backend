package dev.earlydreamer.todayus.dto.webhooks;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sweetbook webhook 수신 응답")
public record SweetbookWebhookAckResponse(
	@Schema(description = "웹훅을 정상 수신했는지 여부예요.", example = "true")
	boolean received,
	@Schema(description = "중복 delivery인지 여부예요.", example = "false")
	boolean duplicate
) {
}
