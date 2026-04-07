package dev.earlydreamer.todayus.controller;

import dev.earlydreamer.todayus.dto.home.HomeResponse;
import dev.earlydreamer.todayus.service.TodayUsContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home", description = "피드 홈과 현재 관계 요약 응답")
@RestController
@RequestMapping("/api/v1/me")
public class HomeController {

	private final TodayUsContractService contractService;

	public HomeController(TodayUsContractService contractService) {
		this.contractService = contractService;
	}

	@Operation(
		summary = "피드 홈 요약 조회",
		description = "현재 연결 상태, 오늘 카드, 최근 기록, 책 진행도를 한 번에 내려줘요."
	)
	@ApiResponse(
		responseCode = "200",
		description = "피드 화면에 필요한 요약 응답이에요.",
		content = @Content(schema = @Schema(implementation = HomeResponse.class))
	)
	@GetMapping("/home")
	public HomeResponse getHome() {
		return this.contractService.getHome();
	}
}
