package dev.earlydreamer.todayus.controller;

import dev.earlydreamer.todayus.dto.archive.ArchiveResponse;
import dev.earlydreamer.todayus.service.TodayUsContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Archive", description = "현재 연결 기록과 이전 연결 기록 보관함")
@RestController
@RequestMapping("/api/v1/archive")
public class ArchiveController {

	private final TodayUsContractService contractService;

	public ArchiveController(TodayUsContractService contractService) {
		this.contractService = contractService;
	}

	@Operation(
		summary = "보관함 조회",
		description = "현재 연결 기록과 이전 연결 기록을 section 단위로 구분해 내려줘요."
	)
	@ApiResponse(
		responseCode = "200",
		description = "보관함 화면 전용 section 응답이에요.",
		content = @Content(schema = @Schema(implementation = ArchiveResponse.class))
	)
	@GetMapping
	public ArchiveResponse getArchive() {
		return this.contractService.getArchive();
	}
}
