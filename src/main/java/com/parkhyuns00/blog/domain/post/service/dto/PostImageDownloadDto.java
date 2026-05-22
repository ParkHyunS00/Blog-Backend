package com.parkhyuns00.blog.domain.post.service.dto;

import com.parkhyuns00.blog.domain.post.model.PostImage;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public record PostImageDownloadDto(
    String mimeType,
    Long contentLength,
    ResponseInputStream<GetObjectResponse> inputStream
) {
    public static PostImageDownloadDto from(PostImage postImage, ResponseInputStream<GetObjectResponse> inputStream) {
        return new PostImageDownloadDto(
            postImage.getMimeType(),
            inputStream.response().contentLength(),
            inputStream
        );
    }
}
