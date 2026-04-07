package dev.earlydreamer.todayus.service;

import dev.earlydreamer.todayus.support.error.ApiException;
import java.time.Duration;
import org.springframework.http.HttpStatus;

public class DisabledR2UploadUrlSigner implements R2UploadUrlSigner {

	@Override
	public String createPutUploadUrl(String objectKey, String contentType, Duration expiresIn) {
		throw new ApiException(
			HttpStatus.SERVICE_UNAVAILABLE,
			"upload_disabled",
			"업로드 기능을 사용할 수 없어요.",
			"R2 연동 환경변수가 아직 설정되지 않았어요."
		);
	}
}
