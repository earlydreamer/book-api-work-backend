package dev.earlydreamer.todayus.dto.orders;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "주문 상세 응답")
public record OrderResponse(
	@Schema(description = "내부 주문 ID예요.", example = "1")
	Long orderId,
	@Schema(description = "연결된 스냅샷 ID예요.", example = "1")
	Long snapshotId,
	@Schema(description = "주문 상태예요.", example = "submitted")
	String status,
	@Schema(description = "Sweetbook 주문 UID예요.", example = "or_3eAx1IQiGByu", nullable = true)
	String sweetbookOrderUid,
	@Schema(description = "Sweetbook 주문 상태 코드예요.", example = "20", nullable = true)
	Integer sweetbookOrderStatusCode,
	@Schema(description = "Sweetbook 주문 상태 표시값이에요.", example = "결제완료", nullable = true)
	String sweetbookOrderStatusDisplay,
	@Schema(description = "수령인 이름이에요.", example = "김지우")
	String recipientName,
	@Schema(description = "수령인 연락처예요.", example = "010-1234-5678")
	String recipientPhone,
	@Schema(description = "우편번호예요.", example = "06101")
	String postalCode,
	@Schema(description = "기본 배송지예요.", example = "서울시 강남구 테헤란로 123")
	String address1,
	@Schema(description = "상세 주소예요.", example = "4층 401호", nullable = true)
	String address2,
	@Schema(description = "배송 메모예요.", example = "부재 시 경비실에 맡겨주세요.", nullable = true)
	String shippingMemo,
	@Schema(description = "택배사 코드예요.", example = "CJ", nullable = true)
	String trackingCarrier,
	@Schema(description = "운송장 번호예요.", example = "1234567890123", nullable = true)
	String trackingNumber,
	@Schema(description = "주문 요청 시각이에요.", example = "2026-04-07T00:10:00Z")
	String requestedAt,
	@Schema(description = "Sweetbook 제출 시각이에요.", example = "2026-04-07T00:10:00Z", nullable = true)
	String submittedAt,
	@Schema(description = "주문 종료 시각이에요.", example = "2026-04-10T03:00:00Z", nullable = true)
	String completedAt
) {
}
