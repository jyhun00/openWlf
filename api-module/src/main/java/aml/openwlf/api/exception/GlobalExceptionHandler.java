package aml.openwlf.api.exception;

import aml.openwlf.data.exception.BusinessException;
import aml.openwlf.data.exception.DuplicateLinkException;
import aml.openwlf.data.exception.EntityNotFoundException;
import aml.openwlf.data.exception.InvalidOperationException;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 핸들러
 *
 * OOP 원칙: 다형성을 활용한 예외 처리
 * - 예외 유형별로 적절한 HTTP 상태 코드 매핑
 * - 일관된 에러 응답 형식 제공
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(DuplicateLinkException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateLink(DuplicateLinkException ex) {
        log.warn("Duplicate link attempt: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(InvalidOperationException ex) {
        log.warn("Invalid operation: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ValidationErrorResponse.of("VALIDATION_FAILED", "입력값 검증 실패", errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_ARGUMENT", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."));
    }

    @Getter
    @Builder
    public static class ErrorResponse {
        private final String errorCode;
        private final String message;
        private final LocalDateTime timestamp;

        public static ErrorResponse of(String errorCode, String message) {
            return ErrorResponse.builder()
                    .errorCode(errorCode)
                    .message(message)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ValidationErrorResponse {
        private final String errorCode;
        private final String message;
        private final Map<String, String> fieldErrors;
        private final LocalDateTime timestamp;

        public static ValidationErrorResponse of(String errorCode, String message,
                                                  Map<String, String> fieldErrors) {
            return ValidationErrorResponse.builder()
                    .errorCode(errorCode)
                    .message(message)
                    .fieldErrors(fieldErrors)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }
}
