package com.cmbchina.datadirect.caliber.adapter.web;

import com.cmbchina.datadirect.caliber.application.exception.BusinessConflictException;
import com.cmbchina.datadirect.caliber.application.exception.ResourceNotFoundException;
import com.cmbchina.datadirect.caliber.domain.exception.DomainValidationException;
import com.cmbchina.datadirect.caliber.infrastructure.common.logging.RequestTraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                    HttpServletRequest request,
                                                                    HttpServletResponse response) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request, response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleConstraintViolation(ConstraintViolationException ex,
                                                                 HttpServletRequest request,
                                                                 HttpServletResponse response) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request, response);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ApiErrorDTO> handleDomainValidation(DomainValidationException ex,
                                                              HttpServletRequest request,
                                                              HttpServletResponse response) {
        return build(HttpStatus.BAD_REQUEST, "DOMAIN_VALIDATION_ERROR", ex.getMessage(), request, response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorDTO> handleNotFound(ResourceNotFoundException ex,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request, response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorDTO> handleNoResource(NoResourceFoundException ex,
                                                        HttpServletRequest request,
                                                        HttpServletResponse response) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request, response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorDTO> handleIllegalState(IllegalStateException ex,
                                                          HttpServletRequest request,
                                                          HttpServletResponse response) {
        return build(HttpStatus.CONFLICT, "ILLEGAL_STATE", ex.getMessage(), request, response);
    }

    @ExceptionHandler(BusinessConflictException.class)
    public ResponseEntity<ApiErrorDTO> handleBusinessConflict(BusinessConflictException ex,
                                                              HttpServletRequest request,
                                                              HttpServletResponse response) {
        return build(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), request, response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleUnexpected(Exception ex,
                                                        HttpServletRequest request,
                                                        HttpServletResponse response) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Unexpected error at " + request.getRequestURI(),
                request,
                response);
    }

    private ResponseEntity<ApiErrorDTO> build(HttpStatus status,
                                              String code,
                                              String message,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        String requestId = RequestTraceContext.ensureRequestId(request, response);
        RequestTraceContext.markErrorCode(request, response, code);
        return ResponseEntity.status(status)
                .body(new ApiErrorDTO(code, message, requestId, OffsetDateTime.now()));
    }
}
