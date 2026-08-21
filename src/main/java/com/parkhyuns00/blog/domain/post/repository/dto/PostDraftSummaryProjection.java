package com.parkhyuns00.blog.domain.post.repository.dto;

import java.time.LocalDateTime;

public record PostDraftSummaryProjection(
    Long postId,
    String title,
    Long categoryId,
    String categoryName,
    String categorySlug,
    LocalDateTime updatedAt
) {
}
