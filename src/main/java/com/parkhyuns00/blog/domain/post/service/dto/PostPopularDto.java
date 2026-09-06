package com.parkhyuns00.blog.domain.post.service.dto;

import java.time.LocalDateTime;

public record PostPopularDto(
    Long postId,
    String title,
    String categoryName,
    String categorySlug,
    LocalDateTime createdAt,
    long viewCount
) {
}
