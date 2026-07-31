package com.parkhyuns00.blog.domain.tag.exception;

import com.parkhyuns00.blog.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TagExceptionCode implements ExceptionCode {

    INVALID_TAG_NAME(HttpStatus.BAD_REQUEST, "T_001", "태그 이름이 올바르지 않습니다."),
    TOO_MANY_TAGS(HttpStatus.BAD_REQUEST, "T_002", "태그 개수가 너무 많습니다."),
    TAG_NAME_DUPLICATED(HttpStatus.CONFLICT, "T_003", "이미 존재하는 태그 이름입니다."),
    TAG_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "T_004", "태그 저장에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
