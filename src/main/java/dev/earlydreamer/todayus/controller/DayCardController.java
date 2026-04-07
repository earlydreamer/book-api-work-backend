package dev.earlydreamer.todayus.controller;

import dev.earlydreamer.todayus.dto.daycard.SaveDayCardEntryRequest;
import dev.earlydreamer.todayus.dto.daycard.SaveDayCardEntryResponse;
import dev.earlydreamer.todayus.dto.daycard.TodayCardResponse;
import dev.earlydreamer.todayus.service.TodayUsContractService;
import dev.earlydreamer.todayus.support.validation.ValidLocalDate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Day Cards", description = "하루 기록 카드 조회와 저장 API")
@RestController
@RequestMapping("/api/v1/day-cards")
public class DayCardController {

	private final TodayUsContractService contractService;

	public DayCardController(TodayUsContractService contractService) {
		this.contractService = contractService;
	}

	@Operation(
		summary = "오늘 카드 조회",
		description = "현재 연결 기준으로 오늘 날짜의 기록 카드를 조회해요."
	)
	@ApiResponse(
		responseCode = "200",
		description = "오늘 카드 응답이에요.",
		content = @Content(schema = @Schema(implementation = TodayCardResponse.class))
	)
	@GetMapping("/today")
	public TodayCardResponse getTodayCard() {
		return this.contractService.getTodayCard();
	}

	@Operation(
		summary = "하루 기록 저장 또는 수정",
		description = "특정 날짜에 내 감정과 메모, 사진 URL 또는 업로드 자산을 저장하거나 수정해요."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "저장된 오늘 카드와 책 진행도예요.",
			content = @Content(schema = @Schema(implementation = SaveDayCardEntryResponse.class))
		),
		@ApiResponse(responseCode = "400", description = "입력값이 올바르지 않아요.")
	})
	@PutMapping("/{localDate}/entry")
	public SaveDayCardEntryResponse saveEntry(
		@Parameter(
			description = "기록을 저장할 로컬 날짜예요. yyyy-MM-dd 형식을 사용해요.",
			example = "2026-04-07"
		)
		@PathVariable
		@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "localDate는 yyyy-MM-dd 형식이어야 해요.")
		@ValidLocalDate(message = "localDate는 실제 달력에 있는 날짜여야 해요.")
		String localDate,
		@Valid @RequestBody SaveDayCardEntryRequest request
	) {
		return this.contractService.saveTodayEntry(localDate, request);
	}
}
