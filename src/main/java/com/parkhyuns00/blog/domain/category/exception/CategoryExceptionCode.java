package com.parkhyuns00.blog.domain.category.exception;

import com.parkhyuns00.blog.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CategoryExceptionCode implements ExceptionCode {

    INVALID_CATEGORY_NAME(HttpStatus.BAD_REQUEST, "C_001", "카테고리 이름이 올바르지 않습니다."),
    CATEGORY_NAME_DUPLICATED(HttpStatus.CONFLICT, "C_002", "이미 존재하는 카테고리 이름입니다."),
    CATEGORY_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "C_003", "카테고리 저장에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
