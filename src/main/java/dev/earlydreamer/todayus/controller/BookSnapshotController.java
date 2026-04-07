package dev.earlydreamer.todayus.controller;

import dev.earlydreamer.todayus.dto.books.CurrentBookSnapshotResponse;
import dev.earlydreamer.todayus.service.TodayUsContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Book Snapshots", description = "책 제작을 위한 현재 스냅샷 요약")
@RestController
@RequestMapping("/api/v1/book-snapshots")
public class BookSnapshotController {

	private final TodayUsContractService contractService;

	public BookSnapshotController(TodayUsContractService contractService) {
		this.contractService = contractService;
	}

	@Operation(
		summary = "현재 책 스냅샷 요약 조회",
		description = "책 진행도와 후보 기록, 스냅샷/주문 상태를 함께 내려줘요."
	)
	@ApiResponse(
		responseCode = "200",
		description = "책 탭에서 사용하는 현재 상태 요약이에요.",
		content = @Content(schema = @Schema(implementation = CurrentBookSnapshotResponse.class))
	)
	@GetMapping("/current")
	public CurrentBookSnapshotResponse getCurrentSnapshot() {
		return this.contractService.getCurrentBookSnapshot();
	}
}
