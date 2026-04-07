package dev.earlydreamer.todayus.support.error;

import org.springframework.http.HttpStatusCode;

public class ApiException extends RuntimeException {

	private final HttpStatusCode status;
	private final String code;
	private final String title;
	private final String detail;

	public ApiException(HttpStatusCode status, String code, String title, String detail) {
		super(detail);
		this.status = status;
		this.code = code;
		this.title = title;
		this.detail = detail;
	}

	public HttpStatusCode status() {
		return this.status;
	}

	public String code() {
		return this.code;
	}

	public String title() {
		return this.title;
	}

	public String detail() {
		return this.detail;
	}
}
