package com.parkhyuns00.blog.domain.category.service.dto;

public record CategoryWithLatestPostDto(
    Long categoryId,
    String categoryName,
    String categorySlug,
    Long postCount,
    CategoryLatestPostDto latestPost
) {
}
