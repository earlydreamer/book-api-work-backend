package dev.earlydreamer.todayus.service;

import dev.earlydreamer.todayus.dto.archive.ArchiveResponse;
import dev.earlydreamer.todayus.dto.books.CurrentBookSnapshotResponse;
import dev.earlydreamer.todayus.dto.common.ContractTypes.ArchiveSectionType;
import dev.earlydreamer.todayus.dto.common.ContractTypes.BookProgressResponse;
import dev.earlydreamer.todayus.dto.common.ContractTypes.BookState;
import dev.earlydreamer.todayus.dto.common.ContractTypes.MomentRecordResponse;
import dev.earlydreamer.todayus.dto.common.ContractTypes.RecordEntryResponse;
import dev.earlydreamer.todayus.dto.common.ContractTypes.RecordState;
import dev.earlydreamer.todayus.dto.common.ContractTypes.RelationshipState;
import dev.earlydreamer.todayus.dto.common.ContractTypes.RelationshipSummaryResponse;
import dev.earlydreamer.todayus.dto.common.ContractTypes.TodayCardState;
import dev.earlydreamer.todayus.dto.couples.AcceptInviteResponse;
import dev.earlydreamer.todayus.dto.couples.CreateInviteRequest;
import dev.earlydreamer.todayus.dto.couples.CreateInviteResponse;
import dev.earlydreamer.todayus.dto.couples.InvitePreviewResponse;
import dev.earlydreamer.todayus.dto.couples.UnlinkCurrentCoupleResponse;
import dev.earlydreamer.todayus.dto.daycard.SaveDayCardEntryRequest;
import dev.earlydreamer.todayus.dto.daycard.SaveDayCardEntryResponse;
import dev.earlydreamer.todayus.dto.daycard.TodayCardResponse;
import dev.earlydreamer.todayus.dto.home.HomeResponse;
import dev.earlydreamer.todayus.entity.CardEntryEntity;
import dev.earlydreamer.todayus.entity.CoupleEntity;
import dev.earlydreamer.todayus.entity.CoupleStatus;
import dev.earlydreamer.todayus.entity.DayCardEntity;
import dev.earlydreamer.todayus.entity.DayCardStatus;
import dev.earlydreamer.todayus.entity.UserEntity;
import dev.earlydreamer.todayus.entity.UserRole;
import dev.earlydreamer.todayus.repository.CoupleRepository;
import dev.earlydreamer.todayus.repository.DayCardRepository;
import dev.earlydreamer.todayus.repository.UserRepository;
import dev.earlydreamer.todayus.service.EmotionCatalog.EmotionView;
import dev.earlydreamer.todayus.support.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TodayUsContractService {

	private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("M월 d일 E", Locale.KOREAN);
	private static final int BOOK_LOOKBACK_DAYS = 30;
	private static final int BOOK_REQUIRED_DAYS = 20;

	private final UserRepository userRepository;
	private final CoupleRepository coupleRepository;
	private final DayCardRepository dayCardRepository;
	private final CurrentUserProvider currentUserProvider;
	private final EmotionCatalog emotionCatalog;
	private final Clock clock;

	public TodayUsContractService(
		UserRepository userRepository,
		CoupleRepository coupleRepository,
		DayCardRepository dayCardRepository,
		CurrentUserProvider currentUserProvider,
		EmotionCatalog emotionCatalog,
		Clock clock
	) {
		this.userRepository = userRepository;
		this.coupleRepository = coupleRepository;
		this.dayCardRepository = dayCardRepository;
		this.currentUserProvider = currentUserProvider;
		this.emotionCatalog = emotionCatalog;
		this.clock = clock;
	}

	@Transactional
	public HomeResponse getHome() {
		UserEntity currentUser = getOrCreateCurrentUser();
		CoupleEntity currentRelationship = findCurrentRelationship(currentUser.getId()).orElse(null);
		return new HomeResponse(
			relationshipSummary(currentRelationship, currentUser),
			currentRelationship == null ? emptyTodayCard(today()) : getTodayCard(currentRelationship, currentUser),
			getRecentMoments(currentRelationship, currentUser, 3),
			getBookProgress(currentRelationship)
		);
	}

	@Transactional
	public ArchiveResponse getArchive() {
		UserEntity currentUser = getOrCreateCurrentUser();
		CoupleEntity activeCouple = findActiveRelationship(currentUser.getId()).orElse(null);

		List<MomentRecordResponse> currentRecords = activeCouple == null
			? List.of()
			: getRecordedMoments(activeCouple, currentUser);

		List<MomentRecordResponse> archivedRecords = coupleRepository
			.findParticipantCouplesByStatus(currentUser.getId(), CoupleStatus.UNLINKED)
			.stream()
			.flatMap((couple) -> getRecordedMoments(couple, currentUser).stream())
			.sorted(Comparator.comparing(MomentRecordResponse::localDate).reversed())
			.collect(Collectors.toList());

		return new ArchiveResponse(List.of(
			new ArchiveResponse.ArchiveSectionResponse(
				ArchiveSectionType.CURRENT,
				"현재 연결 기록",
				"지금 함께 쌓고 있는 기록이에요.",
				currentRecords.size(),
				null,
				currentRecords
			),
			new ArchiveResponse.ArchiveSectionResponse(
				ArchiveSectionType.ARCHIVED,
				"이전 연결 기록",
				"연결이 끝난 뒤에도 이전 기록은 보관함에 남아 있어요.",
				archivedRecords.size(),
				"이전 파트너와의 기록",
				archivedRecords
			)
		));
	}

	@Transactional
	public CreateInviteResponse createInvite(CreateInviteRequest request) {
		UserEntity currentUser = getOrCreateCurrentUserForUpdate();
		if (hasOpenRelationship(currentUser.getId())) {
			throw new ApiException(
				HttpStatus.CONFLICT,
				"active_relationship_conflict",
				"이미 진행 중인 관계가 있어요.",
				"현재 관계를 종료한 뒤 새 초대 코드를 만들 수 있어요."
			);
		}

		CoupleEntity couple = CoupleEntity.invitePending(
			nextCoupleId(),
			currentUser,
			nextInviteCode(),
			LocalDate.parse(request.startDate())
		);
		CoupleEntity saved = coupleRepository.save(couple);
		return new CreateInviteResponse(relationshipSummary(saved, currentUser));
	}

	public InvitePreviewResponse getInvitePreview(String inviteCode) {
		CoupleEntity pendingCouple = getPendingInvite(inviteCode);
		return new InvitePreviewResponse(
			pendingCouple.getInviteCode(),
			pendingCouple.getCreatorUser().getDisplayName(),
			pendingCouple.getAnniversaryDate().toString(),
			RelationshipState.INVITE_PENDING
		);
	}

	@Transactional
	public AcceptInviteResponse acceptInvite(String inviteCode) {
		UserEntity currentUser = getOrCreateCurrentUserForUpdate();
		if (hasOpenRelationship(currentUser.getId())) {
			throw new ApiException(
				HttpStatus.CONFLICT,
				"active_relationship_conflict",
				"이미 진행 중인 관계가 있어요.",
				"현재 관계를 종료한 뒤 다른 초대를 수락할 수 있어요."
			);
		}

		CoupleEntity pendingCouple = getPendingInviteForUpdate(inviteCode);
		pendingCouple.connect(currentUser, Instant.now(clock));
		return new AcceptInviteResponse(
			relationshipSummary(pendingCouple, currentUser),
			bookProgress(0)
		);
	}

	@Transactional
	public UnlinkCurrentCoupleResponse unlinkCurrent() {
		UserEntity currentUser = getOrCreateCurrentUser();
		CoupleEntity activeCouple = findActiveRelationship(currentUser.getId())
			.orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND,
				"relationship_not_found",
				"현재 연결을 찾을 수 없어요.",
				"이미 연결이 종료됐거나 아직 연결되지 않았어요."
			));

		activeCouple.unlink(Instant.now(clock));
		return new UnlinkCurrentCoupleResponse(
			unconnectedRelationship(currentUser),
			true,
			bookProgress(0)
		);
	}

	@Transactional
	public TodayCardResponse getTodayCard() {
		UserEntity currentUser = getOrCreateCurrentUser();
		return findActiveRelationship(currentUser.getId())
			.map((couple) -> getTodayCard(couple, currentUser))
			.orElseGet(() -> emptyTodayCard(today()));
	}

	@Transactional
	public SaveDayCardEntryResponse saveTodayEntry(String localDate, SaveDayCardEntryRequest request) {
		UserEntity currentUser = getOrCreateCurrentUser();
		CoupleEntity activeCouple = findActiveRelationship(currentUser.getId())
			.orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND,
				"relationship_not_found",
				"현재 연결을 찾을 수 없어요.",
				"연결된 상대가 있을 때만 기록을 저장할 수 있어요."
			));

		LocalDate targetDate = LocalDate.parse(localDate);
		DayCardEntity dayCard = dayCardRepository.findByCouple_IdAndLocalDate(activeCouple.getId(), targetDate)
			.orElseGet(() -> new DayCardEntity(activeCouple, targetDate, closeAtUtc(targetDate)));

		dayCard.upsertEntry(currentUser, request.emotionCode(), request.memo(), request.photoUrl());
		DayCardEntity saved = dayCardRepository.save(dayCard);

		return new SaveDayCardEntryResponse(
			toTodayCard(saved, currentUser),
			getBookProgress(activeCouple)
		);
	}

	@Transactional
	public CurrentBookSnapshotResponse getCurrentBookSnapshot() {
		UserEntity currentUser = getOrCreateCurrentUser();
		return findActiveRelationship(currentUser.getId())
			.map((couple) -> {
				List<DayCardEntity> recentCards = getRecentWindowCards(couple.getId());
				List<MomentRecordResponse> candidateMoments = recentCards.stream()
					.filter((dayCard) -> dayCard.getState() == DayCardStatus.COMPLETE)
					.map((dayCard) -> toMomentRecord(dayCard, currentUser))
					.collect(Collectors.toList());
				return new CurrentBookSnapshotResponse(
					bookProgress(recordedDays(recentCards)),
					candidateMoments,
					null,
					null
				);
			})
			.orElseGet(() -> new CurrentBookSnapshotResponse(bookProgress(0), List.of(), null, null));
	}

	private UserEntity getOrCreateCurrentUser() {
		CurrentUserIdentity currentUserIdentity = currentUserProvider.getCurrentUser();
		return userRepository.findById(currentUserIdentity.authUserId())
			.orElseGet(() -> insertUserIfAbsentAndLoad(currentUserIdentity));
	}

	private UserEntity getOrCreateCurrentUserForUpdate() {
		CurrentUserIdentity currentUserIdentity = currentUserProvider.getCurrentUser();
		return userRepository.findByIdForUpdate(currentUserIdentity.authUserId())
			.orElseGet(() -> {
				insertUserIfAbsent(currentUserIdentity);
				return userRepository.findByIdForUpdate(currentUserIdentity.authUserId())
					.orElseThrow(() -> new IllegalStateException("현재 사용자를 잠글 수 없어요."));
			});
	}

	private UserEntity insertUserIfAbsentAndLoad(CurrentUserIdentity currentUserIdentity) {
		insertUserIfAbsent(currentUserIdentity);
		return userRepository.findById(currentUserIdentity.authUserId())
			.orElseThrow(() -> new IllegalStateException("현재 사용자를 찾을 수 없어요."));
	}

	private void insertUserIfAbsent(CurrentUserIdentity currentUserIdentity) {
		userRepository.insertIfAbsent(
			currentUserIdentity.authUserId(),
			currentUserIdentity.authProvider(),
			currentUserIdentity.displayName(),
			UserRole.USER.name()
		);
	}

	private java.util.Optional<CoupleEntity> findCurrentRelationship(String userId) {
		List<CoupleEntity> couples = coupleRepository.findParticipantCouplesByStatuses(
			userId,
			EnumSet.of(CoupleStatus.ACTIVE, CoupleStatus.INVITE_PENDING)
		);
		return couples.stream().findFirst();
	}

	private java.util.Optional<CoupleEntity> findActiveRelationship(String userId) {
		List<CoupleEntity> couples = coupleRepository.findParticipantCouplesByStatuses(
			userId,
			EnumSet.of(CoupleStatus.ACTIVE)
		);
		return couples.stream().findFirst();
	}

	private boolean hasOpenRelationship(String userId) {
		return findCurrentRelationship(userId).isPresent();
	}

	private CoupleEntity getPendingInvite(String inviteCode) {
		return coupleRepository.findByInviteCodeAndStatus(inviteCode, CoupleStatus.INVITE_PENDING)
			.orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND,
				"invite_not_found",
				"초대 코드를 찾을 수 없어요.",
				"만료됐거나 존재하지 않는 초대 코드예요."
			));
	}

	private CoupleEntity getPendingInviteForUpdate(String inviteCode) {
		return coupleRepository.findByInviteCodeAndStatusForUpdate(inviteCode, CoupleStatus.INVITE_PENDING)
			.orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND,
				"invite_not_found",
				"초대 코드를 찾을 수 없어요.",
				"만료됐거나 존재하지 않는 초대 코드예요."
			));
	}

	private TodayCardResponse getTodayCard(CoupleEntity couple, UserEntity currentUser) {
		return dayCardRepository.findByCouple_IdAndLocalDate(couple.getId(), today())
			.map((dayCard) -> toTodayCard(dayCard, currentUser))
			.orElseGet(() -> emptyTodayCard(today()));
	}

	private List<MomentRecordResponse> getRecentMoments(CoupleEntity couple, UserEntity currentUser, int limit) {
		if (couple == null || couple.getStatus() != CoupleStatus.ACTIVE) {
			return List.of();
		}
		return getRecordedMoments(couple, currentUser).stream()
			.limit(limit)
			.collect(Collectors.toList());
	}

	private List<MomentRecordResponse> getRecordedMoments(CoupleEntity couple, UserEntity currentUser) {
		return dayCardRepository.findByCouple_IdOrderByLocalDateDesc(couple.getId()).stream()
			.filter(this::hasRecordedEntries)
			.map((dayCard) -> toMomentRecord(dayCard, currentUser))
			.collect(Collectors.toList());
	}

	private List<DayCardEntity> getRecentWindowCards(String coupleId) {
		LocalDate today = today();
		return dayCardRepository.findByCouple_IdAndLocalDateBetweenOrderByLocalDateDesc(
			coupleId,
			today.minusDays(BOOK_LOOKBACK_DAYS - 1L),
			today
		);
	}

	private BookProgressResponse getBookProgress(CoupleEntity couple) {
		if (couple == null || couple.getStatus() != CoupleStatus.ACTIVE) {
			return bookProgress(0);
		}
		return bookProgress(recordedDays(getRecentWindowCards(couple.getId())));
	}

	private int recordedDays(List<DayCardEntity> dayCards) {
		return (int) dayCards.stream()
			.filter(this::hasRecordedEntries)
			.count();
	}

	private boolean hasRecordedEntries(DayCardEntity dayCard) {
		return dayCard.getEntries().stream().anyMatch(CardEntryEntity::hasRecordedContent);
	}

	private TodayCardResponse toTodayCard(DayCardEntity dayCard, UserEntity currentUser) {
		CardEntryEntity myEntry = dayCard.findEntry(currentUser.getId()).orElse(null);
		UserEntity counterpart = dayCard.getCouple().getCounterpart(currentUser.getId());
		CardEntryEntity partnerEntry = counterpart == null ? null : dayCard.findEntry(counterpart.getId()).orElse(null);

		return new TodayCardResponse(
			dayCard.getLocalDate().toString(),
			dateLabel(dayCard.getLocalDate()),
			toTodayCardState(dayCard.getState(), myEntry, partnerEntry),
			toRecordEntry(myEntry),
			toRecordEntry(partnerEntry)
		);
	}

	private MomentRecordResponse toMomentRecord(DayCardEntity dayCard, UserEntity currentUser) {
		CardEntryEntity myEntry = dayCard.findEntry(currentUser.getId()).orElse(null);
		UserEntity counterpart = dayCard.getCouple().getCounterpart(currentUser.getId());
		CardEntryEntity partnerEntry = counterpart == null ? null : dayCard.findEntry(counterpart.getId()).orElse(null);

		return new MomentRecordResponse(
			"moment-%s-%s".formatted(dayCard.getCouple().getId(), dayCard.getLocalDate()),
			dayCard.getLocalDate().toString(),
			dateLabel(dayCard.getLocalDate()),
			toRecordState(dayCard.getState()),
			toRecordEntry(myEntry),
			toRecordEntry(partnerEntry)
		);
	}

	private RecordEntryResponse toRecordEntry(CardEntryEntity entry) {
		if (entry == null) {
			return null;
		}
		EmotionView emotionView = emotionCatalog.get(entry.getEmotionCode());
		return new RecordEntryResponse(
			entry.getUser().getDisplayName(),
			entry.getEmotionCode(),
			emotionView.emoji(),
			emotionView.label(),
			entry.getMemo(),
			entry.getPhotoUrl()
		);
	}

	private RelationshipSummaryResponse relationshipSummary(CoupleEntity couple, UserEntity currentUser) {
		if (couple == null) {
			return unconnectedRelationship(currentUser);
		}

		if (couple.getStatus() == CoupleStatus.INVITE_PENDING) {
			return new RelationshipSummaryResponse(
				RelationshipState.INVITE_PENDING,
				couple.getId(),
				currentUser.getDisplayName(),
				null,
				couple.getAnniversaryDate().toString(),
				couple.getInviteCode()
			);
		}

		UserEntity counterpart = couple.getCounterpart(currentUser.getId());
		return new RelationshipSummaryResponse(
			RelationshipState.CONNECTED,
			couple.getId(),
			currentUser.getDisplayName(),
			counterpart != null ? counterpart.getDisplayName() : null,
			couple.getAnniversaryDate().toString(),
			couple.getInviteCode()
		);
	}

	private RelationshipSummaryResponse unconnectedRelationship(UserEntity currentUser) {
		return new RelationshipSummaryResponse(
			RelationshipState.UNCONNECTED,
			null,
			currentUser.getDisplayName(),
			null,
			null,
			null
		);
	}

	private TodayCardResponse emptyTodayCard(LocalDate localDate) {
		return new TodayCardResponse(
			localDate.toString(),
			dateLabel(localDate),
			TodayCardState.EMPTY,
			null,
			null
		);
	}

	private TodayCardState toTodayCardState(DayCardStatus state, CardEntryEntity myEntry, CardEntryEntity partnerEntry) {
		if (state == DayCardStatus.CLOSED) {
			return TodayCardState.CLOSED;
		}
		if (myEntry == null && partnerEntry == null) {
			return TodayCardState.EMPTY;
		}
		if (myEntry != null && partnerEntry != null) {
			return TodayCardState.COMPLETE;
		}
		return myEntry != null ? TodayCardState.MINE_ONLY : TodayCardState.PARTNER_ONLY;
	}

	private RecordState toRecordState(DayCardStatus state) {
		return switch (state) {
			case COMPLETE -> RecordState.COMPLETE;
			case CLOSED -> RecordState.CLOSED;
			default -> RecordState.PARTIAL;
		};
	}

	private BookProgressResponse bookProgress(int recordedDays) {
		return new BookProgressResponse(
			BOOK_LOOKBACK_DAYS,
			BOOK_REQUIRED_DAYS,
			recordedDays,
			Math.max(BOOK_REQUIRED_DAYS - recordedDays, 0),
			recordedDays >= BOOK_REQUIRED_DAYS ? BookState.ELIGIBLE : BookState.GROWING
		);
	}

	private String nextCoupleId() {
		return "cpl_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

	private String nextInviteCode() {
		String candidate = "TODAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
		while (coupleRepository.existsByInviteCode(candidate)) {
			candidate = "TODAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
		}
		return candidate;
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}

	private String dateLabel(LocalDate localDate) {
		return DATE_LABEL_FORMATTER.format(localDate);
	}

	private Instant closeAtUtc(LocalDate localDate) {
		return localDate.plusDays(1L)
			.atTime(LocalTime.of(4, 0))
			.atZone(ZoneId.of(clock.getZone().getId()))
			.toInstant();
	}
}
