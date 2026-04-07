package dev.earlydreamer.todayus.dto.common;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

public final class ContractTypes {

	private ContractTypes() {
	}

	@Schema(description = "현재 관계 상태")
	public enum RelationshipState {
		UNCONNECTED("unconnected"),
		INVITE_PENDING("invite-pending"),
		CONNECTED("connected");

		private final String value;

		RelationshipState(String value) {
			this.value = value;
		}

		@JsonValue
		public String value() {
			return this.value;
		}
	}

	@Schema(description = "오늘 카드 상태")
	public enum TodayCardState {
		EMPTY("empty"),
		MINE_ONLY("mine-only"),
		PARTNER_ONLY("partner-only"),
		COMPLETE("complete"),
		CLOSED("closed");

		private final String value;

		TodayCardState(String value) {
			this.value = value;
		}

		@JsonValue
		public String value() {
			return this.value;
		}
	}

	@Schema(description = "책 진행 상태")
	public enum BookState {
		GROWING("growing"),
		ELIGIBLE("eligible"),
		SNAPSHOT_BUILDING("snapshot-building"),
		READY_TO_ORDER("ready-to-order"),
		ORDERED("ordered");

		private final String value;

		BookState(String value) {
			this.value = value;
		}

		@JsonValue
		public String value() {
			return this.value;
		}
	}

	@Schema(description = "보관함 section 종류")
	public enum ArchiveSectionType {
		CURRENT("current"),
		ARCHIVED("archived");

		private final String value;

		ArchiveSectionType(String value) {
			this.value = value;
		}

		@JsonValue
		public String value() {
			return this.value;
		}
	}

	@Schema(description = "하루 기록 완성 상태")
	public enum RecordState {
		PARTIAL("partial"),
		COMPLETE("complete"),
		CLOSED("closed");

		private final String value;

		RecordState(String value) {
			this.value = value;
		}

		@JsonValue
		public String value() {
			return this.value;
		}
	}

	@Schema(description = "유효성 검증 필드 오류")
	public record FieldErrorResponse(
		@Schema(description = "오류가 난 필드 이름", example = "emotionCode")
		String field,
		@Schema(description = "필드 오류 메시지", example = "emotionCode는 비워둘 수 없어요.")
		String message
	) {
	}

	@Schema(description = "현재 관계 요약 정보")
	public record RelationshipSummaryResponse(
		@Schema(description = "현재 관계 상태")
		RelationshipState state,
		@Schema(description = "관계 인스턴스 ID", nullable = true, example = "cpl_active_20260407")
		String coupleId,
		@Schema(description = "내 이름", example = "지우")
		String myName,
		@Schema(description = "파트너 이름", nullable = true, example = "민준")
		String partnerName,
		@Schema(description = "연결 시작일", nullable = true, example = "2026-04-07")
		String startDate,
		@Schema(description = "초대 코드", nullable = true, example = "TODAY2026")
		String inviteCode
	) {
	}

	@Schema(description = "한 사람의 기록 엔트리")
	public record RecordEntryResponse(
		@Schema(description = "기록 작성자 이름", example = "지우")
		String author,
		@Schema(description = "감정 코드", example = "calm")
		String emotionCode,
		@Schema(description = "감정 이모지", example = "🌿")
		String emotionEmoji,
		@Schema(description = "감정 라벨", example = "차분해")
		String emotionLabel,
		@Schema(description = "메모", nullable = true, example = "새 관계에서 처음 남긴 기록이에요.")
		String memo,
		@Schema(description = "사진 URL", nullable = true, example = "https://example.com/photo.jpg")
		String photoUrl
	) {
	}

	@Schema(description = "하루 기록 카드 하나")
	public record MomentRecordResponse(
		@Schema(description = "기록 ID", example = "moment-cpl_active_20260407-2026-04-07")
		String id,
		@Schema(description = "기록 날짜", example = "2026-04-07")
		String localDate,
		@Schema(description = "화면용 날짜 라벨", example = "4월 7일 화")
		String dateLabel,
		@Schema(description = "기록 완성 상태")
		RecordState state,
		@Schema(description = "내 기록")
		RecordEntryResponse me,
		@Schema(description = "파트너 기록", nullable = true)
		RecordEntryResponse partner
	) {
	}

	@Schema(description = "책 진행도 요약")
	public record BookProgressResponse(
		@Schema(description = "최근 며칠을 기준으로 계산하는지", example = "30")
		int lookbackDays,
		@Schema(description = "책 제작 최소 필요 기록 수", example = "20")
		int requiredDays,
		@Schema(description = "현재 기록한 일수", example = "12")
		int recordedDays,
		@Schema(description = "남은 필요 일수", example = "8")
		int remainingDays,
		@Schema(description = "현재 책 상태")
		BookState state
	) {
	}
}
