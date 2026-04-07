package dev.earlydreamer.todayus.service;

import dev.earlydreamer.todayus.dto.books.CurrentOrderSummaryResponse;
import dev.earlydreamer.todayus.dto.orders.CreateOrderRequest;
import dev.earlydreamer.todayus.dto.orders.OrderResponse;
import dev.earlydreamer.todayus.entity.BookSnapshotEntity;
import dev.earlydreamer.todayus.entity.BookSnapshotStatus;
import dev.earlydreamer.todayus.entity.CoupleEntity;
import dev.earlydreamer.todayus.entity.OrderEntity;
import dev.earlydreamer.todayus.entity.OrderStatus;
import dev.earlydreamer.todayus.entity.SweetbookBookEntity;
import dev.earlydreamer.todayus.entity.SweetbookBookStatus;
import dev.earlydreamer.todayus.entity.UserEntity;
import dev.earlydreamer.todayus.integration.sweetbook.SweetbookClient;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateOrderCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateOrderResult;
import dev.earlydreamer.todayus.repository.BookSnapshotRepository;
import dev.earlydreamer.todayus.repository.OrderRepository;
import dev.earlydreamer.todayus.repository.SweetbookBookRepository;
import dev.earlydreamer.todayus.support.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

	private final OrderRepository orderRepository;
	private final BookSnapshotRepository bookSnapshotRepository;
	private final SweetbookBookRepository sweetbookBookRepository;
	private final SweetbookClient sweetbookClient;
	private final Clock clock;

	public OrderService(
		OrderRepository orderRepository,
		BookSnapshotRepository bookSnapshotRepository,
		SweetbookBookRepository sweetbookBookRepository,
		SweetbookClient sweetbookClient,
		Clock clock
	) {
		this.orderRepository = orderRepository;
		this.bookSnapshotRepository = bookSnapshotRepository;
		this.sweetbookBookRepository = sweetbookBookRepository;
		this.sweetbookClient = sweetbookClient;
		this.clock = clock;
	}

	@Transactional(noRollbackFor = ApiException.class)
	public OrderResponse createOrder(UserEntity currentUser, CreateOrderRequest request) {
		BookSnapshotEntity snapshot = bookSnapshotRepository.findById(request.snapshotId())
			.orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND,
				"snapshot_not_found",
				"책 스냅샷을 찾을 수 없어요.",
				"존재하지 않는 스냅샷이에요."
			));
		requireParticipant(snapshot.getCouple(), currentUser.getId());
		if (snapshot.getCouple().getStatus() != dev.earlydreamer.todayus.entity.CoupleStatus.ACTIVE) {
			throw new ApiException(
				HttpStatus.CONFLICT,
				"relationship_not_active",
				"현재 활성 관계에서만 주문할 수 있어요.",
				"연결이 유지된 상태인지 먼저 확인해 주세요."
			);
		}
		if (orderRepository.existsBySnapshot_IdAndStatusIn(
			snapshot.getId(),
			EnumSet.of(
				OrderStatus.REQUESTED,
				OrderStatus.SUBMITTED,
				OrderStatus.CONFIRMED,
				OrderStatus.IN_PRODUCTION,
				OrderStatus.PRODUCTION_COMPLETE,
				OrderStatus.SHIPPED,
				OrderStatus.DELIVERED
			)
		)) {
			throw new ApiException(
				HttpStatus.CONFLICT,
				"order_already_exists",
				"이미 진행 중인 주문이 있어요.",
				"현재 주문 상태를 먼저 확인해 주세요."
			);
		}
		if (snapshot.getStatus() != BookSnapshotStatus.READY_TO_ORDER) {
			throw new ApiException(
				HttpStatus.CONFLICT,
				"snapshot_not_ready",
				"아직 주문 가능한 스냅샷이 아니에요.",
				"책 빌드가 끝난 뒤 다시 시도해 주세요."
			);
		}

		SweetbookBookEntity sweetbookBook = sweetbookBookRepository.findBySnapshot_Id(snapshot.getId())
			.orElseThrow(() -> new ApiException(
				HttpStatus.CONFLICT,
				"sweetbook_book_not_found",
				"Sweetbook 책 정보를 찾을 수 없어요.",
				"스냅샷 빌드가 정상적으로 끝났는지 확인해 주세요."
			));
		if (sweetbookBook.getStatus() != SweetbookBookStatus.FINALIZED || sweetbookBook.getSweetbookBookUid() == null) {
			throw new ApiException(
				HttpStatus.CONFLICT,
				"sweetbook_book_not_finalized",
				"아직 주문 가능한 책이 아니에요.",
				"책 제작이 끝난 뒤 다시 시도해 주세요."
			);
		}

		OrderEntity order = orderRepository.save(new OrderEntity(
			snapshot,
			currentUser,
			request.recipientName(),
			request.recipientPhone(),
			request.postalCode(),
			request.address1(),
			request.address2(),
			request.shippingMemo(),
			Instant.now(clock)
		));

		try {
			CreateOrderResult createOrderResult = sweetbookClient.createOrder(new CreateOrderCommand(
				sweetbookBook.getSweetbookBookUid(),
				1,
				request.recipientName(),
				request.recipientPhone(),
				request.postalCode(),
				request.address1(),
				request.address2(),
				request.shippingMemo(),
				"todayus-order-%d".formatted(order.getId()),
				currentUser.getId(),
				"todayus-order-%d".formatted(order.getId())
			));
			order.markSubmitted(
				createOrderResult.orderUid(),
				createOrderResult.orderStatusCode(),
				createOrderResult.orderStatusDisplay(),
				createOrderResult.orderedAt()
			);
			snapshot.markOrdered();
			return toResponse(order);
		} catch (RuntimeException exception) {
			order.markFailed("order_submission_failed", exception.getMessage());
			throw new ApiException(
				HttpStatus.BAD_GATEWAY,
				"order_submission_failed",
				"주문을 접수하지 못했어요.",
				"잠시 뒤 다시 시도해 주세요."
			);
		}
	}

	public OrderResponse getOrder(Long orderId, String currentUserId) {
		OrderEntity order = orderRepository.findById(orderId)
			.orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND,
				"order_not_found",
				"주문을 찾을 수 없어요.",
				"존재하지 않는 주문이에요."
			));
		requireParticipant(order.getSnapshot().getCouple(), currentUserId);
		return toResponse(order);
	}

	public Optional<CurrentOrderSummaryResponse> findLatestSummaryBySnapshotId(Long snapshotId) {
		return orderRepository.findTopBySnapshot_IdOrderByCreatedAtDesc(snapshotId)
			.map((order) -> new CurrentOrderSummaryResponse(
				order.getId(),
				order.getStatus().value(),
				order.getCreatedAt().toString()
			));
	}

	private void requireParticipant(CoupleEntity couple, String currentUserId) {
		if (!couple.hasParticipant(currentUserId)) {
			throw new ApiException(
				HttpStatus.FORBIDDEN,
				"order_access_denied",
				"이 주문에 접근할 수 없어요.",
				"현재 사용자와 연결된 주문인지 확인해 주세요."
			);
		}
	}

	private OrderResponse toResponse(OrderEntity order) {
		return new OrderResponse(
			order.getId(),
			order.getSnapshot().getId(),
			order.getStatus().value(),
			order.getSweetbookOrderUid(),
			order.getSweetbookOrderStatusCode(),
			order.getSweetbookOrderStatusDisplay(),
			order.getRecipientName(),
			order.getRecipientPhone(),
			order.getPostalCode(),
			order.getAddress1(),
			order.getAddress2(),
			order.getShippingMemo(),
			order.getTrackingCarrier(),
			order.getTrackingNumber(),
			order.getRequestedAt().toString(),
			order.getSubmittedAt() != null ? order.getSubmittedAt().toString() : null,
			order.getCompletedAt() != null ? order.getCompletedAt().toString() : null
		);
	}
}
