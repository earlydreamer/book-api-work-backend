package dev.earlydreamer.todayus.support.error;

import dev.earlydreamer.todayus.dto.common.ContractTypes.FieldErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ApiProblemDetailHandler {

	@ExceptionHandler(ApiException.class)
	public ProblemDetail handleApiException(ApiException exception, HttpServletRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(exception.status(), exception.detail());
		problemDetail.setTitle(exception.title());
		problemDetail.setType(URI.create("https://api.todayus.dev/problems/" + exception.code()));
		problemDetail.setInstance(URI.create(request.getRequestURI()));
		problemDetail.setProperty("code", exception.code());
		problemDetail.setProperty("traceId", request.getRequestId());
		problemDetail.setProperty("fieldErrors", List.of());
		return problemDetail;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		List<FieldErrorResponse> fieldErrors = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(this::toFieldError)
			.collect(Collectors.toList());

		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST,
			"요청 본문을 다시 확인해 주세요."
		);
		problemDetail.setTitle("입력값이 올바르지 않아요.");
		problemDetail.setType(URI.create("https://api.todayus.dev/problems/validation-failed"));
		problemDetail.setInstance(URI.create(request.getRequestURI()));
		problemDetail.setProperty("code", "validation_failed");
		problemDetail.setProperty("traceId", request.getRequestId());
		problemDetail.setProperty("fieldErrors", fieldErrors);
		return problemDetail;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
		List<FieldErrorResponse> fieldErrors = exception.getConstraintViolations()
			.stream()
			.map(this::toFieldError)
			.collect(Collectors.toList());

		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
			HttpStatus.BAD_REQUEST,
			"요청 경로나 파라미터를 다시 확인해 주세요."
		);
		problemDetail.setTitle("입력값이 올바르지 않아요.");
		problemDetail.setType(URI.create("https://api.todayus.dev/problems/validation-failed"));
		problemDetail.setInstance(URI.create(request.getRequestURI()));
		problemDetail.setProperty("code", "validation_failed");
		problemDetail.setProperty("traceId", request.getRequestId());
		problemDetail.setProperty("fieldErrors", fieldErrors);
		return problemDetail;
	}

	private FieldErrorResponse toFieldError(FieldError fieldError) {
		return new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
	}

	private FieldErrorResponse toFieldError(ConstraintViolation<?> constraintViolation) {
		String propertyPath = constraintViolation.getPropertyPath().toString();
		int lastDot = propertyPath.lastIndexOf('.');
		String field = lastDot >= 0 ? propertyPath.substring(lastDot + 1) : propertyPath;
		return new FieldErrorResponse(field, constraintViolation.getMessage());
	}
}
