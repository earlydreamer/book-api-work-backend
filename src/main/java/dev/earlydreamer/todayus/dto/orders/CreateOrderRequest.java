package dev.earlydreamer.todayus.dto.orders;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "수동 주문 생성 요청")
public record CreateOrderRequest(
	@Schema(description = "주문할 스냅샷 ID예요.", example = "1")
	@NotNull(message = "snapshotId는 필수예요.")
	Long snapshotId,
	@Schema(description = "수령인 이름이에요.", example = "김지우")
	@NotBlank(message = "recipientName은 필수예요.")
	@Size(max = 100, message = "recipientName은 100자 이하로 입력해 주세요.")
	String recipientName,
	@Schema(description = "수령인 연락처예요.", example = "010-1234-5678")
	@NotBlank(message = "recipientPhone은 필수예요.")
	@Size(max = 20, message = "recipientPhone은 20자 이하로 입력해 주세요.")
	String recipientPhone,
	@Schema(description = "우편번호예요.", example = "06101")
	@NotBlank(message = "postalCode는 필수예요.")
	@Size(max = 10, message = "postalCode는 10자 이하로 입력해 주세요.")
	String postalCode,
	@Schema(description = "기본 배송지예요.", example = "서울시 강남구 테헤란로 123")
	@NotBlank(message = "address1은 필수예요.")
	@Size(max = 200, message = "address1은 200자 이하로 입력해 주세요.")
	String address1,
	@Schema(description = "상세 주소예요.", example = "4층 401호", nullable = true)
	@Size(max = 200, message = "address2는 200자 이하로 입력해 주세요.")
	String address2,
	@Schema(description = "배송 메모예요.", example = "부재 시 경비실에 맡겨주세요.", nullable = true)
	@Size(max = 200, message = "shippingMemo는 200자 이하로 입력해 주세요.")
	String shippingMemo
) {
}
