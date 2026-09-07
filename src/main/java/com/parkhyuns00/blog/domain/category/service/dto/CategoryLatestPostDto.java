package com.parkhyuns00.blog.domain.category.service.dto;

import java.time.LocalDateTime;

public record CategoryLatestPostDto(
    Long postId,
    String title,
    String summary,
    LocalDateTime createdAt
) {
}
