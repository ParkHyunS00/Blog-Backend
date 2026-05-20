package com.parkhyuns00.blog.global.exception.storage;

import com.parkhyuns00.blog.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StorageExceptionCode implements ExceptionCode {

    STORAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ST_001", "파일 업로드에 실패했습니다."),
    STORAGE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ST_002", "파일 다운로드에 실패했습니다."),
    STORAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ST_003", "파일 삭제에 실패했습니다."),
    STORAGE_OBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "ST_004", "파일을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
