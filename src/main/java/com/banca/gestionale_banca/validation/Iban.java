package com.banca.gestionale_banca.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Pattern(regexp = "^IT[0-9A-F]{20}$")
@ReportAsSingleViolation
public @interface Iban {
    String message() default "IBAN non valido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
