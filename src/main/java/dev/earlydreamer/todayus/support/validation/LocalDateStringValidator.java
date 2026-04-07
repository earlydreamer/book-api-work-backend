package dev.earlydreamer.todayus.support.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class LocalDateStringValidator implements ConstraintValidator<ValidLocalDate, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true;
		}

		try {
			LocalDate.parse(value);
			return true;
		} catch (DateTimeParseException exception) {
			return false;
		}
	}
}
