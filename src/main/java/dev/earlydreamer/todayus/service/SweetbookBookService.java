package dev.earlydreamer.todayus.service;

import dev.earlydreamer.todayus.dto.books.BuildBookSnapshotRequest;
import dev.earlydreamer.todayus.dto.upload.CompleteUploadRequest;
import dev.earlydreamer.todayus.dto.upload.CompleteUploadResponse;
import dev.earlydreamer.todayus.entity.BookSnapshotEntity;
import dev.earlydreamer.todayus.entity.BookSnapshotItemEntity;
import dev.earlydreamer.todayus.entity.SweetbookBookEntity;
import dev.earlydreamer.todayus.entity.SweetbookUploadedAssetEntity;
import dev.earlydreamer.todayus.entity.SweetbookUploadedAssetStatus;
import dev.earlydreamer.todayus.integration.storage.FetchedUploadedAsset;
import dev.earlydreamer.todayus.integration.storage.UploadedAssetBinaryFetcher;
import dev.earlydreamer.todayus.integration.sweetbook.SweetbookClient;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateBookCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.CreateBookResult;
import dev.earlydreamer.todayus.integration.sweetbook.dto.FinalizeBookResult;
import dev.earlydreamer.todayus.integration.sweetbook.dto.UploadPhotoCommand;
import dev.earlydreamer.todayus.integration.sweetbook.dto.UploadPhotoResult;
import dev.earlydreamer.todayus.repository.BookSnapshotRepository;
import dev.earlydreamer.todayus.repository.SweetbookBookRepository;
import dev.earlydreamer.todayus.repository.SweetbookUploadedAssetRepository;
import dev.earlydreamer.todayus.support.error.ApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SweetbookBookService {

	private static final DateTimeFormatter DATE_RANGE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
	private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

	private final BookSnapshotRepository bookSnapshotRepository;
	private final SweetbookBookRepository sweetbookBookRepository;
	private final SweetbookUploadedAssetRepository sweetbookUploadedAssetRepository;
	private final SweetbookClient sweetbookClient;
	private final UploadedAssetBinaryFetcher uploadedAssetBinaryFetcher;
	private final SweetbookProperties sweetbookProperties;

	public SweetbookBookService(
		BookSnapshotRepository bookSnapshotRepository,
		SweetbookBookRepository sweetbookBookRepository,
		SweetbookUploadedAssetRepository sweetbookUploadedAssetRepository,
		SweetbookClient sweetbookClient,
		UploadedAssetBinaryFetcher uploadedAssetBinaryFetcher,
		SweetbookProperties sweetbookProperties
	) {
		this.bookSnapshotRepository = bookSnapshotRepository;
		this.sweetbookBookRepository = sweetbookBookRepository;
		this.sweetbookUploadedAssetRepository = sweetbookUploadedAssetRepository;
		this.sweetbookClient = sweetbookClient;
		this.uploadedAssetBinaryFetcher = uploadedAssetBinaryFetcher;
		this.sweetbookProperties = sweetbookProperties;
	}

	public BookSnapshotEntity buildSnapshot(Long snapshotId, BuildBookSnapshotRequest request) {
		BookSnapshotEntity snapshot = bookSnapshotRepository.findByIdWithItems(snapshotId)
			.orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND,
				"snapshot_not_found",
				"책 스냅샷을 찾을 수 없어요.",
				"존재하지 않는 스냅샷이에요."
			));

		SweetbookBookEntity sweetbookBook = sweetbookBookRepository.save(new SweetbookBookEntity(
			snapshot,
			request.bookSpecId(),
			request.coverTemplateId(),
			request.contentTemplateId(),
			request.interleafTemplateId(),
			request.publishingTemplateId(),
			"snapshot-%d".formatted(snapshotId)
		));

		try {
			// 1. 책 생성
			CreateBookResult createBookResult = sweetbookClient.createBook(new CreateBookCommand(
				"오늘 우리 %s".formatted(snapshot.getWindowEndDate()),
				request.bookSpecId(),
				"snapshot-%d".formatted(snapshotId)
			));
			sweetbookBook.attachBookUid(createBookResult.bookUid());

			// 2. 사진 업로드
			Map<String, String> uploadedFileNames = uploadSnapshotAssets(createBookResult.bookUid(), sweetbookBook, snapshot.getItems());

			// 3. 표지 생성
			createCover(createBookResult.bookUid(), request, snapshot, uploadedFileNames);

			// 4. 간지 생성 (필요 시)
			if (request.interleafTemplateId() != null && !request.interleafTemplateId().isBlank()) {
				createInterleaf(createBookResult.bookUid(), request.interleafTemplateId(), snapshot);
			}

			// 5. 내지 생성
			createContents(createBookResult.bookUid(), request.contentTemplateId(), snapshot.getItems(), uploadedFileNames);

			// 6. 발행면 생성 (필요 시)
			if (request.publishingTemplateId() != null && !request.publishingTemplateId().isBlank()) {
				createPublishingPage(createBookResult.bookUid(), request.publishingTemplateId(), snapshot, uploadedFileNames);
			}

			// 7. 확정
			FinalizeBookResult finalizeBookResult = sweetbookClient.finalizeBook(createBookResult.bookUid());
			sweetbookBook.markFinalized(finalizeBookResult.finalizedAt());
			snapshot.markReadyToOrder(finalizeBookResult.finalizedAt());
			return snapshot;
		} catch (RuntimeException exception) {
			sweetbookBook.markFailed("sweetbook_book_build_failed", exception.getMessage());
			throw exception;
		}
	}

	private Map<String, String> uploadSnapshotAssets(
		String bookUid,
		SweetbookBookEntity sweetbookBook,
		List<BookSnapshotItemEntity> snapshotItems
	) {
		Map<String, String> uploadedFileNames = new LinkedHashMap<>();
		for (BookSnapshotItemEntity item : snapshotItems) {
			if (item.getPhotoAsset() == null || uploadedFileNames.containsKey(item.getPhotoAsset().getId())) {
				continue;
			}

			FetchedUploadedAsset fetchedUploadedAsset = uploadedAssetBinaryFetcher.fetch(item.getPhotoAsset());
			UploadPhotoResult uploadPhotoResult = sweetbookClient.uploadPhoto(
				bookUid,
				new UploadPhotoCommand(fetchedUploadedAsset.fileName(), fetchedUploadedAsset.contentType(), fetchedUploadedAsset.bytes())
			);
			uploadedFileNames.put(item.getPhotoAsset().getId(), uploadPhotoResult.fileName());
			sweetbookUploadedAssetRepository.save(new SweetbookUploadedAssetEntity(
				sweetbookBook,
				item.getPhotoAsset(),
				uploadPhotoResult.fileName(),
				SweetbookUploadedAssetStatus.UPLOADED,
				Instant.now()
			));
		}
		return uploadedFileNames;
	}

	private void createCover(String bookUid, BuildBookSnapshotRequest request, BookSnapshotEntity snapshot, Map<String, String> uploadedFileNames) {
		Map<String, Object> coverParameters = new LinkedHashMap<>();
		String firstUploadedFileName = uploadedFileNames.values().stream().findFirst().orElse(null);

		coverParameters.put("coverPhoto", firstUploadedFileName != null ? firstUploadedFileName : "");
		coverParameters.put("subtitle", "우리의 모든 순간들");
		coverParameters.put("dateRange", "%s - %s".formatted(
			snapshot.getWindowStartDate().format(DateTimeFormatter.ofPattern("yyyy.MM")),
			snapshot.getWindowEndDate().format(DateTimeFormatter.ofPattern("yyyy.MM"))
		));

		sweetbookClient.createCover(bookUid, request.coverTemplateId(), coverParameters);
	}

	private void createInterleaf(String bookUid, String templateId, BookSnapshotEntity snapshot) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("monthYearTitle", "%s\n%s".formatted(
			snapshot.getWindowStartDate().format(MONTH_YEAR_FORMATTER).toUpperCase(),
			snapshot.getWindowEndDate().format(MONTH_YEAR_FORMATTER).toUpperCase()
		));
		params.put("dateRangeDetail", "%s - %s".formatted(
			snapshot.getWindowStartDate().format(DATE_RANGE_FORMATTER),
			snapshot.getWindowEndDate().format(DATE_RANGE_FORMATTER)
		));
		params.put("photoCount", "%d장".formatted(snapshot.getSelectedItemCount()));

		sweetbookClient.createContent(bookUid, templateId, params, "page");
	}

	private void createContents(String bookUid, String templateId, List<BookSnapshotItemEntity> snapshotItems, Map<String, String> uploadedFileNames) {
		for (BookSnapshotItemEntity item : snapshotItems) {
			Map<String, Object> parameters = new LinkedHashMap<>();
			parameters.put("date", item.getLocalDate().toString());
			parameters.put("user1text", item.getMeMemo() != null ? item.getMeMemo() : "");
			parameters.put("user2text", item.getPartnerMemo() != null ? item.getPartnerMemo() : "");
			parameters.put("name1", item.getMeDisplayName() != null ? item.getMeDisplayName() : "");
			parameters.put("name2", item.getPartnerDisplayName() != null ? item.getPartnerDisplayName() : "");

			// 내지의 경우 업로드된 파일명을 직접 파라미터로 넘기는 구조라면 (제시된 스펙엔 photo 필드가 없지만 보통 필요함)
			if (item.getPhotoAsset() != null) {
				parameters.put("photo", uploadedFileNames.get(item.getPhotoAsset().getId()));
			}

			sweetbookClient.createContent(bookUid, templateId, parameters, "page");
		}
	}

	private void createPublishingPage(String bookUid, String templateId, BookSnapshotEntity snapshot, Map<String, String> uploadedFileNames) {
		Map<String, Object> params = new LinkedHashMap<>();
		String firstUploadedFileName = uploadedFileNames.values().stream().findFirst().orElse(null);

		params.put("photo", firstUploadedFileName != null ? firstUploadedFileName : "");
		params.put("title", "오늘 우리");
		params.put("publishDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")));
		params.put("author", "오늘 우리");
		params.put("hashtags", "#오늘우리 #로그북");
		params.put("publisher", "(주)오늘우리");

		sweetbookClient.createContent(bookUid, templateId, params, "page");
	}
}
