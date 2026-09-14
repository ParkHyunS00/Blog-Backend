package com.parkhyuns00.blog.domain.visitor.exception;

import com.parkhyuns00.blog.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VisitorExceptionCode implements ExceptionCode {

    INVALID_VISITOR_ID(HttpStatus.BAD_REQUEST, "V_001", "방문자 ID가 올바르지 않습니다."),
    VISITOR_RECORD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "V_002", "방문 기록 저장에 실패했습니다."),
    VISITOR_STATS_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "V_003", "방문자 통계 조회에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
