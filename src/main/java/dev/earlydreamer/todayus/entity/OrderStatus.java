package dev.earlydreamer.todayus.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
	REQUESTED("requested"),
	SUBMITTED("submitted"),
	CONFIRMED("confirmed"),
	IN_PRODUCTION("in-production"),
	PRODUCTION_COMPLETE("production-complete"),
	SHIPPED("shipped"),
	DELIVERED("delivered"),
	FAILED("failed"),
	CANCELED("canceled");

	private final String value;

	OrderStatus(String value) {
		this.value = value;
	}

	@JsonValue
	public String value() {
		return this.value;
	}
}
