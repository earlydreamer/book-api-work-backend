package dev.earlydreamer.todayus.support.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LocalDateStringValidator.class)
public @interface ValidLocalDate {

	String message() default "유효한 날짜가 아니에요.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
