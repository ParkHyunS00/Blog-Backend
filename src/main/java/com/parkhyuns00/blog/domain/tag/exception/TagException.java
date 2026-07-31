package com.parkhyuns00.blog.domain.tag.exception;

import com.parkhyuns00.blog.global.exception.BusinessException;

public class TagException extends BusinessException {

    public TagException(TagExceptionCode code) {
        super(code);
    }

    public TagException(TagExceptionCode exceptionCode, String detail) {
        super(exceptionCode, detail);
    }

    public TagException(TagExceptionCode exceptionCode, Throwable cause) {
        super(exceptionCode, cause);
    }
}
