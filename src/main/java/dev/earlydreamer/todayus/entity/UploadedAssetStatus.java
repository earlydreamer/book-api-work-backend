package dev.earlydreamer.todayus.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum UploadedAssetStatus {
	PENDING_UPLOAD("pending-upload"),
	UPLOADED("uploaded");

	private final String value;

	UploadedAssetStatus(String value) {
		this.value = value;
	}

	@JsonValue
	public String value() {
		return this.value;
	}
}
