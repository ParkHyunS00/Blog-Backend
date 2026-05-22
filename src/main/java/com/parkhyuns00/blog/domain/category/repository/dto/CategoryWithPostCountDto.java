package com.parkhyuns00.blog.domain.category.repository.dto;

public record CategoryWithPostCountDto(
    Long categoryId,
    String name,
    String slug,
    Long postCount
) {
}
