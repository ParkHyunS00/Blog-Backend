package com.parkhyuns00.blog.domain.post.controller.dto;

import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.service.dto.PostImageUploadDto;

public record ImageUploadResponse(
    Long imageId,
    PostImageType type,
    String objectKey,
    String mimeType
) {
    public static ImageUploadResponse from(PostImageUploadDto dto) {
        return new ImageUploadResponse(dto.imageId(), dto.type(), dto.objectKey(), dto.mimeType());
    }
}
