package com.parkhyuns00.blog.global.exception.storage;

import com.parkhyuns00.blog.global.exception.ExternalException;

public class StorageException extends ExternalException {

    public StorageException(StorageExceptionCode exceptionCode, String detail, Throwable cause) {
        super(exceptionCode, detail, cause);
    }
}
