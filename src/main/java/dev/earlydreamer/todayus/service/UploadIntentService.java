package dev.earlydreamer.todayus.service;

import dev.earlydreamer.todayus.config.R2Properties;
import dev.earlydreamer.todayus.dto.upload.CreateUploadIntentRequest;
import dev.earlydreamer.todayus.dto.upload.CreateUploadIntentResponse;
import dev.earlydreamer.todayus.entity.CoupleEntity;
import dev.earlydreamer.todayus.entity.CoupleStatus;
import dev.earlydreamer.todayus.entity.UploadedAssetEntity;
import dev.earlydreamer.todayus.entity.UserEntity;
import dev.earlydreamer.todayus.entity.UserRole;
import dev.earlydreamer.todayus.repository.CoupleRepository;
import dev.earlydreamer.todayus.repository.UploadedAssetRepository;
import dev.earlydreamer.todayus.repository.UserRepository;
import dev.earlydreamer.todayus.support.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UploadIntentService {

	private final UserRepository userRepository;
	private final CoupleRepository coupleRepository;
	private final UploadedAssetRepository uploadedAssetRepository;
	private final CurrentUserProvider currentUserProvider;
	private final R2UploadUrlSigner r2UploadUrlSigner;
	private final R2Properties r2Properties;
	private final Clock clock;

	public UploadIntentService(
		UserRepository userRepository,
		CoupleRepository coupleRepository,
		UploadedAssetRepository uploadedAssetRepository,
		CurrentUserProvider currentUserProvider,
		R2UploadUrlSigner r2UploadUrlSigner,
		R2Properties r2Properties,
		Clock clock
	) {
		this.userRepository = userRepository;
		this.coupleRepository = coupleRepository;
		this.uploadedAssetRepository = uploadedAssetRepository;
		this.currentUserProvider = currentUserProvider;
		this.r2UploadUrlSigner = r2UploadUrlSigner;
		this.r2Properties = r2Properties;
		this.clock = clock;
	}

	public CreateUploadIntentResponse createIntent(CreateUploadIntentRequest request) {
		UserEntity currentUser = getOrCreateCurrentUser();
		CoupleEntity activeCouple = getActiveCoupleOrThrow(currentUser.getId());
		String assetId = nextAssetId();
		String sanitizedFileName = sanitizeFileName(request.fileName());
		String objectKey = objectKey(currentUser.getId(), assetId, sanitizedFileName);
		String publicUrl = publicUrl(objectKey);

		UploadedAssetEntity asset = new UploadedAssetEntity(
			assetId,
			currentUser,
			activeCouple,
			objectKey,
			publicUrl,
			request.fileName(),
			request.contentType(),
			request.fileSize()
		);
		uploadedAssetRepository.save(asset);

		return new CreateUploadIntentResponse(
			asset.getId(),
			objectKey,
			r2UploadUrlSigner.createPutUploadUrl(objectKey, request.contentType(), Duration.ofSeconds(r2Properties.presignTtlSeconds())),
			publicUrl,
			r2Properties.presignTtlSeconds()
		);
	}

	private UserEntity getOrCreateCurrentUser() {
		CurrentUserIdentity currentUserIdentity = currentUserProvider.getCurrentUser();
		return userRepository.findById(currentUserIdentity.authUserId())
			.orElseGet(() -> {
				userRepository.insertIfAbsent(
					currentUserIdentity.authUserId(),
					currentUserIdentity.authProvider(),
					currentUserIdentity.displayName(),
					UserRole.USER.name()
				);
				return userRepository.findById(currentUserIdentity.authUserId())
					.orElseThrow(() -> new IllegalStateException("현재 사용자를 찾을 수 없어요."));
			});
	}

	private CoupleEntity getActiveCoupleOrThrow(String userId) {
		List<CoupleEntity> activeCouples = coupleRepository.findParticipantCouplesByStatuses(userId, EnumSet.of(CoupleStatus.ACTIVE));
		return activeCouples.stream()
			.findFirst()
			.orElseThrow(() -> new ApiException(
				HttpStatus.NOT_FOUND,
				"relationship_not_found",
				"현재 연결을 찾을 수 없어요.",
				"연결된 상대가 있을 때만 사진 업로드를 시작할 수 있어요."
			));
	}

	private String nextAssetId() {
		return "asset_" + UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT);
	}

	private String objectKey(String ownerUserId, String assetId, String sanitizedFileName) {
		LocalDate today = LocalDate.now(clock);
		return "%s/%s/%04d/%02d/%02d/%s-%s".formatted(
			r2Properties.uploadPrefix(),
			ownerUserId,
			today.getYear(),
			today.getMonthValue(),
			today.getDayOfMonth(),
			assetId,
			sanitizedFileName
		);
	}

	private String publicUrl(String objectKey) {
		String normalizedBaseUrl = r2Properties.publicBaseUrl().endsWith("/")
			? r2Properties.publicBaseUrl().substring(0, r2Properties.publicBaseUrl().length() - 1)
			: r2Properties.publicBaseUrl();
		return normalizedBaseUrl + "/" + objectKey;
	}

	private String sanitizeFileName(String fileName) {
		String leafName = fileName.replace("\\", "/");
		int lastSlashIndex = leafName.lastIndexOf('/');
		String rawFileName = lastSlashIndex >= 0 ? leafName.substring(lastSlashIndex + 1) : leafName;
		String sanitized = rawFileName.replaceAll("[^a-zA-Z0-9._-]", "-");
		if (sanitized.isBlank()) {
			return "upload.bin";
		}
		return sanitized;
	}
}
