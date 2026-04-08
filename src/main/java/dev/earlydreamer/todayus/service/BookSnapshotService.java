package dev.earlydreamer.todayus.service;

import dev.earlydreamer.todayus.entity.BookSnapshotEntity;
import dev.earlydreamer.todayus.entity.BookSnapshotItemEntity;
import dev.earlydreamer.todayus.entity.BookSnapshotStatus;
import dev.earlydreamer.todayus.entity.CardEntryEntity;
import dev.earlydreamer.todayus.entity.CoupleEntity;
import dev.earlydreamer.todayus.entity.DayCardEntity;
import dev.earlydreamer.todayus.entity.UploadedAssetEntity;
import dev.earlydreamer.todayus.entity.UserEntity;
import dev.earlydreamer.todayus.repository.BookSnapshotRepository;
import dev.earlydreamer.todayus.support.error.ApiException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookSnapshotService {

	private static final int BOOK_LOOKBACK_DAYS = 30;
	private static final int BOOK_REQUIRED_DAYS = 20;

	private final BookSnapshotRepository bookSnapshotRepository;
	private final Clock clock;

	public BookSnapshotService(
		BookSnapshotRepository bookSnapshotRepository,
		Clock clock
	) {
		this.bookSnapshotRepository = bookSnapshotRepository;
		this.clock = clock;
	}

	public Optional<BookSnapshotEntity> findLatestByCoupleId(String coupleId) {
		return bookSnapshotRepository.findTopByCouple_IdOrderByCreatedAtDesc(coupleId);
	}

	@Transactional
	public BookSnapshotEntity createCurrentSnapshot(CoupleEntity activeCouple, UserEntity currentUser, List<DayCardEntity> recentCards) {
		List<DayCardEntity> recordedCards = recentCards.stream()
			.filter(this::hasRecordedEntries)
			.sorted(Comparator.comparing(DayCardEntity::getLocalDate))
			.toList();

		if (recordedCards.size() < BOOK_REQUIRED_DAYS) {
			throw new ApiException(
				HttpStatus.CONFLICT,
				"snapshot_ineligible",
				"아직 책 스냅샷을 만들 수 없어요.",
				"최근 30일 안에 20일 이상 기록이 쌓인 뒤에만 책 스냅샷을 만들 수 있어요."
			);
		}

		if (bookSnapshotRepository.existsByCouple_IdAndStatusIn(
			activeCouple.getId(),
			EnumSet.of(BookSnapshotStatus.SNAPSHOT_BUILDING, BookSnapshotStatus.READY_TO_ORDER, BookSnapshotStatus.ORDERED)
		)) {
			throw new ApiException(
				HttpStatus.CONFLICT,
				"snapshot_already_exists",
				"이미 진행 중인 책 스냅샷이 있어요.",
				"현재 스냅샷이나 주문이 끝난 뒤에 새 스냅샷을 만들 수 있어요."
			);
		}

		LocalDate today = LocalDate.now(clock);
		BookSnapshotEntity snapshot = new BookSnapshotEntity(
			activeCouple,
			BookSnapshotStatus.SNAPSHOT_BUILDING,
			today.minusDays(BOOK_LOOKBACK_DAYS - 1L),
			today,
			recordedCards.size(),
			recordedCards.size(),
			clock.instant()
		);

		for (int index = 0; index < recordedCards.size(); index++) {
			DayCardEntity dayCard = recordedCards.get(index);
			CardEntryEntity myEntry = dayCard.findEntry(currentUser.getId()).orElse(null);
			UserEntity counterpart = activeCouple.getCounterpart(currentUser.getId());
			CardEntryEntity partnerEntry = counterpart == null ? null : dayCard.findEntry(counterpart.getId()).orElse(null);

			snapshot.addItem(new BookSnapshotItemEntity(
				snapshot,
				dayCard,
				dayCard.getLocalDate(),
				writeEntryJson(myEntry),
				writeEntryJson(partnerEntry),
				myEntry != null ? myEntry.getMemo() : null,
				partnerEntry != null ? partnerEntry.getMemo() : null,
				currentUser.getDisplayName(),
				counterpart != null ? counterpart.getDisplayName() : null,
				firstPhotoAsset(myEntry, partnerEntry),
				index + 1
			));
		}

		return bookSnapshotRepository.save(snapshot);
	}

	private boolean hasRecordedEntries(DayCardEntity dayCard) {
		return dayCard.getEntries().stream().anyMatch(CardEntryEntity::hasRecordedContent);
	}

	private String writeEntryJson(CardEntryEntity entry) {
		if (entry == null) {
			return null;
		}
		return """
			{"author":"%s","emotionCode":"%s","memo":%s,"photoUrl":%s}
			""".formatted(
			escapeJson(entry.getUser().getDisplayName()),
			escapeJson(entry.getEmotionCode()),
			toJsonValue(entry.getMemo()),
			toJsonValue(entry.getPhotoUrl())
		);
	}

	private UploadedAssetEntity firstPhotoAsset(CardEntryEntity myEntry, CardEntryEntity partnerEntry) {
		if (myEntry != null && myEntry.getUploadedAsset() != null) {
			return myEntry.getUploadedAsset();
		}
		if (partnerEntry != null && partnerEntry.getUploadedAsset() != null) {
			return partnerEntry.getUploadedAsset();
		}
		return null;
	}

	private String toJsonValue(String value) {
		return value == null ? "null" : "\"%s\"".formatted(escapeJson(value));
	}

	private String escapeJson(String value) {
		return value
			.replace("\\", "\\\\")
			.replace("\"", "\\\"");
	}
}
