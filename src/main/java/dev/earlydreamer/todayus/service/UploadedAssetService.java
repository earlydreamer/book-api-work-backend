package dev.earlydreamer.todayus.service;

import dev.earlydreamer.todayus.dto.upload.CompleteUploadRequest;
import dev.earlydreamer.todayus.dto.upload.CompleteUploadResponse;
import dev.earlydreamer.todayus.entity.UploadedAssetEntity;
import dev.earlydreamer.todayus.entity.UploadedAssetStatus;
import dev.earlydreamer.todayus.repository.UploadedAssetRepository;
import dev.earlydreamer.todayus.support.error.ApiException;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UploadedAssetService {

	private final UploadedAssetRepository uploadedAssetRepository;
	private final CurrentUserProvider currentUserProvider;
	private final Clock clock;

	public UploadedAssetService(
		UploadedAssetRepository uploadedAssetRepository,
		CurrentUserProvider currentUserProvider,
		Clock clock
	) {
		this.uploadedAssetRepository = uploadedAssetRepository;
		this.currentUserProvider = currentUserProvider;
		this.clock = clock;
	}

	@Transactional
	public CompleteUploadResponse completeUpload(CompleteUploadRequest request) {
		UploadedAssetEntity asset = requireOwnedAsset(request.assetId());
		asset.markUploaded(clock.instant());
		return new CompleteUploadResponse(asset.getId(), asset.getStatus(), asset.getPublicUrl());
	}

	public UploadedAssetEntity requireUsableAsset(String assetId, String ownerUserId, String coupleId) {
		UploadedAssetEntity asset = uploadedAssetRepository.findById(assetId)
			.orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND,
				"uploaded_asset_not_found",
				"업로드 자산을 찾을 수 없어요.",
				"존재하지 않거나 더 이상 사용할 수 없는 업로드 자산이에요."
			));

		if (!asset.getOwnerUser().getId().equals(ownerUserId) || !asset.getCouple().getId().equals(coupleId)) {
			throw new ApiException(
				HttpStatus.FORBIDDEN,
				"uploaded_asset_forbidden",
				"이 업로드 자산은 사용할 수 없어요.",
				"현재 사용자나 현재 관계에 속한 업로드 자산만 연결할 수 있어요."
			);
		}

		if (asset.getStatus() != UploadedAssetStatus.UPLOADED) {
			throw new ApiException(
				HttpStatus.CONFLICT,
				"uploaded_asset_not_ready",
				"업로드가 아직 끝나지 않았어요.",
				"업로드 완료 등록이 끝난 뒤에만 기록에 연결할 수 있어요."
			);
		}

		return asset;
	}

	private UploadedAssetEntity requireOwnedAsset(String assetId) {
		String currentUserId = currentUserProvider.getCurrentUser().authUserId();
		return uploadedAssetRepository.findByIdAndOwnerUser_Id(assetId, currentUserId)
			.orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND,
				"uploaded_asset_not_found",
				"업로드 자산을 찾을 수 없어요.",
				"현재 사용자가 만든 업로드 자산만 완료 처리할 수 있어요."
			));
	}
}
