package dev.earlydreamer.todayus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.earlydreamer.todayus.config.SweetbookProperties;
import dev.earlydreamer.todayus.entity.BookSnapshotEntity;
import dev.earlydreamer.todayus.entity.BookSnapshotItemEntity;
import dev.earlydreamer.todayus.entity.BookSnapshotStatus;
import dev.earlydreamer.todayus.entity.CoupleEntity;
import dev.earlydreamer.todayus.entity.DayCardEntity;
import dev.earlydreamer.todayus.entity.UploadedAssetEntity;
import dev.earlydreamer.todayus.entity.UserEntity;
import dev.earlydreamer.todayus.entity.UserRole;
import dev.earlydreamer.todayus.integration.storage.FetchedUploadedAsset;
import dev.earlydreamer.todayus.integration.storage.UploadedAssetBinaryFetcher;
import dev.earlydreamer.todayus.integration.sweetbook.SweetbookClient;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateBookCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateBookResult;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateOrderCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateOrderResult;
import dev.earlydreamer.todayus.integration.sweetbook.dto.FinalizeBookResult;
import dev.earlydreamer.todayus.integration.sweetbook.dto.UploadPhotoCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.UploadPhotoResult;
import dev.earlydreamer.todayus.repository.BookSnapshotRepository;
import dev.earlydreamer.todayus.repository.SweetbookBookRepository;
import dev.earlydreamer.todayus.repository.SweetbookUploadedAssetRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SweetbookBookServiceTest {

	@Mock
	private BookSnapshotRepository bookSnapshotRepository;

	@Mock
	private SweetbookBookRepository sweetbookBookRepository;

	@Mock
	private SweetbookUploadedAssetRepository sweetbookUploadedAssetRepository;

	@Mock
	private SweetbookClient sweetbookClient;

	@Mock
	private UploadedAssetBinaryFetcher uploadedAssetBinaryFetcher;

	private SweetbookBookService sweetbookBookService;

	@BeforeEach
	void setUp() {
		sweetbookBookService = new SweetbookBookService(
			bookSnapshotRepository,
			sweetbookBookRepository,
			sweetbookUploadedAssetRepository,
			sweetbookClient,
			uploadedAssetBinaryFetcher,
			new SweetbookProperties(
				"https://api-sandbox.sweetbook.com",
				"test-api-key",
				"PHOTOBOOK_A4_SC",
				"tpl_shared",
				"test-webhook-secret"
			)
		);
	}

	@Test
	void buildSnapshotCreatesBookUploadsAssetsAndFinalizes() {
		UserEntity me = new UserEntity("local-user-1", "local-dev", "지우", UserRole.USER);
		UserEntity partner = new UserEntity("local-user-2", "local-dev", "민준", UserRole.USER);
		CoupleEntity couple = CoupleEntity.invitePending("cpl_active_20260407", me, "TODAY2026", LocalDate.parse("2026-04-07"));
		couple.connect(partner, Instant.parse("2026-04-07T00:00:00Z"));
		DayCardEntity dayCard = new DayCardEntity(couple, LocalDate.parse("2026-04-06"), Instant.parse("2026-04-06T19:00:00Z"));
		UploadedAssetEntity uploadedAsset = new UploadedAssetEntity(
			"asset_abc123",
			me,
			couple,
			"uploads/local-user-1/2026/04/07/asset_abc123-photo.jpg",
			"https://cdn.example.com/uploads/local-user-1/2026/04/07/asset_abc123-photo.jpg",
			"photo.jpg",
			"image/jpeg",
			231231
		);
		uploadedAsset.markUploaded(Instant.parse("2026-04-07T00:00:00Z"));

		BookSnapshotEntity snapshot = new BookSnapshotEntity(
			couple,
			BookSnapshotStatus.SNAPSHOT_BUILDING,
			LocalDate.parse("2026-03-09"),
			LocalDate.parse("2026-04-07"),
			20,
			20,
			Instant.parse("2026-04-07T00:00:00Z")
		);
		snapshot.addItem(new BookSnapshotItemEntity(
			snapshot,
			dayCard,
			LocalDate.parse("2026-04-06"),
			"{\"author\":\"지우\"}",
			"{\"author\":\"민준\"}",
			uploadedAsset,
			1
		));

		when(bookSnapshotRepository.findByIdWithItems(1L)).thenReturn(Optional.of(snapshot));
		when(sweetbookBookRepository.save(any())).thenAnswer((invocation) -> invocation.getArgument(0));
		when(sweetbookUploadedAssetRepository.save(any())).thenAnswer((invocation) -> invocation.getArgument(0));
		when(sweetbookClient.createBook(any(CreateBookCommand.class))).thenReturn(new CreateBookResult("bk_test_123"));
		when(uploadedAssetBinaryFetcher.fetch(uploadedAsset)).thenReturn(new FetchedUploadedAsset("photo.jpg", "image/jpeg", new byte[] {1, 2, 3}));
		when(sweetbookClient.uploadPhoto(eq("bk_test_123"), any(UploadPhotoCommand.class))).thenReturn(new UploadPhotoResult("photo260107065637669.JPG"));
		when(sweetbookClient.finalizeBook("bk_test_123")).thenReturn(new FinalizeBookResult(24, Instant.parse("2026-04-07T00:05:00Z")));

		sweetbookBookService.buildSnapshot(1L);

		assertThat(snapshot.getStatus()).isEqualTo(BookSnapshotStatus.READY_TO_ORDER);
		verify(sweetbookClient).createBook(any(CreateBookCommand.class));
		verify(sweetbookClient).uploadPhoto(eq("bk_test_123"), any(UploadPhotoCommand.class));
		verify(sweetbookClient).createCover(eq("bk_test_123"), eq("tpl_shared"), any());
		verify(sweetbookClient).createContent(eq("bk_test_123"), eq("tpl_shared"), any(), eq("page"));
		verify(sweetbookClient).finalizeBook("bk_test_123");
		verify(sweetbookBookRepository).save(any());
		verify(sweetbookUploadedAssetRepository).save(any());
	}
}
