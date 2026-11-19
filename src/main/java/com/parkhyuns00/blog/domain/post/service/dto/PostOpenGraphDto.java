package com.parkhyuns00.blog.domain.post.service.dto;

public record PostOpenGraphDto(
    Long postId,
    String title,
    String summary,
    Long thumbnailImageId,
    String mimeType,
    Integer width,
    Integer height
) {
}
