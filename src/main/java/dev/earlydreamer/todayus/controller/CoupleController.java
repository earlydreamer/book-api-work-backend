package dev.earlydreamer.todayus.controller;

import dev.earlydreamer.todayus.dto.couples.AcceptInviteResponse;
import dev.earlydreamer.todayus.dto.couples.CreateInviteRequest;
import dev.earlydreamer.todayus.dto.couples.CreateInviteResponse;
import dev.earlydreamer.todayus.dto.couples.InvitePreviewResponse;
import dev.earlydreamer.todayus.dto.couples.UnlinkCurrentCoupleResponse;
import dev.earlydreamer.todayus.service.TodayUsContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Couples", description = "연결 생성, 초대, 수락, 연결 종료 API")
@RestController
@RequestMapping("/api/v1/couples")
public class CoupleController {

	private final TodayUsContractService contractService;

	public CoupleController(TodayUsContractService contractService) {
		this.contractService = contractService;
	}

	@Operation(
		summary = "초대 코드 생성",
		description = "내 공간을 만들고 파트너에게 보낼 초대 코드를 발급해요."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "초대 코드 발급이 끝난 상태예요.",
			content = @Content(schema = @Schema(implementation = CreateInviteResponse.class))
		),
		@ApiResponse(responseCode = "400", description = "요청 형식이 올바르지 않아요.")
	})
	@PostMapping("/invites")
	public CreateInviteResponse createInvite(@Valid @RequestBody CreateInviteRequest request) {
		return this.contractService.createInvite(request);
	}

	@Operation(
		summary = "초대 코드 미리보기 조회",
		description = "초대 코드가 유효한지 확인하고 초대한 사람 이름과 시작일을 보여줘요."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "초대 코드가 유효해요.",
			content = @Content(schema = @Schema(implementation = InvitePreviewResponse.class))
		),
		@ApiResponse(responseCode = "404", description = "초대 코드를 찾을 수 없어요.")
	})
	@GetMapping("/invites/{inviteCode}")
	public InvitePreviewResponse getInvitePreview(
		@Parameter(description = "확인할 초대 코드예요.", example = "TODAY2026")
		@PathVariable @Size(min = 4, max = 20, message = "inviteCode 길이가 올바르지 않아요.") String inviteCode
	) {
		return this.contractService.getInvitePreview(inviteCode);
	}

	@Operation(
		summary = "초대 코드 수락",
		description = "초대 코드를 수락해 현재 연결을 활성화해요."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "연결이 활성화됐어요.",
			content = @Content(schema = @Schema(implementation = AcceptInviteResponse.class))
		),
		@ApiResponse(responseCode = "404", description = "초대 코드를 찾을 수 없어요.")
	})
	@PostMapping("/invites/{inviteCode}/accept")
	public AcceptInviteResponse acceptInvite(
		@Parameter(description = "수락할 초대 코드예요.", example = "TODAY2026")
		@PathVariable @Size(min = 4, max = 20, message = "inviteCode 길이가 올바르지 않아요.") String inviteCode
	) {
		return this.contractService.acceptInvite(inviteCode);
	}

	@Operation(
		summary = "현재 연결 종료",
		description = "현재 연결을 종료하고 기존 기록을 보관함으로 이동해요."
	)
	@ApiResponse(
		responseCode = "200",
		description = "연결 종료가 반영된 상태예요.",
		content = @Content(schema = @Schema(implementation = UnlinkCurrentCoupleResponse.class))
	)
	@PostMapping("/current/unlink")
	public UnlinkCurrentCoupleResponse unlinkCurrent() {
		return this.contractService.unlinkCurrent();
	}
}
