package dev.earlydreamer.todayus.integration.storage;

public record FetchedUploadedAsset(
	String fileName,
	String contentType,
	byte[] bytes
) {
}
