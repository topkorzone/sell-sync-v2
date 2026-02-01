package com.mhub.api.config;

import com.mhub.common.dto.ApiResponse;
import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;

@Slf4j @RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) { log.warn("Business error: [{}] {}", e.getCode(), e.getMessage()); return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getCode(), e.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) { String msg = e.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage).collect(Collectors.joining(", ")); return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ErrorCodes.VALIDATION_ERROR, msg)); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccess(AccessDeniedException e) { return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ErrorCodes.AUTH_INSUFFICIENT_PERMISSION, "Access denied")); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception e) { log.error("Unexpected error", e); return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ErrorCodes.INTERNAL_ERROR, "Internal server error")); }
}
