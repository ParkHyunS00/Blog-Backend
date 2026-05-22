package com.parkhyuns00.blog.domain.post.exception;

import com.parkhyuns00.blog.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostExceptionCode implements ExceptionCode {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P_001", "게시글을 찾을 수 없습니다."),

    POST_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "P_101", "게시글 이미지를 찾을 수 없습니다."),
    POST_IMAGE_EMPTY(HttpStatus.BAD_REQUEST, "P_102", "이미지 파일이 비어 있습니다."),
    POST_IMAGE_READ_FAILED(HttpStatus.BAD_REQUEST, "P_103", "이미지 파일을 읽을 수 없습니다."),
    INVALID_POST_IMAGE_MIME_TYPE(HttpStatus.BAD_REQUEST, "P_104", "이미지 파일 형식이 올바르지 않습니다."),
    UNSUPPORTED_POST_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "P_105", "지원하지 않는 이미지 형식입니다."),
    POST_IMAGE_ALREADY_ATTACHED(HttpStatus.CONFLICT, "P_106", "이미 연결된 이미지입니다."),
    INVALID_POST_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "P_107", "게시글 이미지 타입이 올바르지 않습니다."),
    POST_IMAGE_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "P_108", "게시글 이미지 저장에 실패했습니다."),
    POST_IMAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "P_109", "게시글 이미지 삭제에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
