package dev.earlydreamer.todayus.service;

import java.time.Duration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public class AwsR2UploadUrlSigner implements R2UploadUrlSigner {

	private final String bucket;
	private final S3Presigner s3Presigner;

	public AwsR2UploadUrlSigner(String bucket, S3Presigner s3Presigner) {
		this.bucket = bucket;
		this.s3Presigner = s3Presigner;
	}

	@Override
	public String createPutUploadUrl(String objectKey, String contentType, Duration expiresIn) {
		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucket)
			.key(objectKey)
			.contentType(contentType)
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(expiresIn)
			.putObjectRequest(putObjectRequest)
			.build();

		PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);
		return presignedPutObjectRequest.url().toString();
	}
}
