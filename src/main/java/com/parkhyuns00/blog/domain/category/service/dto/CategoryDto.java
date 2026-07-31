package com.parkhyuns00.blog.domain.category.service.dto;

import com.parkhyuns00.blog.domain.category.model.Category;

public record CategoryDto(
    Long categoryId,
    String name,
    String slug
) {
    public static CategoryDto from(Category category) {
        return new CategoryDto(
            category.getId(),
            category.getName(),
            category.getSlug()
        );
    }
}
