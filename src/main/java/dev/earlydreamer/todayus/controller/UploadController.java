package dev.earlydreamer.todayus.controller;

import dev.earlydreamer.todayus.dto.upload.CompleteUploadRequest;
import dev.earlydreamer.todayus.dto.upload.CompleteUploadResponse;
import dev.earlydreamer.todayus.dto.upload.CreateUploadIntentRequest;
import dev.earlydreamer.todayus.dto.upload.CreateUploadIntentResponse;
import dev.earlydreamer.todayus.service.UploadIntentService;
import dev.earlydreamer.todayus.service.UploadedAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Uploads", description = "R2 direct upload intent와 업로드 완료 등록 API")
@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

	private final UploadIntentService uploadIntentService;
	private final UploadedAssetService uploadedAssetService;

	public UploadController(UploadIntentService uploadIntentService, UploadedAssetService uploadedAssetService) {
		this.uploadIntentService = uploadIntentService;
		this.uploadedAssetService = uploadedAssetService;
	}

	@Operation(
		summary = "업로드 intent 발급",
		description = "브라우저가 R2에 직접 PUT 업로드할 수 있도록 presigned intent를 발급해요."
	)
	@ApiResponse(
		responseCode = "200",
		description = "업로드 intent 응답이에요.",
		content = @Content(schema = @Schema(implementation = CreateUploadIntentResponse.class))
	)
	@PostMapping("/intents")
	public CreateUploadIntentResponse createIntent(@Valid @RequestBody CreateUploadIntentRequest request) {
		return uploadIntentService.createIntent(request);
	}

	@Operation(
		summary = "업로드 완료 등록",
		description = "브라우저 업로드가 끝난 뒤 업로드 자산 상태를 완료로 바꿔요."
	)
	@ApiResponse(
		responseCode = "200",
		description = "업로드 완료 등록 응답이에요.",
		content = @Content(schema = @Schema(implementation = CompleteUploadResponse.class))
	)
	@PostMapping("/complete")
	public CompleteUploadResponse completeUpload(@Valid @RequestBody CompleteUploadRequest request) {
		return uploadedAssetService.completeUpload(request);
	}
}
