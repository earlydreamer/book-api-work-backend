package dev.earlydreamer.todayus.config;

import dev.earlydreamer.todayus.service.AwsR2UploadUrlSigner;
import dev.earlydreamer.todayus.service.DisabledR2UploadUrlSigner;
import dev.earlydreamer.todayus.service.R2UploadUrlSigner;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class R2StorageConfig {

	@Bean
	R2UploadUrlSigner r2UploadUrlSigner(R2Properties r2Properties) {
		if (isMissing(r2Properties.accountId())
			|| isMissing(r2Properties.accessKeyId())
			|| isMissing(r2Properties.secretAccessKey())
			|| isMissing(r2Properties.bucket())) {
			return new DisabledR2UploadUrlSigner();
		}

		S3Presigner s3Presigner = S3Presigner.builder()
			.endpointOverride(URI.create("https://%s.r2.cloudflarestorage.com".formatted(r2Properties.accountId())))
			.credentialsProvider(StaticCredentialsProvider.create(
				AwsBasicCredentials.create(r2Properties.accessKeyId(), r2Properties.secretAccessKey())
			))
			.region(Region.of("auto"))
			.serviceConfiguration(S3Configuration.builder()
				.pathStyleAccessEnabled(true)
				.build())
			.build();

		return new AwsR2UploadUrlSigner(r2Properties.bucket(), s3Presigner);
	}

	private boolean isMissing(String value) {
		return value == null || value.isBlank();
	}
}
