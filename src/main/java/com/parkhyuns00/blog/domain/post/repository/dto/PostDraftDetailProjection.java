package com.parkhyuns00.blog.domain.post.repository.dto;

import java.time.LocalDateTime;

public record PostDraftDetailProjection(
    Long postId,
    String title,
    String summary,
    String content,
    Long categoryId,
    String categoryName,
    String categorySlug,
    Long thumbnailImageId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
