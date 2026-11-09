package com.parkhyuns00.blog.domain.visitor.exception;

import com.parkhyuns00.blog.global.exception.BusinessException;
import com.parkhyuns00.blog.global.exception.ExceptionCode;

public class VisitorException extends BusinessException {

    public VisitorException(ExceptionCode exceptionCode) {
        super(exceptionCode);
    }

    public VisitorException(VisitorExceptionCode code, String detail) {
        super(code, detail);
    }

    public VisitorException(VisitorExceptionCode code, Throwable cause) {
        super(code, cause);
    }
}
