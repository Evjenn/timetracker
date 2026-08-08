package com.yevos.timetracker.model.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegisterRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should pass validation when all fields are perfectly valid")
    void validation_ValidRequest_NoViolations() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("validUser");
        request.setPassword("Password123");
        request.setHourlyRate(BigDecimal.valueOf(50.25));
        request.setEmail("user@example.com");
        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        // Then
        assertTrue(violations.isEmpty(), "Expected no validation errors");
    }

    @Test
    @DisplayName("Should fail validation when email format is invalid")
    void validation_InvalidEmail_HasViolations() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("validUser");
        request.setPassword("Password123");
        request.setHourlyRate(BigDecimal.valueOf(10.00));
        request.setEmail("invalid-email-format"); // Missing @ and domain
        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        // Then
        assertFalse(violations.isEmpty());
        long emailErrors = violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("email"))
                .count();
        assertEquals(1, emailErrors);
    }

    @Test
    @DisplayName("Should fail validation when hourly rate is zero or negative due to inclusive false")
    void validation_HourlyRateIsZeroOrNegative_HasViolations() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("validUser");
        request.setPassword("Password123");
        request.setHourlyRate(BigDecimal.valueOf(0.00));
        request.setEmail("user@example.com");
        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        // Then
        assertFalse(violations.isEmpty());
        boolean hasMinRateError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("hourlyRate"));
        assertTrue(hasMinRateError);
    }

    @Test
    @DisplayName("Should fail validation when hourly rate exceeds digits constraint")
    void validation_HourlyRateTooManyDigits_HasViolations() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("validUser");
        request.setPassword("Password123");
        request.setHourlyRate(new BigDecimal("1000000.00"));
        request.setEmail("user@example.com");
        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        // Then
        assertFalse(violations.isEmpty());
    }
}
