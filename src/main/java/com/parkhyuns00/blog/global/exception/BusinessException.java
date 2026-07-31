package com.parkhyuns00.blog.global.exception;

import lombok.Getter;

@Getter
public abstract class BusinessException extends RuntimeException {

    private final ExceptionCode exceptionCode;
    private final String detail;

    public BusinessException(ExceptionCode exceptionCode) {
        super(exceptionCode.getMessage());
        this.exceptionCode = exceptionCode;
        this.detail = null;
    }

    protected BusinessException(ExceptionCode exceptionCode, String detail) {
        super(exceptionCode.getMessage());
        this.exceptionCode = exceptionCode;
        this.detail = detail;
    }

    protected BusinessException(ExceptionCode exceptionCode, Throwable cause) {
        super(exceptionCode.getMessage(), cause);
        this.exceptionCode = exceptionCode;
        this.detail = null;
    }
}
