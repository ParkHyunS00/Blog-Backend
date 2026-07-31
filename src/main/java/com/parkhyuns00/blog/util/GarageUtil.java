package com.parkhyuns00.blog.util;

import com.parkhyuns00.blog.config.storage.GarageProperties;
import com.parkhyuns00.blog.global.exception.storage.StorageException;
import com.parkhyuns00.blog.global.exception.storage.StorageExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Component
@RequiredArgsConstructor
public class GarageUtil {

    private final S3Client s3Client;
    private final GarageProperties properties;

    public void uploadObject(String objectKey, String contentType, byte[] content) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(properties.bucket())
            .key(objectKey)
            .contentType(contentType)
            .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
        } catch (S3Exception | SdkClientException exception) {
            throw new StorageException(StorageExceptionCode.STORAGE_UPLOAD_FAILED, objectKey, exception);
        }
    }

    public ResponseInputStream<GetObjectResponse> downloadObject(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(properties.bucket())
            .key(objectKey)
            .build();

        try {
            return s3Client.getObject(getObjectRequest);
        } catch (NoSuchKeyException exception) {
            throw new StorageException(StorageExceptionCode.STORAGE_OBJECT_NOT_FOUND, objectKey, exception);
        } catch (S3Exception | SdkClientException exception) {
            throw new StorageException(StorageExceptionCode.STORAGE_DOWNLOAD_FAILED, objectKey, exception);
        }
    }

    public void deleteObject(String objectKey) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
            .bucket(properties.bucket())
            .key(objectKey)
            .build();

        try {
            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception | SdkClientException exception) {
            throw new StorageException(StorageExceptionCode.STORAGE_DELETE_FAILED, objectKey, exception);
        }
    }
}
