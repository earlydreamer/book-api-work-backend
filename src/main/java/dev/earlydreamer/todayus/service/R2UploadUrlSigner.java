package dev.earlydreamer.todayus.service;

import java.time.Duration;

public interface R2UploadUrlSigner {

	String createPutUploadUrl(String objectKey, String contentType, Duration expiresIn);
}
