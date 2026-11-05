package com.server.server.middlewares.errors;

import com.server.server.exceptions.common.AccessDeniedException;
import com.server.server.exceptions.common.DuplicateResourceException;
import com.server.server.exceptions.common.ExternalServiceException;
import com.server.server.exceptions.common.ResourceNotFoundException;
import com.server.server.exceptions.common.StorageException;
import com.server.server.exceptions.common.WorkflowException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.server.server.constants.errors.ErrorCodes;
import com.server.server.utilities.responses.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/** Maps every exception to one status and error code in the shared response envelope. */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ BadCredentialsException.class, UsernameNotFoundException.class })
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(Exception ex, HttpServletRequest request) {
        return build(ErrorCodes.UNAUTHORIZED, "Invalid username or password", HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex, HttpServletRequest request) {
        return build(ErrorCodes.ACCOUNT_DISABLED, "This account has been deactivated.", HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityDenied(Exception ex, HttpServletRequest request) {
        return build(ErrorCodes.FORBIDDEN, "You do not have permission to perform this action.", HttpStatus.FORBIDDEN,
                request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCodes.FORBIDDEN, ex.getMessage(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler({ MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex, HttpServletRequest request) {
        return build(ErrorCodes.BAD_REQUEST, "The request is missing a value or contains an invalid value.",
                HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex,
            HttpServletRequest request) {
        return build(ErrorCodes.BAD_REQUEST, ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return build(ErrorCodes.VALIDATION_FAILED, "Please correct the highlighted fields.", HttpStatus.BAD_REQUEST,
                errors, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(ErrorCodes.NOT_FOUND, ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownRoute(NoResourceFoundException ex,
            HttpServletRequest request) {
        return build(ErrorCodes.ROUTE_NOT_FOUND, "This address does not exist.", HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return build(ErrorCodes.METHOD_NOT_ALLOWED, "This address does not accept " + ex.getMethod() + " requests.",
                HttpStatus.METHOD_NOT_ALLOWED, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaType(HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        return build(ErrorCodes.UNSUPPORTED_MEDIA_TYPE, "This content type is not supported.",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleTooLarge(MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        return build(ErrorCodes.PAYLOAD_TOO_LARGE, "The uploaded file is too large.", HttpStatus.PAYLOAD_TOO_LARGE,
                request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        return build(ErrorCodes.DUPLICATE_RESOURCE, ex.getMessage(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(Exception ex, HttpServletRequest request) {
        return build(ErrorCodes.DUPLICATE_RESOURCE, "Record conflict: Duplicate unique field.", HttpStatus.CONFLICT,
                request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleConcurrentUpdate(Exception ex, HttpServletRequest request) {
        return build(ErrorCodes.CONCURRENT_UPDATE, "This record was changed by another request. Please try again.",
                HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(WorkflowException.class)
    public ResponseEntity<ApiResponse<Void>> handleWorkflow(WorkflowException ex, HttpServletRequest request) {
        return build(ex.getCode(), ex.getMessage(), HttpStatus.CONFLICT, request);
    }

    /** State rules such as "only a pending request can be assigned". */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return build(ErrorCodes.CONFLICT, ex.getMessage(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleStorage(StorageException ex, HttpServletRequest request) {
        log.error("Storage failure: {}", ex.getMessage(), ex);
        return build(ErrorCodes.STORAGE_FAILED, "The file could not be stored or read. Please try again.",
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternal(ExternalServiceException ex, HttpServletRequest request) {
        log.error("External service failure: {}", ex.getMessage(), ex);
        return build(ErrorCodes.EXTERNAL_SERVICE_FAILED, "An external service is unavailable. Please try again later.",
                HttpStatus.BAD_GATEWAY, request);
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransaction(TransactionSystemException ex,
            HttpServletRequest request) {
        for (Throwable cause = ex; cause != null && cause.getCause() != cause; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violations) {
                String errors = violations.getConstraintViolations().stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.joining(", "));
                return build(ErrorCodes.VALIDATION_FAILED, "Validation failed: " + errors, HttpStatus.BAD_REQUEST,
                        request);
            }
        }
        log.error("Transaction error", ex);
        return build(ErrorCodes.DATABASE_ERROR, "A database error occurred.", HttpStatus.INTERNAL_SERVER_ERROR,
                request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return build(ErrorCodes.INTERNAL_ERROR, "An internal server error occurred", HttpStatus.INTERNAL_SERVER_ERROR,
                request);
    }

    private ResponseEntity<ApiResponse<Void>> build(String code, String message, HttpStatus status,
            HttpServletRequest request) {
        return build(code, message, status, Map.of(), request);
    }

    private ResponseEntity<ApiResponse<Void>> build(String code, String message, HttpStatus status,
            Map<String, String> fieldErrors, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(code, message, fieldErrors, request.getRequestURI()));
    }
}
