package com.parkhyuns00.blog.domain.post.service.dto;

import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;

public record PostImageUploadDto(
    Long imageId,
    PostImageType type,
    String objectKey,
    String mimeType
) {
    public static PostImageUploadDto from(PostImage image) {
        return new PostImageUploadDto(image.getId(), image.getType(), image.getObjectKey(), image.getMimeType());
    }
}
