package com.parkhyuns00.blog.domain.category.exception;

import com.parkhyuns00.blog.global.exception.BusinessException;

public class CategoryException extends BusinessException {

    public CategoryException(CategoryExceptionCode code) {
        super(code);
    }

    public CategoryException(CategoryExceptionCode exceptionCode, String detail) {
        super(exceptionCode, detail);
    }

    public CategoryException(CategoryExceptionCode exceptionCode, Throwable cause) {
        super(exceptionCode, cause);
    }
}
