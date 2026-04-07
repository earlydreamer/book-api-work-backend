package dev.earlydreamer.todayus.integration.sweetbook.dto;

public record UploadPhotoCommand(
	String fileName,
	String contentType,
	byte[] bytes
) {
}
