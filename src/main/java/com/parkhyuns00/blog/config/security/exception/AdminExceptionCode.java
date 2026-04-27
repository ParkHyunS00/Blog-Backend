package com.parkhyuns00.blog.config.security.exception;

import com.parkhyuns00.blog.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdminExceptionCode implements ExceptionCode {
    ADMIN_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AU01", "관리자 인증에 실패했습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
