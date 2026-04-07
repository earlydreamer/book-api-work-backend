package dev.earlydreamer.todayus.service;

import dev.earlydreamer.todayus.config.SweetbookProperties;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SweetbookBookService {

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

	public BookSnapshotEntity buildSnapshot(Long snapshotId) {
		BookSnapshotEntity snapshot = bookSnapshotRepository.findByIdWithItems(snapshotId)
			.orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND,
				"snapshot_not_found",
				"책 스냅샷을 찾을 수 없어요.",
				"존재하지 않는 스냅샷이에요."
			));

		SweetbookBookEntity sweetbookBook = sweetbookBookRepository.save(new SweetbookBookEntity(
			snapshot,
			sweetbookProperties.bookSpecId(),
			sweetbookProperties.templateId(),
			"snapshot-%d".formatted(snapshotId)
		));

		try {
			CreateBookResult createBookResult = sweetbookClient.createBook(new CreateBookCommand(
				"오늘 우리 %s".formatted(snapshot.getWindowEndDate()),
				sweetbookProperties.bookSpecId(),
				"snapshot-%d".formatted(snapshotId)
			));
			sweetbookBook.attachBookUid(createBookResult.bookUid());

			Map<String, String> uploadedFileNames = uploadSnapshotAssets(createBookResult.bookUid(), sweetbookBook, snapshot.getItems());
			createCover(createBookResult.bookUid(), uploadedFileNames);
			createContents(createBookResult.bookUid(), snapshot.getItems(), uploadedFileNames);

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

	private void createCover(String bookUid, Map<String, String> uploadedFileNames) {
		Map<String, Object> coverParameters = new LinkedHashMap<>();
		coverParameters.put("title", "오늘 우리");
		String firstUploadedFileName = uploadedFileNames.values().stream().findFirst().orElse(null);
		if (firstUploadedFileName != null) {
			coverParameters.put("frontPhoto", firstUploadedFileName);
			coverParameters.put("backPhoto", firstUploadedFileName);
		}

		sweetbookClient.createCover(bookUid, sweetbookProperties.templateId(), coverParameters);
	}

	private void createContents(String bookUid, List<BookSnapshotItemEntity> snapshotItems, Map<String, String> uploadedFileNames) {
		for (BookSnapshotItemEntity item : snapshotItems) {
			Map<String, Object> parameters = new LinkedHashMap<>();
			parameters.put("date", item.getLocalDate().toString());
			if (item.getPhotoAsset() != null) {
				parameters.put("galleryPhotos", List.of(uploadedFileNames.get(item.getPhotoAsset().getId())));
			} else {
				parameters.put("galleryPhotos", List.of());
			}
			sweetbookClient.createContent(bookUid, sweetbookProperties.templateId(), parameters, "page");
		}
	}
}
