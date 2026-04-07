package dev.earlydreamer.todayus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.earlydreamer.todayus.dto.couples.CreateInviteRequest;
import dev.earlydreamer.todayus.entity.CoupleEntity;
import dev.earlydreamer.todayus.entity.CoupleStatus;
import dev.earlydreamer.todayus.entity.UserEntity;
import dev.earlydreamer.todayus.entity.UserRole;
import dev.earlydreamer.todayus.repository.CoupleRepository;
import dev.earlydreamer.todayus.repository.DayCardRepository;
import dev.earlydreamer.todayus.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(MockitoExtension.class)
class TodayUsContractServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private CoupleRepository coupleRepository;

	@Mock
	private DayCardRepository dayCardRepository;

	@Mock
	private CurrentUserProvider currentUserProvider;

	@Mock
	private UploadedAssetService uploadedAssetService;

	@Mock
	private BookSnapshotService bookSnapshotService;

	@Mock
	private SweetbookBookService sweetbookBookService;

	@Mock
	private OrderService orderService;

	@Mock
	private EmotionCatalog emotionCatalog;

	private final Clock clock = Clock.fixed(Instant.parse("2026-04-07T00:00:00Z"), ZoneId.of("Asia/Seoul"));

	private TodayUsContractService contractService;

	@BeforeEach
	void setUp() {
		contractService = new TodayUsContractService(
			userRepository,
			coupleRepository,
			dayCardRepository,
			currentUserProvider,
			uploadedAssetService,
			bookSnapshotService,
			sweetbookBookService,
			orderService,
			emotionCatalog,
			clock
		);
	}

	@Test
	void createInviteUsesLockedCurrentUserLookup() {
		UserEntity currentUser = new UserEntity("local-user-4", "local-dev", "하늘", UserRole.USER);
		when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUserIdentity("local-user-4", "하늘", "local-dev"));
		when(userRepository.findByIdForUpdate("local-user-4")).thenReturn(Optional.of(currentUser));
		when(coupleRepository.findParticipantCouplesByStatuses("local-user-4", java.util.EnumSet.of(CoupleStatus.ACTIVE, CoupleStatus.INVITE_PENDING)))
			.thenReturn(List.of());
		when(coupleRepository.existsByInviteCode(any())).thenReturn(false);
		when(coupleRepository.save(any(CoupleEntity.class))).thenAnswer((invocation) -> invocation.getArgument(0));

		contractService.createInvite(new CreateInviteRequest("2026-04-08"));

		verify(userRepository).findByIdForUpdate("local-user-4");
	}

	@Test
	void acceptInviteUsesLockedPendingInviteLookup() {
		UserEntity inviter = new UserEntity("local-user-4", "local-dev", "하늘", UserRole.USER);
		UserEntity invitee = new UserEntity("local-user-5", "local-dev", "도윤", UserRole.USER);
		CoupleEntity pendingInvite = CoupleEntity.invitePending("cpl_pending", inviter, "TODAYABCD1234", LocalDate.parse("2026-04-08"));

		when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUserIdentity("local-user-5", "도윤", "local-dev"));
		when(userRepository.findByIdForUpdate("local-user-5")).thenReturn(Optional.of(invitee));
		when(coupleRepository.findParticipantCouplesByStatuses("local-user-5", java.util.EnumSet.of(CoupleStatus.ACTIVE, CoupleStatus.INVITE_PENDING)))
			.thenReturn(List.of());
		when(coupleRepository.findByInviteCodeAndStatusForUpdate("TODAYABCD1234", CoupleStatus.INVITE_PENDING))
			.thenReturn(Optional.of(pendingInvite));

		contractService.acceptInvite("TODAYABCD1234");

		verify(coupleRepository).findByInviteCodeAndStatusForUpdate("TODAYABCD1234", CoupleStatus.INVITE_PENDING);
	}

	@Test
	void getHomeLoadsInsertedUserAfterInsertIfAbsent() {
		UserEntity persistedUser = new UserEntity("supabase-user-1", "supabase", "지우", UserRole.USER);
		when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUserIdentity("supabase-user-1", "지우", "supabase"));
		when(userRepository.findById("supabase-user-1"))
			.thenReturn(Optional.empty())
			.thenReturn(Optional.of(persistedUser));
		when(userRepository.insertIfAbsent("supabase-user-1", "supabase", "지우", "USER")).thenReturn(1);
		when(coupleRepository.findParticipantCouplesByStatuses("supabase-user-1", java.util.EnumSet.of(CoupleStatus.ACTIVE, CoupleStatus.INVITE_PENDING)))
			.thenReturn(List.of());

		var response = contractService.getHome();

		assertThat(response.relationship().myName()).isEqualTo("지우");
	}

	@Test
	void createInviteReacquiresUserLockAfterConcurrentUserInsertCollision() {
		UserEntity persistedUser = new UserEntity("supabase-user-2", "supabase", "민서", UserRole.USER);
		when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUserIdentity("supabase-user-2", "민서", "supabase"));
		when(userRepository.findByIdForUpdate("supabase-user-2"))
			.thenReturn(Optional.empty())
			.thenReturn(Optional.of(persistedUser));
		when(userRepository.insertIfAbsent("supabase-user-2", "supabase", "민서", "USER")).thenReturn(0);
		when(coupleRepository.findParticipantCouplesByStatuses("supabase-user-2", java.util.EnumSet.of(CoupleStatus.ACTIVE, CoupleStatus.INVITE_PENDING)))
			.thenReturn(List.of());
		when(coupleRepository.existsByInviteCode(any())).thenReturn(false);
		when(coupleRepository.save(any(CoupleEntity.class))).thenAnswer((invocation) -> invocation.getArgument(0));

		contractService.createInvite(new CreateInviteRequest("2026-04-08"));

		verify(userRepository, org.mockito.Mockito.times(2)).findByIdForUpdate("supabase-user-2");
	}
}
