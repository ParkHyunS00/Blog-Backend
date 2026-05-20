package com.parkhyuns00.blog.global.exception;

import lombok.Getter;

@Getter
public abstract class ExternalException extends RuntimeException {

    private final ExceptionCode exceptionCode;
    private final String detail;

    protected ExternalException(ExceptionCode exceptionCode) {
        super(exceptionCode.getMessage());
        this.exceptionCode = exceptionCode;
        this.detail = null;
    }

    protected ExternalException(ExceptionCode exceptionCode, String detail) {
        super(exceptionCode.getMessage());
        this.exceptionCode = exceptionCode;
        this.detail = detail;
    }

    protected ExternalException(ExceptionCode exceptionCode, String detail, Throwable cause) {
        super(exceptionCode.getMessage(), cause);
        this.exceptionCode = exceptionCode;
        this.detail = detail;
    }
}
