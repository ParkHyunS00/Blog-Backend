package com.parkhyuns00.blog.domain.post.exception;

import com.parkhyuns00.blog.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostExceptionCode implements ExceptionCode {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P_001", "게시글을 찾을 수 없습니다."),
    INVALID_POST_TITLE(HttpStatus.BAD_REQUEST, "P_002", "게시글 제목이 올바르지 않습니다."),
    INVALID_POST_SUMMARY(HttpStatus.BAD_REQUEST, "P_003", "게시글 요약이 올바르지 않습니다."),
    INVALID_POST_CONTENT(HttpStatus.BAD_REQUEST, "P_004", "게시글 본문이 올바르지 않습니다."),
    INVALID_POST_STATUS(HttpStatus.BAD_REQUEST, "P_005", "게시글 상태가 올바르지 않습니다."),
    INVALID_POST_CATEGORY(HttpStatus.BAD_REQUEST, "P_006", "게시글 카테고리가 올바르지 않습니다."),
    POST_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "P_007", "게시글 저장에 실패했습니다."),
    INVALID_POST(HttpStatus.BAD_REQUEST, "P_008", "게시글이 올바르지 않습니다."),
    INVALID_POST_TAG(HttpStatus.BAD_REQUEST, "P_009", "게시글 태그가 올바르지 않습니다."),
    INVALID_POST_IMAGE(HttpStatus.BAD_REQUEST, "P_010", "게시글 이미지가 올바르지 않습니다."),

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
