package com.parkhyuns00.blog.global.response;

import com.parkhyuns00.blog.global.exception.ExceptionCode;
import lombok.Builder;

import java.util.List;

@Builder(access = lombok.AccessLevel.PRIVATE)
public record ErrorResponse(
    String code,
    String message,
    String description,
    List<DetailError> detailErrors
) {
    public static ErrorResponse of(ExceptionCode code) {
        return of(code, null, null);
    }

    public static ErrorResponse of(ExceptionCode code, String detail) {
        return of(code, detail, null);
    }

    public static ErrorResponse of(ExceptionCode code, List<DetailError> detailErrors) {
        return of(code, null, detailErrors);
    }

    public static ErrorResponse of(ExceptionCode code, String detail, List<DetailError> detailErrors) {
        return ErrorResponse.builder()
            .code(code.getCode())
            .message(code.getMessage())
            .description(detail)
            .detailErrors(detailErrors)
            .build();
    }
}
