package dev.earlydreamer.todayus.controller;

import dev.earlydreamer.todayus.dto.orders.CreateOrderRequest;
import dev.earlydreamer.todayus.dto.orders.OrderResponse;
import dev.earlydreamer.todayus.service.CurrentUserProvider;
import dev.earlydreamer.todayus.service.OrderService;
import dev.earlydreamer.todayus.service.TodayUsContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Orders", description = "Sweetbook 주문 생성과 상태 조회")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

	private final OrderService orderService;
	private final TodayUsContractService contractService;
	private final CurrentUserProvider currentUserProvider;

	public OrderController(OrderService orderService, TodayUsContractService contractService, CurrentUserProvider currentUserProvider) {
		this.orderService = orderService;
		this.contractService = contractService;
		this.currentUserProvider = currentUserProvider;
	}

	@Operation(
		summary = "수동 주문 생성",
		description = "READY_TO_ORDER 스냅샷을 대상으로 Sweetbook sandbox 주문을 만들어요."
	)
	@ApiResponse(
		responseCode = "201",
		description = "주문이 생성됐어요.",
		content = @Content(schema = @Schema(implementation = OrderResponse.class))
	)
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
		return orderService.createOrder(contractService.getCurrentUserForRead(), request);
	}

	@Operation(
		summary = "주문 상세 조회",
		description = "현재 사용자와 연결된 주문 상세를 조회해요."
	)
	@ApiResponse(
		responseCode = "200",
		description = "주문 상세 응답이에요.",
		content = @Content(schema = @Schema(implementation = OrderResponse.class))
	)
	@GetMapping("/{orderId}")
	public OrderResponse getOrder(@PathVariable Long orderId) {
		return orderService.getOrder(orderId, currentUserProvider.getCurrentUser().authUserId());
	}
}
