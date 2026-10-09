
package com.smartmove.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler
        extends ResponseEntityExceptionHandler {

    // ==========================================
    // STANDARD ERROR RESPONSE
    // ==========================================

    public record ApiError(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {}

    // ==========================================
    // INVALID REQUEST BODY
    // ==========================================

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Map<String, String> fieldErrors =
                new LinkedHashMap<>();

        for (FieldError fieldError :
                exception.getBindingResult().getFieldErrors()) {

            fieldErrors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage() == null
                            ? "Invalid value"
                            : fieldError.getDefaultMessage()
            );
        }

        for (ObjectError objectError :
                exception.getBindingResult().getGlobalErrors()) {

            fieldErrors.putIfAbsent(
                    objectError.getObjectName(),
                    objectError.getDefaultMessage() == null
                            ? "Invalid request"
                            : objectError.getDefaultMessage()
            );
        }

        ApiError error = createError(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                requestPath(request),
                fieldErrors
        );

        return new ResponseEntity<>(
                error,
                headers,
                HttpStatus.BAD_REQUEST
        );
    }

    // ==========================================
    // MALFORMED JSON OR INVALID DATA TYPE
    // ==========================================

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ApiError error = createError(
                HttpStatus.BAD_REQUEST,
                "Invalid JSON request body or field format",
                requestPath(request),
                Map.of()
        );

        return new ResponseEntity<>(
                error,
                headers,
                HttpStatus.BAD_REQUEST
        );
    }

    // ==========================================
    // MISSING QUERY PARAMETERS
    // ==========================================

    @Override
    protected ResponseEntity<Object>
    handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ApiError error = createError(
                HttpStatus.BAD_REQUEST,
                "Missing required parameter: "
                        + exception.getParameterName(),
                requestPath(request),
                Map.of()
        );

        return new ResponseEntity<>(
                error,
                headers,
                HttpStatus.BAD_REQUEST
        );
    }

    // ==========================================
    // SPRING RESPONSE STATUS EXCEPTIONS
    // ==========================================

    @Override
    protected ResponseEntity<Object> handleErrorResponseException(
            ErrorResponseException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String message = exception.getBody().getDetail();

        if (message == null || message.isBlank()) {
            message = "Request could not be processed";
        }

        ApiError error = createError(
                status,
                message,
                requestPath(request),
                Map.of()
        );

        return new ResponseEntity<>(
                error,
                headers,
                status
        );
    }

    // ==========================================
    // CONSTRAINT VALIDATION
    // ==========================================

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors =
                new LinkedHashMap<>();

        exception.getConstraintViolations().forEach(
                violation -> fieldErrors.putIfAbsent(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                )
        );

        return response(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request,
                fieldErrors
        );
    }

    // ==========================================
    // INVALID PATH / QUERY PARAMETER TYPES
    // ==========================================

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: "
                        + exception.getName(),
                request,
                Map.of()
        );
    }

    // ==========================================
    // DATABASE CONSTRAINT VIOLATIONS
    // ==========================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        // Do not expose database table names,
        // SQL statements or constraint details.
        return response(
                HttpStatus.CONFLICT,
                "This operation conflicts with "
                        + "existing database records",
                request,
                Map.of()
        );
    }

    // ==========================================
    // PERMISSION DENIED
    // ==========================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.FORBIDDEN,
                "You do not have permission "
                        + "to perform this operation",
                request,
                Map.of()
        );
    }

    // ==========================================
    // AUTHENTICATION FAILED
    // ==========================================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.UNAUTHORIZED,
                "Authentication is required "
                        + "or credentials are invalid",
                request,
                Map.of()
        );
    }

    // ==========================================
    // INVALID ARGUMENTS
    // ==========================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "Invalid request argument",
                request,
                Map.of()
        );
    }

    // ==========================================
    // UNEXPECTED SERVER ERRORS
    // ==========================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        // Avoid exposing stack traces,
        // database credentials or internal details.
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred",
                request,
                Map.of()
        );
    }

    // ==========================================
    // RESPONSE BUILDERS
    // ==========================================

    private ResponseEntity<ApiError> response(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors
    ) {
        ApiError error = createError(
                status,
                message,
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity
                .status(status)
                .body(error);
    }

    private ApiError createError(
            HttpStatusCode status,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        HttpStatus httpStatus =
                HttpStatus.resolve(status.value());

        String errorName = httpStatus == null
                ? "HTTP Error"
                : httpStatus.getReasonPhrase();

        return new ApiError(
                LocalDateTime.now(),
                status.value(),
                errorName,
                message,
                path,
                fieldErrors == null
                        ? Map.of()
                        : fieldErrors
        );
    }

    private String requestPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return servletRequest
                    .getRequest()
                    .getRequestURI();
        }

        return "";
    }
}
