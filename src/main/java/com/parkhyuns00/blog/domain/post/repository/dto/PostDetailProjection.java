package com.parkhyuns00.blog.domain.post.repository.dto;

import java.time.LocalDateTime;

public record PostDetailProjection(
    Long postId,
    String title,
    String summary,
    String content,
    long viewCount,
    Long thumbnailImageId,
    String categoryName,
    String categorySlug,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
