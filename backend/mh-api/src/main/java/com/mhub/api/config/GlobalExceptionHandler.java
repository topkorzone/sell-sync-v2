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
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        log.warn("Business error: [{}] {}", e.getCode(), e.getMessage());
        HttpStatus status = resolveStatus(e.getCode());
        return ResponseEntity.status(status).body(ApiResponse.error(e.getCode(), e.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) { String msg = e.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage).collect(Collectors.joining(", ")); return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ErrorCodes.VALIDATION_ERROR, msg)); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccess(AccessDeniedException e) { return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ErrorCodes.AUTH_INSUFFICIENT_PERMISSION, "Access denied")); }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
        if (e.getMessage() != null && e.getMessage().contains("Tenant ID is not set")) {
            log.warn("Tenant context missing: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(ErrorCodes.AUTH_TENANT_MISSING,
                            "테넌트 정보가 없습니다. JWT의 app_metadata에 tenant_id가 설정되어 있는지 확인하세요."));
        }
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCodes.INTERNAL_ERROR, "Internal server error"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception e) { log.error("Unexpected error", e); return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ErrorCodes.INTERNAL_ERROR, "Internal server error")); }

    private HttpStatus resolveStatus(String code) {
        if (code == null) return HttpStatus.BAD_REQUEST;
        return switch (code) {
            case ErrorCodes.AUTH_INVALID_CREDENTIALS, ErrorCodes.AUTH_REFRESH_FAILED,
                 ErrorCodes.AUTH_INVALID_TOKEN, ErrorCodes.AUTH_EXPIRED_TOKEN -> HttpStatus.UNAUTHORIZED;
            case ErrorCodes.AUTH_INSUFFICIENT_PERMISSION, ErrorCodes.AUTH_TENANT_MISSING -> HttpStatus.FORBIDDEN;
            case ErrorCodes.MARKETPLACE_CREDENTIAL_NOT_FOUND, ErrorCodes.ORDER_NOT_FOUND,
                 ErrorCodes.TENANT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ErrorCodes.MARKETPLACE_CREDENTIAL_DUPLICATE -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
