package dev.earlydreamer.todayus.controller;

import dev.earlydreamer.todayus.dto.webhooks.SweetbookWebhookAckResponse;
import dev.earlydreamer.todayus.service.SweetbookWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Webhooks", description = "Sweetbook webhook 수신")
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

	private final SweetbookWebhookService sweetbookWebhookService;

	public WebhookController(SweetbookWebhookService sweetbookWebhookService) {
		this.sweetbookWebhookService = sweetbookWebhookService;
	}

	@Operation(
		summary = "Sweetbook webhook 수신",
		description = "Sweetbook가 보내는 주문 상태 변경 webhook을 검증하고 저장해요."
	)
	@ApiResponse(
		responseCode = "200",
		description = "웹훅을 정상 수신했어요.",
		content = @Content(schema = @Schema(implementation = SweetbookWebhookAckResponse.class))
	)
	@PostMapping("/sweetbook")
	public SweetbookWebhookAckResponse handleSweetbookWebhook(
		@RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
		@RequestHeader(value = "X-Webhook-Timestamp", required = false) String timestamp,
		@RequestHeader(value = "X-Webhook-Event", required = false) String eventType,
		@RequestHeader(value = "X-Webhook-Delivery", required = false) String deliveryId,
		@RequestBody String rawPayload
	) {
		return sweetbookWebhookService.handle(signature, timestamp, eventType, deliveryId, rawPayload);
	}
}
