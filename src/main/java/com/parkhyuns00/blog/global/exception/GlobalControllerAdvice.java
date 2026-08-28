package com.parkhyuns00.blog.global.exception;

import com.parkhyuns00.blog.global.response.DetailError;
import com.parkhyuns00.blog.global.response.StandardResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<StandardResponse<Void>> handleBusinessException(BusinessException exception) {
        log.warn("[BusinessException] code={}, message={}, detail={}",
            exception.getExceptionCode().getCode(),
            exception.getExceptionCode().getMessage(),
            exception.getDetail()
        );

        if (exception.getDetail() != null) {
            return StandardResponse.fail(exception.getExceptionCode(), exception.getDetail());
        }

        return StandardResponse.fail(exception.getExceptionCode());
    }

    @ExceptionHandler(ExternalException.class)
    public ResponseEntity<StandardResponse<Void>> handleExternalException(ExternalException exception) {
        log.error("[ExternalException] code={}, message={}, detail={}",
            exception.getExceptionCode().getCode(),
            exception.getExceptionCode().getMessage(),
            exception.getDetail(),
            exception
        );

        if (exception.getDetail() != null) {
            return StandardResponse.fail(exception.getExceptionCode(), exception.getDetail());
        }

        return StandardResponse.fail(exception.getExceptionCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardResponse<Void>> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException exception
    ) {
        List<DetailError> detailErrors = exception.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.groupingBy(
                FieldError::getField,
                Collectors.mapping(
                    fieldError -> fieldError.getDefaultMessage() == null
                        ? "Invalid Value"
                        : fieldError.getDefaultMessage(),
                    Collectors.toList()
                )
            ))
            .entrySet()
            .stream()
            .map(entry -> new DetailError(entry.getKey(), entry.getValue()))
            .toList();

        log.warn("[MethodArgumentNotValidException] errors={}", detailErrors);

        return StandardResponse.fail(CommonExceptionCode.INVALID_REQUEST, detailErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<StandardResponse<Void>> handleConstraintViolationException(
        ConstraintViolationException exception
    ) {
        List<DetailError> detailErrors = exception.getConstraintViolations()
            .stream()
            .collect(Collectors.groupingBy(
                violation -> violation.getPropertyPath().toString(),
                Collectors.mapping(
                    ConstraintViolation::getMessage,
                    Collectors.toList()
                )
            ))
            .entrySet()
            .stream()
            .map(entry -> new DetailError(entry.getKey(), entry.getValue()))
            .toList();

        log.warn("[ConstraintViolationException] errors={}", detailErrors);

        return StandardResponse.fail(CommonExceptionCode.INVALID_REQUEST, detailErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<StandardResponse<Void>> handleHandlerMethodValidationException(
        HandlerMethodValidationException exception
    ) {
        List<DetailError> detailErrors = exception
                .getParameterValidationResults()
                .stream()
                .map(result -> {
                    String parameterName = result.getMethodParameter().getParameterName();

                    List<String> messages = result
                            .getResolvableErrors()
                            .stream()
                            .map(error ->
                                error.getDefaultMessage() == null
                                    ? "Invalid Value"
                                    : error.getDefaultMessage()
                            )
                            .toList();

                    return new DetailError(parameterName == null ? "parameter" : parameterName, messages);
                })
                .toList();

        log.warn("[HandlerMethodValidationException] errors={}", detailErrors);

        return StandardResponse.fail(CommonExceptionCode.INVALID_REQUEST, detailErrors);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<StandardResponse<Void>> handleMaxUploadSizeExceededException(
        MaxUploadSizeExceededException exception
    ) {
        log.warn("[MaxUploadSizeExceededException] maxUploadSize={}", exception.getMaxUploadSize());

        return StandardResponse.fail(CommonExceptionCode.UPLOAD_SIZE_EXCEEDED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardResponse<Void>> handleException(Exception e) {
        log.error("Unexpected exception", e);

        return StandardResponse.fail(CommonExceptionCode.INTERNAL_SERVER_ERROR);
    }
}
