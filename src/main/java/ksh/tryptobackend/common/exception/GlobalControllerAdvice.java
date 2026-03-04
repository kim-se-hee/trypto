package ksh.tryptobackend.common.exception;

import ksh.tryptobackend.common.dto.response.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final MessageSource messageSource;

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        String message = messageSource.getMessage(
            errorCode.getMessageKey(),
            e.getArgs().toArray(),
            Locale.getDefault()
        );

        ApiResponseDto<Void> response = ApiResponseDto.of(
            errorCode.getStatus(),
            errorCode.name(),
            message,
            null
        );

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleValidationException(
        MethodArgumentNotValidException e
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        ApiResponseDto<Void> response = ApiResponseDto.of(
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            message,
            null
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleOptimisticLockingFailure(
        ObjectOptimisticLockingFailureException e
    ) {
        log.warn("Optimistic locking failure: {}", e.getMessage());

        String message = messageSource.getMessage(
            ErrorCode.CONCURRENT_MODIFICATION.getMessageKey(),
            null,
            Locale.getDefault()
        );

        ApiResponseDto<Void> response = ApiResponseDto.of(
            ErrorCode.CONCURRENT_MODIFICATION.getStatus(),
            ErrorCode.CONCURRENT_MODIFICATION.name(),
            message,
            null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleDataIntegrityViolation(
        DataIntegrityViolationException e
    ) {
        log.warn("Data integrity violation: {}", e.getMessage());

        String message = messageSource.getMessage(
            ErrorCode.DUPLICATE_REQUEST.getMessageKey(),
            null,
            Locale.getDefault()
        );

        ApiResponseDto<Void> response = ApiResponseDto.of(
            ErrorCode.DUPLICATE_REQUEST.getStatus(),
            ErrorCode.DUPLICATE_REQUEST.name(),
            message,
            null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleTypeMismatchException(
        MethodArgumentTypeMismatchException e
    ) {
        String message = String.format("'%s' 파라미터의 값이 유효하지 않습니다: %s", e.getName(), e.getValue());

        ApiResponseDto<Void> response = ApiResponseDto.of(
            HttpStatus.BAD_REQUEST.value(),
            "TYPE_MISMATCH",
            message,
            null
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);

        String message = messageSource.getMessage(
            ErrorCode.INTERNAL_SERVER_ERROR.getMessageKey(),
            null,
            Locale.getDefault()
        );

        ApiResponseDto<Void> response = ApiResponseDto.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ErrorCode.INTERNAL_SERVER_ERROR.name(),
            message,
            null
        );

        return ResponseEntity.internalServerError().body(response);
    }
}
