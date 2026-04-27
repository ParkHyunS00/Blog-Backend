package com.parkhyuns00.blog.global.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.parkhyuns00.blog.global.exception.ExceptionCode;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Getter
@Builder
@JsonPropertyOrder({"status", "data", "error"})
public class StandardResponse<T> {

    private int status;

    private T data;

    private ErrorResponse error;

    public static <T> ResponseEntity<StandardResponse<T>> ok(T data) {
        StandardResponse<T> response = StandardResponse.<T>builder()
            .status(HttpStatus.OK.value())
            .data(data)
            .build();

        return ResponseEntity.ok(response);
    }

    public static <T> ResponseEntity<StandardResponse<T>> created(T data) {
        StandardResponse<T> response = StandardResponse.<T>builder()
            .status(HttpStatus.CREATED.value())
            .data(data)
            .build();

        return ResponseEntity.status(HttpStatus.CREATED.value()).body(response);
    }

    public static <T> ResponseEntity<StandardResponse<T>> fail(ExceptionCode code) {
        StandardResponse<T> response = StandardResponse.<T>builder()
            .status(code.getHttpStatus().value())
            .error(ErrorResponse.of(code))
            .build();

        return ResponseEntity.status(code.getHttpStatus().value()).body(response);
    }

    public static <T> ResponseEntity<StandardResponse<T>> fail(ExceptionCode code, String detail) {
        StandardResponse<T> response = StandardResponse.<T>builder()
            .status(code.getHttpStatus().value())
            .error(ErrorResponse.of(code, detail))
            .build();

        return ResponseEntity.status(code.getHttpStatus().value()).body(response);
    }

    public static <T> ResponseEntity<StandardResponse<T>> fail(ExceptionCode code, List<DetailError> detailErrors) {
        final ErrorResponse err = ErrorResponse.of(code, detailErrors);
        final StandardResponse<T> res = StandardResponse.<T>builder()
            .status(code.getHttpStatus().value())
            .error(err)
            .build();
        return ResponseEntity
            .status(code.getHttpStatus().value())
            .body(res);
    }
}
