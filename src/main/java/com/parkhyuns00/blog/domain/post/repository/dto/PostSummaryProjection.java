package com.parkhyuns00.blog.domain.post.repository.dto;

import java.time.LocalDateTime;

public record PostSummaryProjection(
    Long postId,
    String title,
    String summary,
    Long thumbnailImageId,
    String categoryName,
    String categorySlug,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
