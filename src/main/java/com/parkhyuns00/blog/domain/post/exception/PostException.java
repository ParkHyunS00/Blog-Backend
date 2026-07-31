package com.parkhyuns00.blog.domain.post.exception;

import com.parkhyuns00.blog.global.exception.BusinessException;

public class PostException extends BusinessException {

    public PostException(PostExceptionCode code) {
        super(code);
    }

    public PostException(PostExceptionCode exceptionCode, String detail) {
        super(exceptionCode, detail);
    }

    public PostException(PostExceptionCode exceptionCode, Throwable cause) {
        super(exceptionCode, cause);
    }
}
