package dev.earlydreamer.todayus.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BookSnapshotStatus {
	SNAPSHOT_BUILDING("snapshot-building"),
	READY_TO_ORDER("ready-to-order"),
	ORDERED("ordered");

	private final String value;

	BookSnapshotStatus(String value) {
		this.value = value;
	}

	@JsonValue
	public String value() {
		return this.value;
	}
}
