package dev.earlydreamer.todayus.integration.storage;

import dev.earlydreamer.todayus.entity.UploadedAssetEntity;
import dev.earlydreamer.todayus.support.error.ApiException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PublicAssetHttpFetcher implements UploadedAssetBinaryFetcher {

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Override
	public FetchedUploadedAsset fetch(UploadedAssetEntity uploadedAsset) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(uploadedAsset.getPublicUrl()))
			.GET()
			.build();

		try {
			HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new ApiException(
					HttpStatus.BAD_GATEWAY,
					"sweetbook_asset_fetch_failed",
					"업로드 자산을 읽을 수 없어요.",
					"Sweetbook 업로드 전에 R2 자산을 내려받지 못했어요."
				);
			}
			return new FetchedUploadedAsset(
				uploadedAsset.getOriginalFileName(),
				uploadedAsset.getContentType(),
				response.body()
			);
		} catch (IOException | InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new ApiException(
				HttpStatus.BAD_GATEWAY,
				"sweetbook_asset_fetch_failed",
				"업로드 자산을 읽을 수 없어요.",
				"Sweetbook 업로드 전에 R2 자산을 내려받지 못했어요."
			);
		}
	}
}
